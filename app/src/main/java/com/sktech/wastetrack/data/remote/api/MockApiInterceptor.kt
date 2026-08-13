package com.sktech.wastetrack.data.remote.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class MockApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        val (responseCode, responseJson) = when {
            path.contains("api/v1/metrics/scrap") -> {
                200 to """{"status":"success", "message":"Metrics synced to Forge"}"""
            }
            path.contains("api/v1/assets/health") -> {
                200 to """{"status":"healthy", "score":98}"""
            }
            path.contains("api/v1/certificates/submit") -> {
                200 to """{"status":"success", "message":"Certificate recorded with MPCB"}"""
            }
            path.contains("api/v1/guidelines/latest") -> {
                200 to """{"version":"1.2", "details":"No new changes"}"""
            }
            else -> {
                404 to """{"error":"Not found"}"""
            }
        }

        return Response.Builder()
            .code(responseCode)
            .message("Mock Response")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(responseJson.toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .build()
    }
}
