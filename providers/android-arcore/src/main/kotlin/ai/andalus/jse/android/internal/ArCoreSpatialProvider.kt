package ai.andalus.jse.android.internal

import android.app.Activity
import android.content.Context
import ai.andalus.jse.android.ProviderErrorCode
import ai.andalus.jse.android.ProviderException
import ai.andalus.jse.android.ProviderSession
import ai.andalus.jse.android.RuntimeInstallResult
import ai.andalus.jse.android.SpatialProvider
import ai.andalus.jse.android.model.*
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Concrete ARCore provider. Create/use sessions on one dedicated host-owned thread. */
class ArCoreSpatialProvider(context: Context, private val adapterVersion: String) : SpatialProvider {
    private val appContext = context.applicationContext
    override val info = ProviderInfo("android.arcore", adapterVersion)
    @Volatile private var capabilityReport = CapabilityDetector.unknown(info.id)

    override suspend fun checkAvailability(cameraPermissionGranted: Boolean): AvailabilityResult {
        if (!cameraPermissionGranted) return ArCoreAvailabilityMapper.map(
            ArCoreApk.Availability.UNKNOWN_CHECKING, false)
        return try {
            suspendCoroutine { continuation ->
                ArCoreApk.getInstance().checkAvailabilityAsync(appContext) { availability ->
                    continuation.resume(ArCoreAvailabilityMapper.map(availability, true))
                }
            }
        } catch (_: RuntimeException) {
            AvailabilityResult(AvailabilityStatus.ERROR, "ARCore availability check failed.", true)
        }
    }

    /**
     * Starts the official, user-mediated AR runtime installation/update flow.
     * Call only from a foreground Activity after availability reports install-required.
     */
    fun requestRuntimeInstall(activity: Activity, userRequestedInstall: Boolean): RuntimeInstallResult {
        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> RuntimeInstallResult.INSTALLED
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> RuntimeInstallResult.INSTALL_FLOW_STARTED
            }
        } catch (error: Exception) {
            throw ArCoreErrorMapper.map(error)
        }
    }

    /**
     * UNKNOWN until a session has been successfully configured. Afterwards this is the
     * capability snapshot for the most recently configured session.
     */
    override fun capabilities(): CapabilityReport = capabilityReport

    override fun createSession(config: SessionConfig, cameraPermissionGranted: Boolean): ProviderSession {
        if (!cameraPermissionGranted) throw ProviderException(
            ProviderErrorCode.CAMERA_PERMISSION_REQUIRED,
            "The host application must grant camera permission before creating an AR session.", true)
        val session = try {
            Session(appContext)
        } catch (error: Exception) {
            throw ArCoreErrorMapper.map(error)
        }
        try {
            val nativeConfig = Config(session).apply {
                planeFindingMode = when {
                    config.enableHorizontalPlanes && config.enableVerticalPlanes ->
                        Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.enableHorizontalPlanes -> Config.PlaneFindingMode.HORIZONTAL
                    config.enableVerticalPlanes -> Config.PlaneFindingMode.VERTICAL
                    else -> Config.PlaneFindingMode.DISABLED
                }
            }
            session.configure(nativeConfig)
            capabilityReport = CapabilityDetector.configured(info.id, config)
            return ArCoreProviderSession(session)
        } catch (error: Exception) {
            session.close()
            throw ArCoreErrorMapper.map(error)
        }
    }
}
