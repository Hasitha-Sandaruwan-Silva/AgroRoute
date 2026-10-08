package com.agroroute.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun preTiramisuReturnsTrueWithoutCheckingPermission() {
        val result = NotificationHelper.hasNotificationPermission(32, false)
        assertTrue(result)
    }

    @Test
    fun tiramisuWithoutPermissionReturnsFalse() {
        val result = NotificationHelper.hasNotificationPermission(33, false)
        assertFalse(result)
    }

    @Test
    fun tiramisuWithPermissionReturnsTrue() {
        val result = NotificationHelper.hasNotificationPermission(33, true)
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
