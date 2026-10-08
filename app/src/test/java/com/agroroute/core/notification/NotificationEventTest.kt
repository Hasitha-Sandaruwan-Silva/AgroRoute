package com.agroroute.core.notification

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationEventTest {

    @Test
    fun harvestReminderTitleAndContent() {
        val event = NotificationEvent.HARVEST_REMINDER
        assertEquals("Harvest reminder", event.title)

        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(cropName = "Maha rice")
        )
        assertEquals("Maha rice is expected today.", content)
    }

    @Test
    fun harvestReminderFallbackUsesGenericHarvestWhenCropNameBlank() {
        val event = NotificationEvent.HARVEST_REMINDER
        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(cropName = "")
        )
        assertEquals("your harvest is expected today.", content)
    }

    @Test
    fun bidAcceptedTitleAndContent() {
        val event = NotificationEvent.BID_ACCEPTED
        assertEquals("Bid accepted", event.title)

        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(
                cropName = "Maha rice",
                supplierName = "John supplier",
                orderId = "bid-1"
            )
        )
        assertEquals("John supplier accepted your bid for Maha rice. Your order is now awaiting pickup.", content)
    }

    @Test
    fun bidAcceptedFallbackUsesGenericSupplierWhenNameBlank() {
        val event = NotificationEvent.BID_ACCEPTED
        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(cropName = "Maha rice", supplierName = "")
        )
        assertEquals("a supplier accepted your bid for Maha rice. Your order is now awaiting pickup.", content)
    }

    @Test
    fun orderRequestAcceptedTitleAndContent() {
        val event = NotificationEvent.ORDER_REQUEST_ACCEPTED
        assertEquals("Order request accepted", event.title)

        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(
                cropName = "Maha rice",
                supplierName = "John supplier",
                orderId = "req-1"
            )
        )
        assertEquals("John supplier's request for Maha rice has been accepted. Your order is now awaiting pickup.", content)
    }

    @Test
    fun orderRequestRejectedContentUsesReason() {
        val event = NotificationEvent.ORDER_REQUEST_REJECTED
        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(reason = "Insufficient stock")
        )
        assertEquals("Your order request was rejected. Insufficient stock", content)
    }

    @Test
    fun orderRequestRejectedFallbackUsesGenericReasonWhenBlank() {
        val event = NotificationEvent.ORDER_REQUEST_REJECTED
        val content = event.content(anyContext(), NotificationEvent.Payload())
        assertEquals("Your order request was rejected. No reason provided.", content)
    }

    @Test
    fun orderRequestCancelledContentUsesReason() {
        val event = NotificationEvent.ORDER_REQUEST_CANCELLED
        val content = event.content(
            anyContext(),
            NotificationEvent.Payload(reason = "Supplier withdrew")
        )
        assertEquals("The order request was cancelled. Supplier withdrew", content)
    }

    @Test
    fun payloadDefaultValues() {
        val payload = NotificationEvent.Payload()
        assertEquals("", payload.cropName)
        assertEquals("", payload.supplierName)
        assertEquals("", payload.reason)
        assertEquals("", payload.orderId)
        assertEquals("", payload.status)
        assertEquals("", payload.listingId)
    }

    @Test
    fun payloadExtractsNonBlankValues() {
        val payload = NotificationEvent.Payload(
            cropName = "  Maha rice  ",
            supplierName = "  J  ",
            reason = "\t",
            orderId = "  order-1  "
        )
        assertEquals("  Maha rice  ", payload.cropName)
        assertEquals("  J  ", payload.supplierName)
        assertEquals("\t", payload.reason)
        assertEquals("  order-1  ", payload.orderId)
    }

    private fun anyContext(): android.content.Context {
        return mockk<android.content.Context>(relaxed = true)
    }
}

