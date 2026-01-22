package aura.notification.filter.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ConfigService {
    @GET("api/v1/summarify-api-key")
    suspend fun getAppConfig(
        @Query("clientAuth") clientAuth: String
    ): Response<SummarifyConfigResponse>
}

data class SummarifyConfigResponse(
    val success: Boolean,
    val data: SummarifyData?,
    val error: String?
)

data class SummarifyData(
    val apiKey: String?
)
