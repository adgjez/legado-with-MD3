package io.legado.app.ui.main.ai

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityAiVideoPlayerBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File

class AiVideoPlayerActivity : BaseActivity<ActivityAiVideoPlayerBinding>() {

    override val binding by viewBinding(ActivityAiVideoPlayerBinding::inflate)

    private var player: ExoPlayer? = null
    private var videoPath: String = ""
    private var isFullScreen = false

    companion object {
        const val EXTRA_VIDEO_PATH = "videoPath"
        const val EXTRA_VIDEO_NAME = "videoName"

        fun start(context: Context, videoPath: String, videoName: String? = null) {
            context.startActivity(Intent(context, AiVideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                putExtra(EXTRA_VIDEO_NAME, videoName)
            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH).orEmpty()
        val videoName = intent.getStringExtra(EXTRA_VIDEO_NAME).orEmpty()

        if (videoPath.isBlank() || !File(videoPath).exists()) {
            finish()
            return
        }

        title = videoName.ifBlank { File(videoPath).name }

        initPlayer()
        initFullScreenToggle()

        onBackPressedDispatcher.addCallback(this) {
            if (isFullScreen) {
                exitFullScreen()
            } else {
                finish()
            }
        }
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(File(videoPath).toURI().toString())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = false
                    }
                }
            })
        }
    }

    private fun initFullScreenToggle() {
        binding.playerView.setFullscreenButtonClickListener { toggleFullScreen() }
    }

    private fun toggleFullScreen() {
        if (isFullScreen) {
            exitFullScreen()
        } else {
            enterFullScreen()
        }
    }

    private fun enterFullScreen() {
        isFullScreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        hideSystemBars()
        binding.playerView.useController = true
    }

    private fun exitFullScreen() {
        isFullScreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        showSystemBars()
        binding.playerView.useController = true
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, binding.playerView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars() {
        WindowCompat.getInsetsController(window, binding.playerView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // PlayerView handles resize automatically
    }
}