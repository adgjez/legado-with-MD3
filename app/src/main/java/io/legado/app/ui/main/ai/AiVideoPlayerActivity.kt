package io.legado.app.ui.main.ai

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.core.net.toUri
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityAiVideoPlayerBinding
import io.legado.app.help.gsyVideo.VideoPlayer
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File

class AiVideoPlayerActivity : BaseActivity<ActivityAiVideoPlayerBinding>() {

    override val binding by viewBinding(ActivityAiVideoPlayerBinding::inflate)

    private var orientationUtils: OrientationUtils? = null
    private var videoPath: String = ""

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

        initVideoPlayer(videoPath)

        onBackPressedDispatcher.addCallback(this) {
            if (orientationUtils != null) {
                orientationUtils?.backToProtVideo()
            }
            finish()
        }
    }

    private fun initVideoPlayer(path: String) {
        val player = binding.videoPlayer

        orientationUtils = OrientationUtils(this, player).apply {
            isEnable = true
        }

        player.setUp(path, true, File(path).name)

        player.setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onPrepared(url: String?, vararg objects: Any?) {
                super.onPrepared(url, *objects)
                orientationUtils?.isEnable = true
            }

            override fun onQuitFullscreen(url: String?, vararg objects: Any?) {
                super.onQuitFullscreen(url, *objects)
                orientationUtils?.backToProtVideo()
            }
        })

        player.startPlayLogic()
    }

    override fun onPause() {
        super.onPause()
        binding.videoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        binding.videoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationUtils?.releaseListener()
        GSYVideoManager.releaseAllVideos()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationUtils?.setEnable(false)
    }
}