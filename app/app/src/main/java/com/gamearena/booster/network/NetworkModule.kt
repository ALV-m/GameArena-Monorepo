package com.gamearena.booster.network

import android.content.Context
import com.gamearena.booster.auth.AuthManager
import com.gamearena.booster.overlay.ScoreOcr
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://gamearena-api.onrender.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authManager: dagger.Lazy<AuthManager>): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = authManager.get().getTokenBlocking()
            val request = if (!token.isNullOrEmpty()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGameArenaApi(retrofit: Retrofit): GameArenaApi {
        return retrofit.create(GameArenaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideScoreOcr(@ApplicationContext context: Context): ScoreOcr {
        return ScoreOcr(context)
    }
}
