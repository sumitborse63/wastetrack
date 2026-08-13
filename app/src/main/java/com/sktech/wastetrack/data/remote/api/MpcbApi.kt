package com.sktech.wastetrack.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MpcbApi {
    
    @POST("api/v1/certificates/submit")
    suspend fun submitCertificate(@Body payload: Map<String, Any>): Map<String, String>

    @GET("api/v1/guidelines/latest")
    suspend fun getLatestGuidelines(): Map<String, Any>
}
