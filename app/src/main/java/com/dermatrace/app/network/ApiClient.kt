package com.dermatrace.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // When testing on the Android Emulator, 10.0.2.2 connects to your PC's localhost:8000
    // If running on a physical phone via Wi-Fi, update this to your PC's IPv4 address or Cloud Render URL
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8000/api/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            retrofitInstance = null
        }

    var authToken: String? = null
    var activeRole: String = "client" // "client" (Patient) or "admin" (Clinician)
    var currentUserEmail: String = "patient@dermatrace.com"

    private var retrofitInstance: Retrofit? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private fun getRetrofit(): Retrofit {
        return retrofitInstance ?: synchronized(this) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { retrofitInstance = it }
        }
    }

    val service: DermaApiService
        get() = getRetrofit().create(DermaApiService::class.java)

    fun isAdmin(): Boolean = activeRole.equals("admin", ignoreCase = true)
}
