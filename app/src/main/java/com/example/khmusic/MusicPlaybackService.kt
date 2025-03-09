package com.example.khmusic.service

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.khmusic.dto.MusicItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
class MusicPlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private var mediaPlayer: MediaPlayer? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _currentSong = MutableStateFlow<MusicItem?>(null)
    val currentSong: StateFlow<MusicItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration.asStateFlow()


    private var musicList: List<MusicItem> = emptyList()
    private var currentSongIndex = 0
    private var updatePositionJob: Job? = null

    // Binderの実装
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnErrorListener(this)

        // AudioAttributes の設定 (Android 5.0 以降で推奨)
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        // WakeLock の設定 (バックグラウンド再生中に画面が消えても再生を続けるため)
        mediaPlayer?.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

    }

    // 曲のセット
    fun setMusicList(list: List<MusicItem>) {
        musicList = list
        if (musicList.isNotEmpty()) { // リストが空でない場合のみ処理
            currentSongIndex = 0 // リストがセットされたら最初の曲にリセット
            _currentSong.value = musicList[currentSongIndex] // 最初の曲をセット
        }
    }


    fun playSong(song: MusicItem) {
        // _currentSong.value = song // ここでは更新しない。onPreparedで更新

        try {
            mediaPlayer?.reset()
            // Content URI を使用
            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id
            )
            mediaPlayer?.setDataSource(applicationContext, contentUri) // Context を渡す
            mediaPlayer?.prepareAsync() // 非同期で準備
            //startUpdatingPosition() // onPrepared に移動

            // 現在再生中の曲のインデックスを更新
            val index = musicList.indexOf(song)
            if(index != -1) {
                currentSongIndex = index
            }


        } catch (e: Exception) {
            Log.e("MusicPlaybackService", "Error playing song: ${e.message}", e)
            _isPlaying.value = false  // エラーが発生したら再生状態を false に
        }
    }
    fun playNext() {
        if (musicList.isEmpty()) return
        currentSongIndex = (currentSongIndex + 1) % musicList.size
        playSong(musicList[currentSongIndex])
        Log.d("MusicPlaybackService","playNext Called")

    }

    fun playPrevious() {
        if (musicList.isEmpty()) return

        currentSongIndex = (currentSongIndex - 1 + musicList.size) % musicList.size
        playSong(musicList[currentSongIndex])
        Log.d("MusicPlaybackService", "playPrevious Called")
    }

    fun playPause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopUpdatingPosition()
        } else {
            // mediaPlayerがnull、または再生準備ができていない場合
            if(mediaPlayer == null || _currentDuration.value == 0L){
                if(musicList.isNotEmpty()){
                    playSong(musicList[currentSongIndex]) //現在の曲、またはリストの最初の曲を再生
                }
            } else{
                mediaPlayer?.start()
                _isPlaying.value = true
                startUpdatingPosition()
            }
        }
        Log.d("MusicPlaybackService", "playPause() called isPlaying: ${_isPlaying.value}")
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt()) // MediaPlayer.seekTo() は Int を取る
        _currentPosition.value = position // UIも更新
        Log.d("MusicPlaybackService","seekTo Called")
    }

    override fun onPrepared(mp: MediaPlayer?) {
        // mp?.start() // ここで start() しない (playPause() で制御)
        _isPlaying.value = true // 再生可能状態にする
        _currentDuration.value = mp?.duration?.toLong() ?: 0L
        _currentSong.value = musicList[currentSongIndex] // 現在の曲情報を更新
        startUpdatingPosition()
        Log.d("MusicPlaybackService", "onPrepared: Song duration = ${_currentDuration.value}")
        playPause() // 一時停止状態から開始
    }


    override fun onCompletion(mp: MediaPlayer?) {
        playNext() // 曲が終了したら次の曲を再生
    }


    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e("MusicPlaybackService", "MediaPlayer Error: what=$what, extra=$extra")
        _isPlaying.value = false
        return true // エラーを処理したことを示す
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdatingPosition() // ジョブのキャンセル
        mediaPlayer?.release()
        mediaPlayer = null
        serviceJob.cancel() // コルーチンスコープをキャンセル
    }

    private fun startUpdatingPosition() {
        updatePositionJob?.cancel() // 既存のジョブがあればキャンセル
        updatePositionJob = serviceScope.launch {
            while (true) { // isPlaying に関係なく、常に位置情報を更新
                _currentPosition.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(500) // 500ms ごとに更新
            }
        }
    }

    private fun stopUpdatingPosition() {
        updatePositionJob?.cancel()
        updatePositionJob = null
    }
}