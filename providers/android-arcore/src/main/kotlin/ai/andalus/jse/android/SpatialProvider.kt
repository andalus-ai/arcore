package ai.andalus.jse.android

import ai.andalus.jse.android.model.AvailabilityResult
import ai.andalus.jse.android.model.CapabilityReport
import ai.andalus.jse.android.model.ProviderFrame
import ai.andalus.jse.android.model.ProviderInfo
import ai.andalus.jse.android.model.SessionConfig
import ai.andalus.jse.android.model.SessionState
import ai.andalus.jse.android.model.SpatialPose
import ai.andalus.jse.android.model.AnchorHandle

/**
 * Host-neutral entry point for a spatial runtime.
 *
 * The host application owns runtime camera-permission UX and passes the resulting
 * permission state to the provider. Implementations must document thread affinity.
 */
interface SpatialProvider {
    val info: ProviderInfo

    suspend fun checkAvailability(cameraPermissionGranted: Boolean): AvailabilityResult

    fun capabilities(): CapabilityReport

    fun createSession(
        config: SessionConfig,
        cameraPermissionGranted: Boolean,
    ): ProviderSession
}

interface ProviderSession : AutoCloseable {
    val id: String
    val state: SessionState

    fun resume()
    fun pause()
    fun update(): ProviderFrame
    fun createAnchor(pose: SpatialPose): AnchorHandle
    fun detachAnchor(anchor: AnchorHandle)

    /** Idempotent. Invalidates all session-scoped handles. */
    override fun close()
}
