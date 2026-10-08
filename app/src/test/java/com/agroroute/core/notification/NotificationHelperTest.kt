package com.agroroute.core.notification

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun preTiramisuReturnsTrueWithoutCheckingPermission() {
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) } returns mockk()
        val result = NotificationHelper.hasNotificationPermission(context)
        assertTrue(result)
    }

    @Test
    fun tiramisuWithoutPermissionReturnsFalse() {
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) } returns mockk()
        every { android.os.Build.VERSION.SDK_INT } returns android.os.Build.VERSION_CODES.TIRAMISU
        every { context.checkPermission(android.Manifest.permission.POST_NOTIFICATIONS, android.os.Process.myPid(), android.os.Process.myUid()) } returns PackageManager.PERMISSION_DENIED
        val result = NotificationHelper.hasNotificationPermission(context)
        assertFalse(result)
    }

    @Test
    fun tiramisuWithPermissionReturnsTrue() {
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) } returns mockk()
        every { android.os.Build.VERSION.SDK_INT } returns android.os.Build.VERSION_CODES.TIRAMISU
        every { context.checkPermission(android.Manifest.permission.POST_NOTIFICATIONS, android.os.Process.myPid(), android.os.Process.myUid()) } returns PackageManager.PERMISSION_GRANTED
        val result = NotificationHelper.hasNotificationPermission(context)
        assertTrue(result)
    }

    @Test
    fun channelIdIsStable() {
        assertEquals("agroroute_notifications", NotificationHelper.CHANNEL_ID)
    }

    @Test
    fun suggestionChannelIdIsStable() {
        assertEquals("agraroute_order_suggestions", NotificationHelper.ORDER_SUGGESTION_CHANNEL_ID)
    }
}
