package com.example.musicquiz.api

import com.example.musicquiz.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Deezer API
 */
interface DeezerApiService {

    /**
     * Search for tracks
     */
    @GET("search/track")
    suspend fun searchTracks(
        @Query("q") query: String
    ): Response<DeezerResponse<Track>>

    /**
     * Search for albums
     */
    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String
    ): Response<DeezerResponse<Album>>

    /**
     * Get track details
     */
    @GET("track/{id}")
    suspend fun getTrack(
        @Path("id") id: Long
    ): Response<Track>

    /**
     * Get album details with tracks
     */
    @GET("album/{id}")
    suspend fun getAlbum(
        @Path("id") id: Long
    ): Response<AlbumDetails>

    /**
     * Get artist details
     */
    @GET("artist/{id}")
    suspend fun getArtist(
        @Path("id") id: Long
    ): Response<Artist>
}
