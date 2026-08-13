package com.sktech.wastetrack.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface HoneywellApi {
    
    @POST("api/v1/metrics/scrap")
    suspend fun postScrapMetrics(@Body payload: Map<String, Any>): Map<String, Any>

    @GET("api/v1/assets/health")
    suspend fun getAssetHealth(): Map<String, Any>
}
