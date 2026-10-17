package com.agroroute

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.components.openPhoneDialer
import org.junit.Assert.*
import org.junit.Test

/** Intercepts intents so QA never opens a real phone app or places a call. */
class PhoneDialerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun dialUsesEncodedTelephoneUriAndWorksWithApplicationContext() {
        var launched: Intent? = null
        val context = object : ContextWrapper(instrumentation.targetContext.applicationContext) {
            override fun startActivity(intent: Intent) { launched = intent }
        }
        openPhoneDialer(context, "+94 71 234 5678")
        val intent = requireNotNull(launched)
        assertEquals(Intent.ACTION_DIAL, intent.action)
        assertEquals("tel", intent.data?.scheme)
        assertEquals("+94 71 234 5678", intent.data?.schemeSpecificPart)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun missingOrRestrictedDialerDoesNotCrash() {
        for (failure in listOf(ActivityNotFoundException(), SecurityException())) {
            val context = object : ContextWrapper(instrumentation.targetContext) {
                override fun startActivity(intent: Intent) { throw failure }
            }
            instrumentation.runOnMainSync { openPhoneDialer(context, "0712345678") }
        }
    }
}
