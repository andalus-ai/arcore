package ai.andalus.jse.android.internal

import ai.andalus.jse.android.model.Capability
import ai.andalus.jse.android.model.CapabilityReport
import ai.andalus.jse.android.model.CapabilityStatus
import ai.andalus.jse.android.model.SessionConfig

/**
 * Reports only capabilities established by a successfully configured ARCore session.
 * This deliberately avoids claiming depth, instant placement, or other optional features
 * that this provider does not expose yet.
 */
internal object CapabilityDetector {
    fun unknown(providerId: String): CapabilityReport = CapabilityReport(
        providerId,
        listOf(
            Capability("camera_tracking", CapabilityStatus.UNKNOWN, "Create a session to confirm runtime support."),
            Capability("plane_detection_horizontal", CapabilityStatus.UNKNOWN, "Create a session to confirm runtime support."),
            Capability("plane_detection_vertical", CapabilityStatus.UNKNOWN, "Create a session to confirm runtime support."),
            Capability("hit_test", CapabilityStatus.UNKNOWN, "Create a session to confirm runtime support."),
            Capability("anchors_local", CapabilityStatus.UNKNOWN, "Create a session to confirm runtime support."),
        ),
    )

    fun configured(providerId: String, config: SessionConfig): CapabilityReport {
        fun plane(enabled: Boolean) = if (enabled) {
            CapabilityStatus.AVAILABLE
        } else {
            CapabilityStatus.UNAVAILABLE
        }

        return CapabilityReport(
            providerId,
            listOf(
                Capability("camera_tracking", CapabilityStatus.AVAILABLE),
                Capability(
                    "plane_detection_horizontal",
                    plane(config.enableHorizontalPlanes),
                    if (config.enableHorizontalPlanes) null else "Horizontal plane finding is disabled in this session.",
                ),
                Capability(
                    "plane_detection_vertical",
                    plane(config.enableVerticalPlanes),
                    if (config.enableVerticalPlanes) null else "Vertical plane finding is disabled in this session.",
                ),
                Capability("hit_test", CapabilityStatus.AVAILABLE),
                Capability("anchors_local", CapabilityStatus.AVAILABLE),
            ),
        )
    }
}
