package com.example.myapplication.live_wallpapers

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.media.MediaPlayer
import android.net.Uri
import java.io.IOException

class VideoWallpaperService : WallpaperService() {

    companion object {
        var videoUri: Uri? = null
    }

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    private inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            updateMediaPlayer(holder)
        }

        private fun updateMediaPlayer(holder: SurfaceHolder) {
            mediaPlayer?.release() // Release previous mediaPlayer if exists

            videoUri?.let { uri ->
                mediaPlayer = MediaPlayer().apply {
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.start()
                    }
                    setOnErrorListener { _, _, _ ->
                        // Handle error
                        false
                    }
                    try {
                        setDataSource(applicationContext, uri)
                        setSurface(holder.surface)
                        prepareAsync()
                    } catch (e: IOException) {
                        // Handle exception
                        e.printStackTrace()
                    }
                }
            } ?: run {
                stopSelf() // Stop the service if no URI is set
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            mediaPlayer?.release()
            mediaPlayer = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                mediaPlayer?.start()
            } else {
                mediaPlayer?.pause()
            }
        }
    }
}
