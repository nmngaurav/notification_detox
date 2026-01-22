package aura.notification.filter.data.repository

import aura.notification.filter.data.SettingsDataStore
import aura.notification.filter.data.remote.ConfigService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class ConfigRepository @Inject constructor(
    private val configService: ConfigService,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val CLIENT_AUTH = "bitart-client-2025"
    }
    // In-memory cache for synchronous access (Interceptor)
    // Initialize with a fallback or empty string
    private var cachedApiKey: String = ""

    suspend fun initialize() {
        // 1. Load from local storage first
        val localKey = settingsDataStore.apiKey.first()
        if (!localKey.isNullOrEmpty()) {
            cachedApiKey = localKey
        }
        
        // 2. Fetch from remote
        try {
            val response = configService.getAppConfig(CLIENT_AUTH)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success == true) {
                    val remoteKey = body.data?.apiKey
                    if (!remoteKey.isNullOrEmpty() && remoteKey != cachedApiKey) {
                        cachedApiKey = remoteKey
                        settingsDataStore.setApiKey(remoteKey)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error, keep using local key
            e.printStackTrace()
        }
    }

    fun getApiKey(): String {
        return cachedApiKey
    }
}
