package ai.andalus.jse.android.internal

import ai.andalus.jse.android.ProviderErrorCode
import ai.andalus.jse.android.ProviderException
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

internal object ArCoreErrorMapper {
    fun map(error: Throwable): ProviderException = when (error) {
        is CameraNotAvailableException -> ProviderException(
            ProviderErrorCode.CAMERA_UNAVAILABLE,
            "The camera is currently unavailable to the AR session.",
            recoverable = true,
            cause = error,
        )
        is UnavailableArcoreNotInstalledException,
        is UnavailableApkTooOldException,
        is UnavailableUserDeclinedInstallationException -> ProviderException(
            ProviderErrorCode.INSTALL_REQUIRED,
            "Google Play Services for AR must be installed or updated.",
            recoverable = true,
            cause = error,
        )
        is UnavailableSdkTooOldException -> ProviderException(
            ProviderErrorCode.RUNTIME_UNAVAILABLE,
            "This application version is too old for the installed AR runtime.",
            recoverable = false,
            cause = error,
        )
        is UnavailableDeviceNotCompatibleException -> ProviderException(
            ProviderErrorCode.RUNTIME_UNAVAILABLE,
            "This device is not compatible with the AR runtime.",
            recoverable = false,
            cause = error,
        )
        is UnavailableException -> ProviderException(
            ProviderErrorCode.RUNTIME_UNAVAILABLE,
            "The AR runtime is unavailable.",
            recoverable = true,
            cause = error,
        )
        else -> ProviderException(
            ProviderErrorCode.PROVIDER_FAILURE,
            "The AR provider operation failed.",
            recoverable = false,
            cause = error,
        )
    }
}
