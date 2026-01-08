package com.aura.di

import com.aura.ai.OpenAIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenAIModule {

    @Provides
    @Singleton
    fun provideOpenAIClient(configRepository: com.aura.data.repository.ConfigRepository): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Dynamic Key Injection from Repository (cached)
                val apiKey = "Bearer ${configRepository.getApiKey()}"
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", apiKey)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIService(client: OkHttpClient): OpenAIService {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
    }

    @Provides
    @Singleton
    fun provideConfigService(): com.aura.data.remote.ConfigService {
        return Retrofit.Builder()
            .baseUrl("https://bitart-apps.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.aura.data.remote.ConfigService::class.java)
    }
}
