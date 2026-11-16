package ai.andalus.jse.android.internal

import ai.andalus.jse.android.*
import ai.andalus.jse.android.model.*
import android.view.Surface
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import java.util.UUID

internal class ArCoreProviderSession(private val nativeSession: Session) : ProviderSession {
    override val id: String = UUID.randomUUID().toString()
    private val ownerThread = Thread.currentThread()
    private val lifecycle = SessionLifecycle()
    private val anchors = mutableMapOf<String, Anchor>()
    private val planeIds = mutableMapOf<Plane, String>()
    private val nativeHits = mutableMapOf<String, HitResult>()
    private var sequence = 0L
    private var latestFrame: Frame? = null
    private var displayGeometryConfigured = false
    private var displayWidth = 0
    private var displayHeight = 0

    init { lifecycle.markConfigured() }

    override val state: SessionState
        get() { checkThread(); return lifecycle.state }

    override fun resume() {
        checkThread()
        try {
            nativeSession.resume()
            lifecycle.resume()
            latestFrame = null
            nativeHits.clear()
        } catch (error: Throwable) {
            lifecycle.fail()
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun pause() {
        checkThread()
        if (lifecycle.state == SessionState.CLOSED) throw closedError()
        if (lifecycle.state == SessionState.RUNNING) {
            try {
                nativeSession.pause()
                lifecycle.pause()
                latestFrame = null
                nativeHits.clear()
            } catch (error: Throwable) {
                lifecycle.fail()
                throw ArCoreErrorMapper.map(error)
            }
        } else lifecycle.pause()
    }

    override fun update(): ProviderFrame {
        checkThread()
        lifecycle.requireRunning()
        try {
            val frame = nativeSession.update()
            latestFrame = frame
            sequence += 1
            nativeHits.clear()

            val camera = frame.camera
            val tracking = mapTracking(camera.trackingState)
            val observations = nativeSession.getAllTrackables(Plane::class.java)
                .map(::toPlaneObservation)
                .sortedBy { it.id }

            return ProviderFrame(
                sessionId = id,
                sequence = sequence,
                timestampNanos = frame.timestamp,
                trackingState = tracking,
                cameraPose = toSpatialPose(camera.pose, "camera"),
                planes = observations,
            )
        } catch (error: Throwable) {
            if (error is ProviderException) throw error
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun setDisplayGeometry(rotationDegrees: Int, widthPixels: Int, heightPixels: Int) {
        checkThread()
        lifecycle.requireRunning()
        if (rotationDegrees !in setOf(0, 90, 180, 270) || widthPixels <= 0 || heightPixels <= 0) {
            throw ProviderException(
                ProviderErrorCode.INVALID_CONFIGURATION,
                "Display geometry requires rotation 0/90/180/270 and positive pixel dimensions.",
                false,
            )
        }
        val rotation = when (rotationDegrees) {
            0 -> Surface.ROTATION_0
            90 -> Surface.ROTATION_90
            180 -> Surface.ROTATION_180
            else -> Surface.ROTATION_270
        }
        try {
            nativeSession.setDisplayGeometry(rotation, widthPixels, heightPixels)
            displayGeometryConfigured = true
            displayWidth = widthPixels
            displayHeight = heightPixels
            nativeHits.clear()
        } catch (error: Throwable) {
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun hitTest(xPixels: Float, yPixels: Float): List<SpatialHit> {
        checkThread()
        lifecycle.requireRunning()
        val frame = latestFrame ?: throw ProviderException(
            ProviderErrorCode.SESSION_NOT_RUNNING,
            "Call update() before hit testing.",
            true,
        )
        if (!displayGeometryConfigured) throw ProviderException(
            ProviderErrorCode.INVALID_CONFIGURATION,
            "Set display geometry before hit testing.",
            true,
        )
        if (!xPixels.isFinite() || !yPixels.isFinite() ||
            xPixels < 0f || yPixels < 0f ||
            xPixels >= displayWidth || yPixels >= displayHeight
        ) {
            throw ProviderException(
                ProviderErrorCode.INVALID_CONFIGURATION,
                "Hit-test coordinates must be finite and inside the configured view bounds.",
                false,
            )
        }

        return try {
            nativeHits.clear()
            frame.hitTest(xPixels, yPixels).mapNotNull { nativeHit ->
                val plane = nativeHit.trackable as? Plane
                if (plane != null && !plane.isPoseInPolygon(nativeHit.hitPose)) return@mapNotNull null

                val token = UUID.randomUUID().toString()
                nativeHits[token] = nativeHit
                SpatialHit(
                    id = token,
                    sessionId = id,
                    frameSequence = sequence,
                    kind = mapHitKind(nativeHit),
                    pose = toSpatialPose(nativeHit.hitPose, "anchor"),
                    distanceMeters = nativeHit.distance.toDouble(),
                    planeId = plane?.let(::planeId),
                )
            }
        } catch (error: Throwable) {
            if (error is ProviderException) throw error
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun createAnchorFromHit(hit: SpatialHit): AnchorHandle {
        checkThread()
        lifecycle.requireRunning()
        if (hit.sessionId != id || hit.frameSequence != sequence) throw ProviderException(
            ProviderErrorCode.INVALID_HANDLE,
            "Hit result belongs to another session or an expired frame.",
            false,
        )
        val nativeHit = nativeHits.remove(hit.id) ?: throw ProviderException(
            ProviderErrorCode.INVALID_HANDLE,
            "Hit result is unknown or has already been consumed.",
            false,
        )
        return try {
            registerAnchor(nativeHit.createAnchor())
        } catch (error: Throwable) {
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun createAnchor(pose: SpatialPose): AnchorHandle {
        checkThread()
        lifecycle.requireRunning()
        if (pose.sourceFrame != "anchor" || pose.targetFrame != "arcore-world") {
            throw ProviderException(
                ProviderErrorCode.INVALID_CONFIGURATION,
                "Anchor pose must use anchor-to-arcore-world frames.",
                false,
            )
        }
        val t = pose.translationMeters
        val q = pose.rotationXyzw
        val values = listOf(t.x, t.y, t.z, q.x, q.y, q.z, q.w)
        if (values.any { !it.isFinite() }) throw ProviderException(
            ProviderErrorCode.INVALID_CONFIGURATION,
            "Anchor pose contains non-finite values.",
            false,
        )
        val nativePose = Pose(
            floatArrayOf(t.x.toFloat(), t.y.toFloat(), t.z.toFloat()),
            floatArrayOf(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat()),
        )
        return try {
            registerAnchor(nativeSession.createAnchor(nativePose))
        } catch (error: Throwable) {
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun detachAnchor(anchor: AnchorHandle) {
        checkThread()
        lifecycle.requireRunning()
        if (anchor.sessionId != id) throw ProviderException(
            ProviderErrorCode.INVALID_HANDLE,
            "Anchor belongs to a different session.",
            false,
        )
        val native = anchors.remove(anchor.id) ?: throw ProviderException(
            ProviderErrorCode.INVALID_HANDLE,
            "Anchor is unknown or already detached.",
            false,
        )
        try {
            native.detach()
        } catch (error: Throwable) {
            throw ArCoreErrorMapper.map(error)
        }
    }

    override fun close() {
        checkThread()
        if (lifecycle.state == SessionState.CLOSED) return
        try {
            nativeHits.clear()
            anchors.values.forEach { it.detach() }
            anchors.clear()
            if (lifecycle.state == SessionState.RUNNING) nativeSession.pause()
        } finally {
            latestFrame = null
            nativeSession.close()
            lifecycle.close()
        }
    }

    private fun registerAnchor(nativeAnchor: Anchor): AnchorHandle {
        val handle = AnchorHandle(id, UUID.randomUUID().toString())
        anchors[handle.id] = nativeAnchor
        return handle
    }

    private fun toPlaneObservation(plane: Plane): PlaneObservation {
        val polygonBuffer = plane.polygon.duplicate()
        polygonBuffer.position(0)
        val vertices = buildList {
            while (polygonBuffer.remaining() >= 2) {
                add(Vector2(
                    polygonBuffer.get().toDouble(),
                    polygonBuffer.get().toDouble(),
                ))
            }
        }
        val type = when (plane.type) {
            Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneType.HORIZONTAL_UPWARD_FACING
            Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneType.HORIZONTAL_DOWNWARD_FACING
            Plane.Type.VERTICAL -> PlaneType.VERTICAL
            else -> PlaneType.UNKNOWN
        }
        return PlaneObservation(
            id = planeId(plane),
            trackingState = mapTracking(plane.trackingState),
            type = type,
            centerPose = toSpatialPose(plane.centerPose, "plane"),
            extentX = plane.extentX.toDouble(),
            extentZ = plane.extentZ.toDouble(),
            polygon = vertices,
            subsumedById = plane.subsumedBy?.let(::planeId),
        )
    }

    private fun planeId(plane: Plane): String =
        planeIds.getOrPut(plane) { UUID.randomUUID().toString() }

    private fun toSpatialPose(pose: Pose, sourceFrame: String): SpatialPose {
        val t = pose.translation
        val q = pose.rotationQuaternion
        return SpatialPose(
            translationMeters = Vector3(t[0].toDouble(), t[1].toDouble(), t[2].toDouble()),
            rotationXyzw = Quaternion(
                q[0].toDouble(), q[1].toDouble(), q[2].toDouble(), q[3].toDouble(),
            ),
            sourceFrame = sourceFrame,
            targetFrame = "arcore-world",
        )
    }

    private fun mapTracking(state: com.google.ar.core.TrackingState): TrackingState =
        when (state) {
            com.google.ar.core.TrackingState.TRACKING -> TrackingState.TRACKING
            com.google.ar.core.TrackingState.PAUSED -> TrackingState.LIMITED
            com.google.ar.core.TrackingState.STOPPED -> TrackingState.LOST
        }

    private fun mapHitKind(hit: HitResult): HitKind = when (hit.trackable) {
        is Plane -> HitKind.PLANE
        is Point -> HitKind.FEATURE_POINT
        is DepthPoint -> HitKind.DEPTH_POINT
        is InstantPlacementPoint -> HitKind.INSTANT_PLACEMENT
        else -> HitKind.OTHER
    }

    private fun checkThread() {
        if (Thread.currentThread() !== ownerThread) throw ProviderException(
            ProviderErrorCode.INVALID_CONFIGURATION,
            "AR session methods must run on the thread that created the session.",
            false,
        )
    }

    private fun closedError() = ProviderException(
        ProviderErrorCode.SESSION_CLOSED,
        "Session is closed.",
        false,
    )
}
