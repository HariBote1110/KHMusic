// ui/components/SeekBar.kt
package com.example.khmusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.khmusic.MusicPlayerViewModel
import com.example.khmusic.utils.formatTime

@Composable
fun SeekBar(viewModel: MusicPlayerViewModel) {
    val progress by viewModel.currentPosition.collectAsState()
    val duration by viewModel.currentDuration.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // ドラッグ終了時にシーク
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            viewModel.seekTo(sliderPosition.toLong())
        }
    }

    // ViewModel の値が変化したら sliderPosition を更新 (ドラッグ中でない場合)
    LaunchedEffect(progress) {
        if (!isDragging) {
            sliderPosition = progress.toFloat()
        }
    }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                isDragging = true // ドラッグ開始
            },
            onValueChangeFinished = {
                isDragging = false // ドラッグ終了
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)  // Slider の高さを明示的に指定
                .padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(progress))
            Text(text = formatTime(duration))
        }
    }
}