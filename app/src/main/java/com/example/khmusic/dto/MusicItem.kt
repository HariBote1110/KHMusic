// dto/MusicItem.kt
package com.example.khmusic.dto

import android.net.Uri

data class MusicItem(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val duration: Long
)