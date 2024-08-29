package com.example.myapplication.live_wallpapers

import android.content.Context
import android.graphics.Canvas
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.media.MediaPlayer
import android.net.Uri
import java.io.IOException


class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    private inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)

            val sharedPreferences = getSharedPreferences("live_wallpaper_prefs", Context.MODE_PRIVATE)
            val videoPath = sharedPreferences.getString("video_path", null)

            if (videoPath != null) {
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
                        setDataSource(applicationContext, Uri.parse(videoPath))
                        setSurface(holder.surface)
                        prepareAsync()
                    } catch (e: IOException) {
                        // Handle exception
                    }
                }
            } else {
                stopSelf()
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
