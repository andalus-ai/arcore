package ai.andalus.jse.android

enum class ProviderErrorCode {
    RUNTIME_UNAVAILABLE,
    INSTALL_REQUIRED,
    CAMERA_PERMISSION_REQUIRED,
    CAMERA_PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    INVALID_CONFIGURATION,
    SESSION_NOT_RUNNING,
    SESSION_CLOSED,
    INVALID_HANDLE,
    RESOURCE_EXHAUSTED,
    PROVIDER_FAILURE,
}

class ProviderException(
    val code: ProviderErrorCode,
    message: String,
    val recoverable: Boolean,
    val providerDiagnosticCode: String? = null,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
