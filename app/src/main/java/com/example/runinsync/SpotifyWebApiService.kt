package com.example.runinsync

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Query



interface SpotifyApiService {
    @GET("v1/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authHeader: String
    ): SpotifyUser

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 20,
        @Query("time_range") timeRange: String = "medium_term"
    ): UserTopTracksResponse
}

object SpotifyApi {
    private const val BASE_URL = "https://api.spotify.com/"

    val retrofitService: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Converts JSON to Kotlin objects
            .build()
            .create(SpotifyApiService::class.java)
    }
}


// DATA MODELS
data class SpotifyUser(
    val id: String,
    @SerializedName("display_name") val displayName: String,
    val email: String,
    val country: String
)

data class Track(
    val id: String,
    val name: String,
    val artists: List<Artist>,
    val album: Album,
    @SerializedName("duration_ms") val durationMs: Long
)

data class Artist(
    val id: String,
    val name: String
)

data class Album(
    val id: String,
    val name: String,
)

data class UserTopTracksResponse(
    val items: List<Track>
) {
    override fun toString(): String {
        if (items.isEmpty()) return "No tracks found."

        // This joins each track into a new line with a bullet point
        return items.joinToString(separator = "\n") { track ->
            "• ${track.name} by ${track.artists.joinToString { it.name }}"
        }
    }
}