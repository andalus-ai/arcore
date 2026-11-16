package ai.andalus.jse.android.internal

import ai.andalus.jse.android.model.AvailabilityResult
import ai.andalus.jse.android.model.AvailabilityStatus
import com.google.ar.core.ArCoreApk

internal object ArCoreAvailabilityMapper {
    fun map(availability: ArCoreApk.Availability, cameraPermissionGranted: Boolean): AvailabilityResult {
        if (!cameraPermissionGranted) return AvailabilityResult(
            AvailabilityStatus.PERMISSION_REQUIRED,
            "The host application must grant camera permission before starting AR.",
            recoverable = true,
        )
        return when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED ->
                AvailabilityResult(AvailabilityStatus.SUPPORTED_INSTALLED)
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD ->
                AvailabilityResult(AvailabilityStatus.SUPPORTED_INSTALL_REQUIRED,
                    "Google Play Services for AR must be installed or updated.", true)
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                AvailabilityResult(AvailabilityStatus.UNSUPPORTED,
                    "This device does not support ARCore.", false)
            ArCoreApk.Availability.UNKNOWN_CHECKING,
            ArCoreApk.Availability.UNKNOWN_ERROR,
            ArCoreApk.Availability.UNKNOWN_TIMED_OUT ->
                AvailabilityResult(AvailabilityStatus.UNAVAILABLE,
                    "ARCore availability could not be confirmed yet.", true)
        }
    }
}
