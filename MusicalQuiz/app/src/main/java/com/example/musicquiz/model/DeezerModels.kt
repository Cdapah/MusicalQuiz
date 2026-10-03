package com.example.musicquiz.model

import com.google.gson.annotations.SerializedName

// Response wrapper for Deezer API
data class DeezerResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("total") val total: Int,
    @SerializedName("prev") val prev: String?,
    @SerializedName("next") val next: String?
)

// Track model from Deezer API
data class Track(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("preview") val previewUrl: String?,
    @SerializedName("artist") val artist: Artist,
    @SerializedName("album") val album: Album
)

// Album model from Deezer API
data class Album(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val coverSmall: String?,
    @SerializedName("cover_medium") val coverMedium: String?,
    @SerializedName("cover_big") val coverBig: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("artist") val artist: Artist? = null
)

// Artist model from Deezer API
data class Artist(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("picture") val pictureSmall: String?,
    @SerializedName("picture_medium") val pictureMedium: String?,
    @SerializedName("picture_big") val pictureBig: String?
)

// Album details with tracks
data class AlbumDetails(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("cover") val coverSmall: String?,
    @SerializedName("cover_medium") val coverMedium: String?,
    @SerializedName("cover_big") val coverBig: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("artist") val artist: Artist,
    @SerializedName("tracks") val tracks: DeezerResponse<Track>
)
