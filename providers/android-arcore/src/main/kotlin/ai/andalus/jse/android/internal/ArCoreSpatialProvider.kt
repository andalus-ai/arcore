package ai.andalus.jse.android.internal

import android.content.Context
import ai.andalus.jse.android.ProviderErrorCode
import ai.andalus.jse.android.ProviderException
import ai.andalus.jse.android.ProviderSession
import ai.andalus.jse.android.SpatialProvider
import ai.andalus.jse.android.model.*
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session

/** Concrete ARCore provider. Create/use sessions on one dedicated host-owned thread. */
class ArCoreSpatialProvider(context: Context, private val adapterVersion: String) : SpatialProvider {
    private val appContext = context.applicationContext
    override val info = ProviderInfo("android.arcore", adapterVersion)

    override suspend fun checkAvailability(cameraPermissionGranted: Boolean): AvailabilityResult {
        if (!cameraPermissionGranted) return ArCoreAvailabilityMapper.map(
            ArCoreApk.Availability.UNKNOWN_CHECKING, false)
        return try {
            ArCoreAvailabilityMapper.map(ArCoreApk.getInstance().checkAvailability(appContext), true)
        } catch (_: RuntimeException) {
            AvailabilityResult(AvailabilityStatus.ERROR, "ARCore availability check failed.", true)
        }
    }

    override fun capabilities(): CapabilityReport = CapabilityReport(
        info.id,
        listOf("camera_tracking", "plane_detection_horizontal", "plane_detection_vertical",
            "hit_test", "anchors_local").map {
            Capability(it, CapabilityStatus.UNKNOWN,
                "Capability is not confirmed until runtime configuration and device validation.")
        },
    )

    override fun createSession(config: SessionConfig, cameraPermissionGranted: Boolean): ProviderSession {
        if (!cameraPermissionGranted) throw ProviderException(
            ProviderErrorCode.CAMERA_PERMISSION_REQUIRED,
            "The host application must grant camera permission before creating an AR session.", true)
        val session = try {
            Session(appContext)
        } catch (error: Throwable) {
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
            return ArCoreProviderSession(session)
        } catch (error: Throwable) {
            session.close()
            throw ArCoreErrorMapper.map(error)
        }
    }
}
