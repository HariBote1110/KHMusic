package com.example.khmusic

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var musicList: ArrayList<String>
    private lateinit var musicFileList: ArrayList<File> // ファイルパスを保持するリスト
    private lateinit var adapter: ArrayAdapter<String>
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1 // 現在再生中の曲の位置

    private val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.musicListView)
        musicList = ArrayList()
        musicFileList = ArrayList() // ファイルリストの初期化


        // パーミッションチェックとリクエスト
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
        } else {
            loadMusic()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, musicList)
        listView.adapter = adapter


        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                playMusic(position)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusic()
            } else {
                Toast.makeText(this, "ストレージへのアクセス許可が必要です。", Toast.LENGTH_SHORT).show()
                finish() // 許可が得られない場合はアプリを終了
            }
        }
    }


    private fun loadMusic() {
        val musicDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        // musicDirがnullでないか、存在するか、ディレクトリかを確認
        if (musicDir != null && musicDir.exists() && musicDir.isDirectory) {
            searchMusicFiles(musicDir)
            if (musicList.isEmpty()) {
                Toast.makeText(this, "Musicディレクトリに音楽ファイルが見つかりません。", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "Musicディレクトリが見つかりません。", Toast.LENGTH_SHORT).show()
            // 必要であれば、ここで代替のディレクトリを指すか、エラー処理を行う
        }
    }


    private fun searchMusicFiles(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        searchMusicFiles(file) // サブディレクトリも再帰的に検索
                    } else if (file.name.endsWith(".mp3") || file.name.endsWith(".wav")) { // 他の形式も追加可能
                        musicList.add(file.name)
                        musicFileList.add(file) // ファイルパスをリストに追加
                    }
                }
            }
        }
    }


    private fun playMusic(position: Int) {
        if (position == currentPlayingPosition && mediaPlayer?.isPlaying == true) {
            // 同じ曲が再生中の場合は何もしない (一時停止/再開は別のボタンで行う)
            return
        }


        mediaPlayer?.apply {
            stop()
            reset()
            release()
        }

        currentPlayingPosition = position // 再生位置を更新

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(musicFileList[position].absolutePath) // ファイルパスから再生
                prepare()
                start()
            }
            Toast.makeText(this, "${musicFileList[position].name} を再生中", Toast.LENGTH_SHORT).show()


            // 曲の終了を検知
            mediaPlayer?.setOnCompletionListener {
                playNextSong()
            }


        } catch (e: IOException) {
            Toast.makeText(this, "再生中にエラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("KHMusic", "Error playing music", e)
        }
    }

    private fun playNextSong() {
        if (musicFileList.isNotEmpty()) {
            currentPlayingPosition = (currentPlayingPosition + 1) % musicFileList.size
            playMusic(currentPlayingPosition)
        } else {
            Toast.makeText(this,"再生可能な曲がありません", Toast.LENGTH_SHORT).show()
        }
    }


    private fun stopMusic() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset() // reset() を追加して状態をリセット
            // release() はここでは行わない。曲選択時に行う
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.apply {
            if(isPlaying){
                pause()
            }
        }
    }

    private fun resumeMusic() {
        mediaPlayer?.apply {
            if(!isPlaying){
                start()
            }
        }
    }

    // 一時停止ボタンのクリックイベント
    fun onPauseButtonClick(view: View) {
        pauseMusic()
    }

    // 再開ボタンのクリックイベント
    fun onResumeButtonClick(view: View) {
        resumeMusic()
    }

    //停止ボタンの実装
    fun onStopButtonClick(view: View) {
        stopMusic()
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release() // アクティビティ破棄時にリソース解放
        mediaPlayer = null
    }
}