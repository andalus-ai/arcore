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
    CHECKING,
    SUPPORTED_INSTALLED,
    SUPPORTED_INSTALL_REQUIRED,
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    UNAVAILABLE,
    ERROR,
}

data class AvailabilityResult(
    val status: AvailabilityStatus,
    val reason: String? = null,
    val recoverable: Boolean = false,
)

enum class SessionState {
    CREATED,
    CONFIGURED,
    RUNNING,
    PAUSED,
    FAILED,
    CLOSED,
}

enum class TrackingState {
    INITIALIZING,
    TRACKING,
    LIMITED,
    LOST,
}

enum class CapabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    REQUIRES_CONFIGURATION,
    UNKNOWN,
}

data class Capability(
    val id: String,
    val status: CapabilityStatus,
    val reason: String? = null,
)

data class CapabilityReport(
    val providerId: String,
    val capabilities: List<Capability>,
)

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

data class Quaternion(val x: Double, val y: Double, val z: Double, val w: Double)

data class ProviderFrame(
    val sessionId: String,
    val sequence: Long,
    /** Provider monotonic timestamp; not Unix/wall-clock time. */
    val timestampNanos: Long,
    val trackingState: TrackingState,
    val cameraPose: SpatialPose?,
)

data class AnchorHandle(
    val sessionId: String,
    val id: String,
)
