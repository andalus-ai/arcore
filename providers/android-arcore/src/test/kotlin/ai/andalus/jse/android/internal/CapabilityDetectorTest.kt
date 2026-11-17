package ai.andalus.jse.android.internal

import ai.andalus.jse.android.model.CapabilityStatus
import ai.andalus.jse.android.model.SessionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CapabilityDetectorTest {
    @Test
    fun capabilitiesRemainUnknownBeforeSessionConfiguration() {
        val report = CapabilityDetector.unknown("android.arcore")
        assertEquals("android.arcore", report.providerId)
        assertEquals(5, report.capabilities.size)
        report.capabilities.forEach { assertEquals(CapabilityStatus.UNKNOWN, it.status) }
    }

    @Test
    fun defaultConfigurationEnablesExposedSpatialFeatures() {
        val report = CapabilityDetector.configured("android.arcore", SessionConfig())
        report.capabilities.forEach { assertEquals(CapabilityStatus.AVAILABLE, it.status) }
    }

    @Test
    fun disabledPlaneFindingIsReportedAsUnavailable() {
        val report = CapabilityDetector.configured(
            "android.arcore",
            SessionConfig(enableHorizontalPlanes = false, enableVerticalPlanes = true),
        )
        val capabilities = report.capabilities.associateBy { it.id }
        assertEquals(CapabilityStatus.UNAVAILABLE, capabilities.getValue("plane_detection_horizontal").status)
        assertEquals(CapabilityStatus.AVAILABLE, capabilities.getValue("plane_detection_vertical").status)
        assertNotNull(capabilities.getValue("plane_detection_horizontal").reason)
        assertEquals(CapabilityStatus.AVAILABLE, capabilities.getValue("hit_test").status)
        assertEquals(CapabilityStatus.AVAILABLE, capabilities.getValue("anchors_local").status)
    }

    @Test
    fun allPlaneFindingCanBeDisabledWithoutDisablingLocalAnchors() {
        val report = CapabilityDetector.configured(
            "android.arcore",
            SessionConfig(enableHorizontalPlanes = false, enableVerticalPlanes = false),
        )
        val capabilities = report.capabilities.associateBy { it.id }
        assertEquals(CapabilityStatus.UNAVAILABLE, capabilities.getValue("plane_detection_horizontal").status)
        assertEquals(CapabilityStatus.UNAVAILABLE, capabilities.getValue("plane_detection_vertical").status)
        assertEquals(CapabilityStatus.AVAILABLE, capabilities.getValue("anchors_local").status)
    }
}
