package com.agroroute.core.notification

import android.net.Uri
import com.agroroute.core.model.UserRole
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ChatNotificationTest {
    @After fun cleanup() { unmockkStatic(Uri::class) }
    @Test fun previewNormalizesWhitespaceAndBoundsUnicodeWithoutSplittingEmoji() {
        assertEquals("Hi, is this order ready?", ChatNotification.preview("  Hi, is this order ready?\n"))
        val text = "\uD83C\uDF31".repeat(121)
        val preview = ChatNotification.preview(text)
        assertEquals(120, preview.codePointCount(0, preview.length))
        assertTrue(preview.endsWith("\u2026"))
        assertFalse(Character.isHighSurrogate(preview[preview.lastIndex - 1]))
        assertEquals(120, ChatNotification.preview("a".repeat(120)).length)
    }
    @Test fun historyUsesSenderAndConcisePreviewAndSameChatWithOptionalOrder() {
        mockkStatic(Uri::class)
        every { Uri.encode(any()) } answers { firstArg<String>() }
        val first = ChatNotification.item("event", "supplier", "Kasun", "a".repeat(500), 123, null)
        assertEquals("New message from Kasun", first.title)
        assertEquals(120, first.body.length)
        assertEquals("chat/supplier", first.route)
        assertFalse(first.isRead)
        val order = ChatNotification.item("event", "supplier", "Kasun", "Ready?", 123, 124, "long-order-123456789012")
        assertEquals("Order #123456789012 \u2022 Ready?", order.body)
        assertEquals("chat/supplier?orderId=long-order-123456789012", order.route)
        assertTrue(order.isRead)
        for (role in UserRole.entries) assertTrue(NotificationRoute.isAllowed(order.route, role))
    }
    @Test fun eventIdentityIsStablePerCommittedMessage() {
        assertEquals(ChatNotification.eventId("pair", "one"), ChatNotification.eventId("pair", "one"))
        assertNotEquals(ChatNotification.eventId("pair", "one"), ChatNotification.eventId("pair", "two"))
    }
    @Test fun chatRoutesRejectMalformedOrInjectedDestinations() {
        for (route in listOf("chat/", "chat/../admin", "chat/%2Fadmin", "chat/u?orderId=", "chat/u?orderId=x&role=admin",
            "chat/u?orderId=x%26y", "chat/u?other=x", "chat/u#fragment", "https://evil/chat/u", "chat/%ZZ", "chat/%0Auser")) {
            assertFalse(route, NotificationRoute.isChatRoute(route))
        }
        assertTrue(NotificationRoute.isChatRoute("chat/farmer-1"))
        assertTrue(NotificationRoute.isChatRoute("chat/farmer-1?orderId=order-1"))
    }
}
