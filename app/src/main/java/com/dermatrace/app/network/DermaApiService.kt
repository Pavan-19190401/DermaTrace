package com.dermatrace.app.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface DermaApiService {

    @GET("health")
    suspend fun getHealth(): Response<Map<String, Any>>

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("compare")
    suspend fun compareImages(
        @Part baseline: MultipartBody.Part,
        @Part followup: MultipartBody.Part
    ): Response<CompareResponse>

    @GET("admin/patients")
    suspend fun getAdminPatients(
        @Header("Authorization") token: String
    ): Response<List<PatientRecord>>

    @GET("admin/metrics")
    suspend fun getMetrics(
        @Header("Authorization") token: String
    ): Response<MetricsResponse>
}
