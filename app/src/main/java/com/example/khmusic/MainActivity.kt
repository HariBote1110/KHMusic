package com.example.khmusic

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.khmusic.ui.theme.KHMusicTheme
import com.example.khmusic.ui.components.MusicPlayerScreen
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import com.example.khmusic.ui.components.SongDetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KHMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ViewModel のインスタンスを取得
                    val viewModel: MusicPlayerViewModel = viewModel()
                    // 画面表示の切り替えを管理する状態変数
                    var showDetail by remember { mutableStateOf(false) }

                    if (showDetail) {
                        SongDetailScreen(viewModel = viewModel, onBack = { showDetail = false })
                    } else {
                        MusicPlayerScreen(viewModel = viewModel, onSongClick = {
                            viewModel.playMusicItem(it)
                            showDetail = true
                        })
                    }
                }
            }
        }
    }
}