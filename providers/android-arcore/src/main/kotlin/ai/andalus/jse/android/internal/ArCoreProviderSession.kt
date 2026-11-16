package ai.andalus.jse.android.internal

import ai.andalus.jse.android.*
import ai.andalus.jse.android.model.*
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.Session
import java.util.UUID

internal class ArCoreProviderSession(private val nativeSession: Session) : ProviderSession {
    override val id: String = UUID.randomUUID().toString()
    private val ownerThread = Thread.currentThread()
    private val lifecycle = SessionLifecycle()
    private val anchors = mutableMapOf<String, Anchor>()
    private var sequence = 0L

    init { lifecycle.markConfigured() }
    override val state: SessionState
        get() { checkThread(); return lifecycle.state }

    override fun resume() {
        checkThread()
        try { nativeSession.resume(); lifecycle.resume() }
        catch (error: Throwable) { lifecycle.fail(); throw ArCoreErrorMapper.map(error) }
    }

    override fun pause() {
        checkThread()
        if (lifecycle.state == SessionState.CLOSED) throw closedError()
        if (lifecycle.state == SessionState.RUNNING) {
            try { nativeSession.pause(); lifecycle.pause() }
            catch (error: Throwable) { lifecycle.fail(); throw ArCoreErrorMapper.map(error) }
        } else lifecycle.pause()
    }

    override fun update(): ProviderFrame {
        checkThread()
        lifecycle.requireRunning()
        try {
            val frame = nativeSession.update()
            val camera = frame.camera
            val tracking = when (camera.trackingState) {
                com.google.ar.core.TrackingState.TRACKING -> TrackingState.TRACKING
                com.google.ar.core.TrackingState.PAUSED -> TrackingState.LIMITED
                com.google.ar.core.TrackingState.STOPPED -> TrackingState.LOST
            }
            val p = camera.pose
            val t = p.translation
            val q = p.rotationQuaternion
            sequence += 1
            return ProviderFrame(id, sequence, frame.timestamp, tracking, SpatialPose(
                Vector3(t[0].toDouble(), t[1].toDouble(), t[2].toDouble()),
                Quaternion(q[0].toDouble(), q[1].toDouble(), q[2].toDouble(), q[3].toDouble()),
                sourceFrame = "camera", targetFrame = "arcore-world"))
        } catch (error: Throwable) {
            if (error is ProviderException) throw error
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun createAnchor(pose: SpatialPose): AnchorHandle {
        checkThread(); lifecycle.requireRunning()
        if (pose.sourceFrame != "anchor" || pose.targetFrame != "arcore-world") {
            throw ProviderException(ProviderErrorCode.INVALID_CONFIGURATION,
                "Anchor pose must use anchor-to-arcore-world frames.", false)
        }
        val t = pose.translationMeters; val q = pose.rotationXyzw
        val values = listOf(t.x, t.y, t.z, q.x, q.y, q.z, q.w)
        if (values.any { !it.isFinite() }) throw ProviderException(
            ProviderErrorCode.INVALID_CONFIGURATION, "Anchor pose contains non-finite values.", false)
        val nativePose = Pose(
            floatArrayOf(t.x.toFloat(), t.y.toFloat(), t.z.toFloat()),
            floatArrayOf(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat()))
        try {
            val nativeAnchor = nativeSession.createAnchor(nativePose)
            val handle = AnchorHandle(id, UUID.randomUUID().toString())
            anchors[handle.id] = nativeAnchor
            return handle
        } catch (error: Throwable) { throw ArCoreErrorMapper.map(error) }
    }

    override fun detachAnchor(anchor: AnchorHandle) {
        checkThread(); lifecycle.requireRunning()
        if (anchor.sessionId != id) throw ProviderException(
            ProviderErrorCode.INVALID_HANDLE, "Anchor belongs to a different session.", false)
        val native = anchors.remove(anchor.id) ?: throw ProviderException(
            ProviderErrorCode.INVALID_HANDLE, "Anchor is unknown or already detached.", false)
        native.detach()
    }

    override fun close() {
        checkThread()
        if (lifecycle.state == SessionState.CLOSED) return
        try {
            anchors.values.forEach { it.detach() }
            anchors.clear()
            if (lifecycle.state == SessionState.RUNNING) nativeSession.pause()
        } finally {
            nativeSession.close()
            lifecycle.close()
        }
    }

    private fun checkThread() {
        if (Thread.currentThread() !== ownerThread) throw ProviderException(
            ProviderErrorCode.INVALID_CONFIGURATION,
            "AR session methods must run on the thread that created the session.", false)
    }
    private fun closedError() = ProviderException(
        ProviderErrorCode.SESSION_CLOSED, "Session is closed.", false)
}
