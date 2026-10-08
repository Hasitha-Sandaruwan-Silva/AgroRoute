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
    fun deliveryRequiresAppAndChannelNotificationsToBeEnabled() {
        assertTrue(NotificationHelper.isNotificationDeliveryEnabled(32, false, true, true))
        assertFalse(NotificationHelper.isNotificationDeliveryEnabled(33, true, false, true))
        assertFalse(NotificationHelper.isNotificationDeliveryEnabled(33, true, true, false))
    }

    @Test
    fun channelIdIsStable() {
        assertEquals("agroroute_notifications", NotificationHelper.CHANNEL_ID)
    }

}
