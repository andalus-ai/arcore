package ai.andalus.jse.android.internal

import ai.andalus.jse.android.ProviderErrorCode
import ai.andalus.jse.android.ProviderException
import ai.andalus.jse.android.model.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionLifecycleTest {
    @Test
    fun configureResumePauseResumeAndClose() {
        val lifecycle = SessionLifecycle()
        lifecycle.markConfigured()
        lifecycle.resume()
        assertEquals(SessionState.RUNNING, lifecycle.state)

        lifecycle.pause()
        assertEquals(SessionState.PAUSED, lifecycle.state)

        lifecycle.resume()
        lifecycle.close()
        lifecycle.close()
        assertEquals(SessionState.CLOSED, lifecycle.state)
    }

    @Test
    fun updateRequiresRunningSession() {
        val lifecycle = SessionLifecycle()
        val error = assertThrows(ProviderException::class.java) {
            lifecycle.requireRunning()
        }
        assertEquals(ProviderErrorCode.SESSION_NOT_RUNNING, error.code)
    }

    @Test
    fun closedSessionCannotResume() {
        val lifecycle = SessionLifecycle()
        lifecycle.close()
        val error = assertThrows(ProviderException::class.java) {
            lifecycle.resume()
        }
        assertEquals(ProviderErrorCode.SESSION_CLOSED, error.code)
    }
}
