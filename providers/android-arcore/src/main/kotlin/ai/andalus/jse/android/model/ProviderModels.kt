package ai.andalus.jse.android.model

/**
 * Stable, vendor-neutral identifiers and values exposed by the Android provider.
 * No ARCore SDK types cross this boundary.
 */
data class ProviderInfo(
    val id: String = "android.arcore",
    val adapterVersion: String,
    val runtimeVersion: String? = null,
)

enum class AvailabilityStatus {
    CHECKING, SUPPORTED_INSTALLED, SUPPORTED_INSTALL_REQUIRED, UNSUPPORTED,
    PERMISSION_REQUIRED, PERMISSION_DENIED, UNAVAILABLE, ERROR,
}

data class AvailabilityResult(
    val status: AvailabilityStatus,
    val reason: String? = null,
    val recoverable: Boolean = false,
)

enum class SessionState { CREATED, CONFIGURED, RUNNING, PAUSED, FAILED, CLOSED }
enum class TrackingState { INITIALIZING, TRACKING, LIMITED, LOST }
enum class CapabilityStatus { AVAILABLE, UNAVAILABLE, REQUIRES_CONFIGURATION, UNKNOWN }

data class Capability(val id: String, val status: CapabilityStatus, val reason: String? = null)
data class CapabilityReport(val providerId: String, val capabilities: List<Capability>)

data class SessionConfig(
    val enableHorizontalPlanes: Boolean = true,
    val enableVerticalPlanes: Boolean = true,
)

data class SpatialPose(
    val translationMeters: Vector3,
    /** Quaternion in x, y, z, w order. */
    val rotationXyzw: Quaternion,
    val sourceFrame: String,
    val targetFrame: String,
)

data class Vector3(val x: Double, val y: Double, val z: Double)
data class Vector2(val x: Double, val y: Double)
data class Quaternion(val x: Double, val y: Double, val z: Double, val w: Double)

enum class PlaneType {
    HORIZONTAL_UPWARD_FACING,
    HORIZONTAL_DOWNWARD_FACING,
    VERTICAL,
    UNKNOWN,
}

/** Snapshot of a detected plane. Polygon vertices are local plane X/Z coordinates in meters. */
data class PlaneObservation(
    val id: String,
    val trackingState: TrackingState,
    val type: PlaneType,
    val centerPose: SpatialPose,
    val extentX: Double,
    val extentZ: Double,
    val polygon: List<Vector2>,
    val subsumedById: String? = null,
)

enum class HitKind { PLANE, FEATURE_POINT, DEPTH_POINT, INSTANT_PLACEMENT, OTHER }

/**
 * Opaque session-scoped hit token. Pass the returned value to createAnchorFromHit;
 * callers must not persist it across frames, pause/resume, or sessions.
 */
data class SpatialHit(
    val id: String,
    val sessionId: String,
    val frameSequence: Long,
    val kind: HitKind,
    val pose: SpatialPose,
    val distanceMeters: Double,
    val planeId: String? = null,
)

data class ProviderFrame(
    val sessionId: String,
    val sequence: Long,
    /** Provider monotonic timestamp; not Unix/wall-clock time. */
    val timestampNanos: Long,
    val trackingState: TrackingState,
    val cameraPose: SpatialPose?,
    val planes: List<PlaneObservation> = emptyList(),
)

data class AnchorHandle(val sessionId: String, val id: String)
