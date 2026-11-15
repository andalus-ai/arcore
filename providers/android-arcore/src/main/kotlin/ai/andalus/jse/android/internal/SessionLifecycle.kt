package ai.andalus.jse.android.internal

import ai.andalus.jse.android.ProviderErrorCode
import ai.andalus.jse.android.ProviderException
import ai.andalus.jse.android.model.SessionState

/**
 * Small deterministic lifecycle state machine. ARCore Session calls are deliberately
 * delegated to an adapter that serializes access on its provider thread.
 */
internal class SessionLifecycle {
    var state: SessionState = SessionState.CREATED
        private set

    fun markConfigured() {
        transition(setOf(SessionState.CREATED, SessionState.PAUSED), SessionState.CONFIGURED)
    }

    fun resume() {
        transition(
            setOf(SessionState.CONFIGURED, SessionState.PAUSED),
            SessionState.RUNNING,
        )
    }

    fun pause() {
        when (state) {
            SessionState.RUNNING -> state = SessionState.PAUSED
            SessionState.PAUSED, SessionState.CONFIGURED -> Unit
            SessionState.CLOSED -> throw closed()
            else -> invalid("Cannot pause session in state $state")
        }
    }

    fun fail() {
        if (state != SessionState.CLOSED) state = SessionState.FAILED
    }

    fun close() {
        state = SessionState.CLOSED
    }

    fun requireRunning() {
        if (state == SessionState.CLOSED) throw closed()
        if (state != SessionState.RUNNING) {
            throw ProviderException(
                ProviderErrorCode.SESSION_NOT_RUNNING,
                "Session must be running for this operation",
                recoverable = true,
            )
        }
    }

    private fun transition(allowed: Set<SessionState>, target: SessionState) {
        if (state == SessionState.CLOSED) throw closed()
        if (state !in allowed) invalid("Cannot transition session from $state to $target")
        state = target
    }

    private fun closed() = ProviderException(
        ProviderErrorCode.SESSION_CLOSED,
        "Session is closed",
        recoverable = false,
    )

    private fun invalid(message: String) = throw ProviderException(
        ProviderErrorCode.INVALID_CONFIGURATION,
        message,
        recoverable = false,
    )
}
