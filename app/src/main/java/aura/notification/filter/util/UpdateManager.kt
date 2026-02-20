package aura.notification.filter.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.tasks.await
import com.google.android.play.core.install.InstallStateUpdatedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UpdateStatus
 * Represents the current state of the in-app update process
 */
enum class UpdateStatus {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    INSTALLING,
    ERROR,
    CANCELED
}

/**
 * UpdateManager
 * Handles Google Play Store in-app update checks and flows for Aura
 */
class UpdateManager(private val activity: Activity) {
    
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val tag = "UpdateManager"
    
    // Update state management
    private val _status = MutableStateFlow(UpdateStatus.IDLE)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // Listener for flexible updates
    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                if (totalBytes > 0) {
                    _progress.value = bytesDownloaded.toFloat() / totalBytes.toFloat()
                }
                _status.value = UpdateStatus.DOWNLOADING
                Log.d(tag, "Downloading: ${_progress.value * 100}%")
            }
            InstallStatus.DOWNLOADED -> {
                _status.value = UpdateStatus.DOWNLOADED
                _progress.value = 1f
                Log.d(tag, "Update downloaded")
            }
            InstallStatus.INSTALLING -> {
                _status.value = UpdateStatus.INSTALLING
                Log.d(tag, "Installing update")
            }
            InstallStatus.FAILED -> {
                _status.value = UpdateStatus.ERROR
                Log.e(tag, "Update failed: ${state.installErrorCode()}")
            }
            InstallStatus.CANCELED -> {
                _status.value = UpdateStatus.CANCELED
                Log.d(tag, "Update canceled")
            }
            else -> {
                Log.d(tag, "Install status: ${state.installStatus()}")
            }
        }
    }
    
    init {
        appUpdateManager.registerListener(installListener)
    }
    
    fun unregister() {
        appUpdateManager.unregisterListener(installListener)
    }
    
    suspend fun checkForUpdate(): AppUpdateInfo? {
        _status.value = UpdateStatus.CHECKING
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                _status.value = UpdateStatus.AVAILABLE
                Log.d(tag, "Update available: ${appUpdateInfo.availableVersionCode()}")
                appUpdateInfo
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.d(tag, "Update already in progress")
                // RESUME LOGIC: If it's already downloading, reflect that in the UI
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADING) {
                     val bytesDownloaded = appUpdateInfo.bytesDownloaded()
                     val totalBytes = appUpdateInfo.totalBytesToDownload()
                     if (totalBytes > 0) {
                         _progress.value = bytesDownloaded.toFloat() / totalBytes.toFloat()
                     }
                     _status.value = UpdateStatus.DOWNLOADING
                }
                appUpdateInfo
            } else {
                _status.value = UpdateStatus.IDLE
                Log.d(tag, "No update available.")
                null
            }
        } catch (e: Exception) {
            _status.value = UpdateStatus.ERROR
            Log.e(tag, "Error checking for update", e)
            null
        }
    }
    
    fun startFlexibleUpdate(appUpdateInfo: AppUpdateInfo): Boolean {
        return try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.FLEXIBLE,
                activity,
                UPDATE_REQUEST_CODE
            )
            true
        } catch (e: Exception) {
            Log.e(tag, "Error starting flexible update", e)
            false
        }
    }
    
    suspend fun isUpdateInProgress(): Boolean {
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            val isDownloaded = appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED
            val isDownloading = appUpdateInfo.installStatus() == InstallStatus.DOWNLOADING

            if (isDownloaded) {
                _status.value = UpdateStatus.DOWNLOADED
                _progress.value = 1f
            } else if (isDownloading) {
                _status.value = UpdateStatus.DOWNLOADING
                // Progress will be updated by the listener we registered in init
            }
            isDownloaded || isDownloading
        } catch (e: Exception) {
            Log.e(tag, "Error checking update status", e)
            false
        }
    }
    
    fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(tag, "Error completing update", e)
        }
    }
    
    companion object {
        const val UPDATE_REQUEST_CODE = 1001
    }
}
