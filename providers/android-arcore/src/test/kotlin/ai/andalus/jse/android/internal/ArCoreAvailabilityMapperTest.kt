package ai.andalus.jse.android.internal

import ai.andalus.jse.android.model.AvailabilityStatus
import com.google.ar.core.ArCoreApk
import org.junit.Assert.assertEquals
import org.junit.Test

class ArCoreAvailabilityMapperTest {
    @Test fun installedRuntimeStillRequiresHostPermission() {
        val result = ArCoreAvailabilityMapper.map(ArCoreApk.Availability.SUPPORTED_INSTALLED, false)
        assertEquals(AvailabilityStatus.PERMISSION_REQUIRED, result.status)
    }
    @Test fun missingRuntimeRequiresInstall() {
        val result = ArCoreAvailabilityMapper.map(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED, true)
        assertEquals(AvailabilityStatus.SUPPORTED_INSTALL_REQUIRED, result.status)
    }
    @Test fun unsupportedDeviceIsNotAdvertisedAsCapable() {
        val result = ArCoreAvailabilityMapper.map(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE, true)
        assertEquals(AvailabilityStatus.UNSUPPORTED, result.status)
    }
}
