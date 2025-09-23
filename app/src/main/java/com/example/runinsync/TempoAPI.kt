package com.example.runinsync

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query



data class TempoApiResponse (
    val bpm : Int
)

interface TempoApiService {
    @GET("search/")
    suspend fun getTempo(
        @Query("api_key") apiKey: String,
        @Query("type") type: String,
        @Query("lookup") lookup: String,
        @Query("limit") limit: Int
    ): Response<TempoApiResponse>
}


