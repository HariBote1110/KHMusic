package com.example.khmusic.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.example.khmusic.MusicPlayerViewModel
import com.example.khmusic.dto.MusicItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(viewModel: MusicPlayerViewModel, onSongClick: (MusicItem) -> Unit) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    //rememberCoroutineScopeを使ってCoroutineScopeを取得
    val scope = rememberCoroutineScope()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.bindService(context)
            scope.launch { // コルーチンスコープ内で launch
                viewModel.loadMusic(context)
            }
        } else {
            // Handle permission denied
        }
    }

    // パーミッションの状態を監視し、変更があった場合、またはviewModelが変更された場合にのみ処理を実行
    val permissionGranted = remember { mutableStateOf(false) }
    LaunchedEffect(key1 = permissionGranted.value, key2 = viewModel) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!permissionGranted.value) { // 初回許可時のみ実行
                permissionGranted.value = true
                viewModel.bindService(context)
                viewModel.viewModelScope.launch { //viewModelScopeでlaunch
                    viewModel.loadMusic(context)
                }
            }
        } else {
            // パーミッションが許可されていない場合は、要求
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "KHMusic",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = currentSong?.albumArtUri),
                            contentDescription = "Album Art",
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),

                            ) {
                            Text(
                                text = currentSong?.title ?: "No title",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = currentSong?.artist ?: "No artist",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End

                    )
                    {
                        IconButton(onClick = { viewModel.playPrevious() }) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                        }
                        IconButton(onClick = { viewModel.playPause() }) {
                            Icon(
                                if (isPlaying) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play"
                            )
                        }
                        IconButton(onClick = { viewModel.playNext() }) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SongList(viewModel = viewModel, onSongClick = { song ->
                viewModel.playMusicItem(song)
            })

            SeekBar(viewModel = viewModel)
            Spacer(Modifier.height(16.dp))
        }
    }
    DisposableEffect(Unit) {
        viewModel.bindService(context)
        onDispose {
            viewModel.unbindService(context)
        }
    }
}