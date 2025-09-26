package com.example.runinsync

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api.getsong.co/"

    val api: TempoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Or your preferred converter
            .build()
            .create(TempoApiService::class.java)
    }
}