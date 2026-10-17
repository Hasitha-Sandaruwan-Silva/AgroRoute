package com.agroroute

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.agroroute.core.notification.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Exercises the Android dispatcher only; does not create business or Firestore events. */
class NotificationDeliveryAuditTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val service = NotificationService(context)
    private val device = UiDevice.getInstance(instrumentation)

    private fun awaitNotificationCount(ids: List<Int>, expected: Int) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (manager.activeNotifications.count { it.id in ids } != expected &&
            android.os.SystemClock.uptimeMillis() < deadline) {
            android.os.SystemClock.sleep(50)
        }
        assertEquals(expected, manager.activeNotifications.count { it.id in ids })
    }

    private fun requireUnlockedScreen() {
        assumeTrue("Unlock the device before checking visible notification UI",
            !(context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isKeyguardLocked)
    }

    @Test fun backgroundMultipleEventsReachNotificationManager() {
        assumeTrue(NotificationHelper.hasNotificationPermission(context))
        device.pressHome()
        val events = listOf(NotificationEvent.CHAT_MESSAGE, NotificationEvent.BID_CREATED,
            NotificationEvent.ORDER_REQUEST_ACCEPTED, NotificationEvent.ORDER_COMPLETED)
        try {
            events.forEachIndexed { index, event ->
                assertTrue(service.post(event, NotificationEvent.Payload(),
                    title = "AgroRoute manager audit $index", body = "Delivery check", eventId = "manager-audit-$index"))
            }
            val ids = events.indices.map { "manager-audit-$it".hashCode() }
            awaitNotificationCount(ids, 4)
            manager.activeNotifications.filter { it.id in ids }.forEach { assertNotNull(it.notification.contentIntent) }
        } finally { events.indices.forEach { service.cancelLocalNotification("manager-audit-$it") } }
    }

    @Test fun foregroundBackgroundAndMultipleEventsReachAndroid() {
        assumeTrue(NotificationHelper.hasNotificationPermission(context))
        requireUnlockedScreen()
        val events = listOf(NotificationEvent.CHAT_MESSAGE, NotificationEvent.BID_CREATED,
            NotificationEvent.ORDER_REQUEST_ACCEPTED, NotificationEvent.ORDER_COMPLETED)
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        device.waitForIdle()
        try {
            events.forEachIndexed { index, event ->
                val id = "notification-audit-$index"
                assertTrue(service.post(event, NotificationEvent.Payload(),
                    title = "AgroRoute delivery audit $index", body = "Notification delivery check", eventId = id))
            }
            val ids = events.indices.map { "notification-audit-$it".hashCode() }
            awaitNotificationCount(ids, 4)
            device.pressHome()
            assertTrue(service.post(NotificationEvent.CHAT_MESSAGE, NotificationEvent.Payload(),
                title = "AgroRoute background audit", body = "Background delivery check", eventId = "notification-audit-background"))
            device.openNotification()
            assertTrue(device.findObject(UiSelector().text("AgroRoute background audit")).waitForExists(10_000))
        } finally {
            events.indices.forEach { service.cancelLocalNotification("notification-audit-$it") }
            service.cancelLocalNotification("notification-audit-background")
            device.pressBack()
        }
    }

    @Test fun screenOffWhileProcessAliveStillPosts() {
        assumeTrue(NotificationHelper.hasNotificationPermission(context))
        val id = "notification-audit-screen-off"
        try {
            device.sleep()
            assertTrue(service.post(NotificationEvent.CHAT_MESSAGE, NotificationEvent.Payload(), eventId = id))
            assertTrue(manager.activeNotifications.any { it.id == id.hashCode() })
        } finally {
            service.cancelLocalNotification(id)
            device.wakeUp()
        }
    }

    @Test fun deniedPermissionReturnsFalseWithoutPosting() {
        assumeTrue(!NotificationHelper.hasNotificationPermission(context))
        val id = "notification-audit-denied"
        assertFalse(service.post(NotificationEvent.CHAT_MESSAGE, NotificationEvent.Payload(), eventId = id))
        assertFalse(manager.activeNotifications.any { it.id == id.hashCode() })
    }

    @Test fun backgroundHeadsUpBanner() {
        assumeTrue(NotificationHelper.hasNotificationPermission(context))
        device.wakeUp()
        requireUnlockedScreen()
        device.pressHome()
        device.waitForIdle()
        val id = "notification-audit-banner"
        try {
            assertTrue(service.post(NotificationEvent.CHAT_MESSAGE, NotificationEvent.Payload(),
                title = "AgroRoute banner audit", body = "Heads-up delivery check", eventId = id))
            assertTrue("Heads-up banner should be visible before opening the shade",
                device.wait(androidx.test.uiautomator.Until.hasObject(
                    androidx.test.uiautomator.By.text("AgroRoute banner audit")), 5_000))
        } finally { service.cancelLocalNotification(id) }
    }
}
