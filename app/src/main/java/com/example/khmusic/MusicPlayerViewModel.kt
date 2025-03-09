package com.example.khmusic

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khmusic.dto.MusicItem
import com.example.khmusic.service.MusicPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _musicList = MutableStateFlow<List<MusicItem>>(emptyList())
    val musicList: StateFlow<List<MusicItem>> = _musicList.asStateFlow()

    private val _currentSong = MutableStateFlow<MusicItem?>(null)
    val currentSong: StateFlow<MusicItem?> = _currentSong.asStateFlow()

    // 詳細表示用の StateFlow
    private val _selectedSong = MutableStateFlow<MusicItem?>(null)
    val selectedSong: StateFlow<MusicItem?> = _selectedSong.asStateFlow()


    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()

    val currentSongTitle = mutableStateOf("")
    val currentArtistName = mutableStateOf("")
    val currentAlbumArtUri = mutableStateOf<Uri?>(null)


    private var musicPlaybackService: MusicPlaybackService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicPlaybackService.LocalBinder
            musicPlaybackService = binder.getService()
            serviceBound = true
            initializeMusicList()
            observeService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBound = false
            musicPlaybackService = null
        }
    }

    fun bindService(context: Context) {
        if (!serviceBound) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService(context: Context) {
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }


    private fun observeService() {
        viewModelScope.launch {
            musicPlaybackService?.currentSong?.collect { song ->
                _currentSong.value = song
                currentSongTitle.value = song?.title ?: ""
                currentArtistName.value = song?.artist ?: ""
                currentAlbumArtUri.value = song?.albumArtUri
                // サービスから曲が変わった場合は、選択状態を解除
                if (song != _selectedSong.value) {
                    _selectedSong.value = null
                }
            }
        }
        viewModelScope.launch {
            musicPlaybackService?.isPlaying?.collect { isPlaying ->
                _isPlaying.value = isPlaying
            }
        }

        viewModelScope.launch {
            musicPlaybackService?.currentPosition?.collect { position ->
                _currentPosition.value = position
            }
        }

        viewModelScope.launch {
            musicPlaybackService?.currentDuration?.collect { duration ->
                _currentDuration.value = duration
            }
        }
    }


    private fun initializeMusicList() {
        viewModelScope.launch {
            val musicItems = loadMusic(getApplication())
            _musicList.value = musicItems
            musicPlaybackService?.setMusicList(musicItems)
        }
    }

    suspend fun loadMusic(context: Context): List<MusicItem> = withContext(Dispatchers.IO) {
        Log.d("MusicPlayerViewModel", "loadMusic() called") // 関数が呼ばれたことを確認
        val musicList = mutableListOf<MusicItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        if (cursor == null) {
            Log.e("MusicPlayerViewModel", "Cursor is null") // クエリが null を返した場合
        } else if (!cursor.moveToFirst()) {
            Log.e("MusicPlayerViewModel", "Cursor is empty") // カーソルが空の場合
        } else {
            Log.d("MusicPlayerViewModel", "Cursor has data") // データがある場合
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            do { // do-while ループに変更 (moveToFirst() が true の場合、必ず1回は実行)
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val albumArtUri = getAlbumArtUri(context, albumId)

                Log.d("MusicPlayerViewModel", "Song: id=$id, title=$title, artist=$artist, albumId=$albumId, duration=$duration, albumArtUri=$albumArtUri") // 各曲の情報を出力

                musicList.add(MusicItem(id, title, artist, albumArtUri, duration))
            } while (cursor.moveToNext())
        }

        cursor?.close() // カーソルは必ず閉じる
        Log.d("MusicPlayerViewModel", "Loaded ${musicList.size} songs") // 読み込んだ曲数を出力
        _musicList.value = musicList
        musicList // return を削除
    }

    private fun getAlbumArtUri(context: Context, albumId: Long): Uri? { // 戻り値の型を Uri? に変更
        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
        return Uri.withAppendedPath(sArtworkUri, albumId.toString()) // albumId を String に変換
    }


    fun playMusicItem(musicItem: MusicItem) {
        musicPlaybackService?.playSong(musicItem)
        _currentSong.value = musicItem // 現在の曲を更新
    }


    fun playPause() {
        musicPlaybackService?.playPause()
    }

    fun playNext() {
        musicPlaybackService?.playNext()
    }

    fun playPrevious() {
        musicPlaybackService?.playPrevious()
    }

    fun seekTo(position: Long) {
        musicPlaybackService?.seekTo(position)
    }

    // 曲が選択されたときに呼ばれる
    fun selectSong(musicItem: MusicItem) {
        _selectedSong.value = musicItem
    }

    // 詳細画面を閉じるときに呼ばれる
    fun clearSelectedSong() {
        _selectedSong.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
    }
}