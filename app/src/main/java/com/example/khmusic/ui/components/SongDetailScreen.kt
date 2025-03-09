// ui/components/SongDetailScreen.kt
package com.example.khmusic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.khmusic.MusicPlayerViewModel
import com.example.khmusic.dto.MusicItem
import com.example.khmusic.utils.formatTime


@Composable
fun SongDetailScreen(viewModel: MusicPlayerViewModel, onBack: () -> Unit) {
    val selectedSong by viewModel.selectedSong.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val currentDuration by viewModel.currentDuration.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()


    selectedSong?.let { song ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album art
            Image(
                painter = rememberAsyncImagePainter(model = song.albumArtUri),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(240.dp)
                    .padding(bottom = 16.dp)
            )

            // Song title
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Artist name
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current position / Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentPosition))
                Text(text = formatTime(currentDuration))
            }
            // Seek bar
            SeekBar(viewModel = viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // File format (Dummy data for now)
            // Text(text = "WAV 192 kHz / 24 bit HR", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(32.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                IconButton(onClick = { /* シャッフル */ }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                }
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = { viewModel.playPause() }) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = { /* リピート */}) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat")
                }

            }
        }
    }
}