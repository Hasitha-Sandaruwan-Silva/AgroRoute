package com.agroroute.core.remote

import org.junit.Assert.*
import org.junit.Test

class PickupPinTest {
    @Test fun generatedPinUsesFixedLengthRandomSaltAndExpensiveVerifier() {
        val first = PickupPin.generate()
        val second = PickupPin.generate()
        assertTrue(first.code.matches(Regex("[0-9]{6}")))
        assertEquals(32, first.salt.length)
        assertEquals(64, first.verifier.length)
        assertNotEquals(first.salt, second.salt)
        assertTrue(PickupPin.matches(first.code, first.salt, first.verifier))
        assertTrue(PickupPin.ITERATIONS >= 100_000)
    }
    @Test fun wrongAndMalformedPinsDoNotMatch() {
        val pin = PickupPin.generate()
        val wrong = ((pin.code.toInt() + 1) % 1_000_000).toString().padStart(6, '0')
        assertFalse(PickupPin.matches(wrong, pin.salt, pin.verifier))
        assertFalse(PickupPin.matches("12345", pin.salt, pin.verifier))
        assertFalse(PickupPin.matches("abcdef", pin.salt, pin.verifier))
    }
    @Test fun rotatingVerifierInvalidatesPreviousPinEvenIfSameDigitsWereGenerated() {
        val old = PickupPin.generate()
        val next = PickupPin.generate()
        assertFalse(PickupPin.matches(old.code, next.salt, old.verifier))
    }
    @Test fun eventIdsAreDeterministicAndCancellationNamesActor() {
        assertEquals("o_farmer_handover", SparkScore.eventId("o", "farmer_handover", "f"))
        assertEquals("o_s_cancellation", SparkScore.eventId("o", "cancellation", "s"))
        assertNotEquals(SparkScore.eventId("o", "cancellation", "s"), SparkScore.eventId("o", "cancellation", "f"))
    }
}
