package ai.andalus.jse.android

import ai.andalus.jse.android.model.*

/**
 * Host-neutral entry point for a spatial runtime.
 * The host owns camera permission UX and all provider session calls are thread-affine.
 */
interface SpatialProvider {
    val info: ProviderInfo
    suspend fun checkAvailability(cameraPermissionGranted: Boolean): AvailabilityResult
    fun capabilities(): CapabilityReport
    fun createSession(config: SessionConfig, cameraPermissionGranted: Boolean): ProviderSession
}

interface ProviderSession : AutoCloseable {
    val id: String
    val state: SessionState

    fun resume()
    fun pause()

    /**
     * Update once per render tick. Plane observations are a session snapshot for this frame.
     */
    fun update(): ProviderFrame

    /**
     * Set the pixel dimensions and clockwise display rotation of the view that supplies
     * hit-test coordinates. Call after view size/rotation changes and before hit testing.
     * rotationDegrees must be one of 0, 90, 180, or 270.
     */
    fun setDisplayGeometry(rotationDegrees: Int, widthPixels: Int, heightPixels: Int)

    /** Returns supported scene intersections nearest-first for view-local pixel coordinates. */
    fun hitTest(xPixels: Float, yPixels: Float): List<SpatialHit>

    /** Creates an anchor from a recent hit returned by this session. */
    fun createAnchorFromHit(hit: SpatialHit): AnchorHandle

    /** Creates a world-space anchor. Pose frames must be anchor -> arcore-world. */
    fun createAnchor(pose: SpatialPose): AnchorHandle
    fun detachAnchor(anchor: AnchorHandle)

    /** Idempotent. Invalidates all session-scoped handles. */
    override fun close()
}
