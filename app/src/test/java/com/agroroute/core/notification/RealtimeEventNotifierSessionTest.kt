package com.agroroute.core.notification

import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import io.mockk.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RealtimeEventNotifierSessionTest {
    @Test fun restartingAndStoppingRemoveThePreviousAccountWatch() = runTest {
        val service = mockk<NotificationService>()
        val active = mutableSetOf<String>()
        coEvery { service.watchInbox(any()) } coAnswers {
            val uid = firstArg<UserProfile>().userId
            assertTrue(active.add(uid))
            try { awaitCancellation() } finally { active.remove(uid) }
        }
        val notifier = RealtimeEventNotifier(service)
        val farmer = UserProfile(userId = "farmer", role = UserRole.FARMER)
        notifier.start(this, farmer); runCurrent()
        notifier.start(this, farmer); runCurrent()
        assertEquals(setOf("farmer"), active)
        notifier.start(this, UserProfile(userId = "supplier", role = UserRole.SUPPLIER)); runCurrent()
        assertEquals(setOf("supplier"), active)
        notifier.stop(); runCurrent()
        assertTrue(active.isEmpty())
        assertTrue(coroutineContext[kotlinx.coroutines.Job]!!.isActive)
    }
}
