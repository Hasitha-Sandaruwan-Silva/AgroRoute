package com.agroroute

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.agroroute.core.notification.*
import java.io.File
import org.junit.*
import org.junit.Assert.*

class MessagesPhoneNotificationTest {
    @Test fun messageChannelPostsVisiblePhoneNotificationWhileProcessIsInBackground() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        NotificationHelper.createOrGetChannel(context)
        Assume.assumeTrue(NotificationHelper.areNotificationsEnabled(context, NotificationHelper.MESSAGES_CHANNEL_ID))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val service = NotificationService(context)
        val id = "pixel-message-channel-verification"
        val title = "New message from Nimal"
        val device = UiDevice.getInstance(instrumentation)
        try {
            device.pressHome()
            assertTrue(service.post(NotificationEvent.CHAT_MESSAGE, NotificationEvent.Payload(),
                "chat/pixel-fixture?orderId=order-1024", title, "Order #order-1024: Is this order ready?", id))
            val channel = manager.getNotificationChannel(NotificationHelper.MESSAGES_CHANNEL_ID)
            assertEquals("Messages", channel.name.toString())
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
            val posted = manager.activeNotifications.single { it.id == id.hashCode() }
            assertEquals(NotificationHelper.MESSAGES_CHANNEL_ID, posted.notification.channelId)
            assertNotNull(posted.notification.contentIntent)
            device.openNotification()
            assertTrue("Chat notification must be visible in Android notification shade",
                device.findObject(UiSelector().text(title)).waitForExists(10_000))
            File(context.cacheDir, "master-phone-notification.png").outputStream().use {
                instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally {
            service.cancelLocalNotification(id)
            device.pressBack()
        }
    }
}
