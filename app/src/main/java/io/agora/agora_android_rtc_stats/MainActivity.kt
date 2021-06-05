package io.agora.agora_android_rtc_stats

import android.Manifest
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay


class MainActivity : AppCompatActivity() {

    private var mRtcEngine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()
        initAgoraEngineAndJoinChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 22)
    }

    fun initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine()
        setupVideoProfile()
        setupLocalVideo()
        joinChannel()
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { onRemoteUserLeft() }
        }


        override fun onRtcStats(stats: RtcStats?) {
            runOnUiThread { updateTextView(message = "Rtc Stats - CPU Usage: ${stats?.cpuAppUsage}, Memory Usage:  ${stats?.memoryAppUsageRatio}, Sent: ${stats?.txBytes}, Recv: ${stats?.rxBytes}, users: ${stats?.users}, ") }
        }

        override fun onLocalVideoStats(stats: LocalVideoStats?) {
            runOnUiThread { updateTextView(message = "Local Video Stats - Sent Bitrate: ${stats?.sentBitrate}, Target Bitrate: ${stats?.targetBitrate}, Codec: ${stats?.codecType}, Packet Loss Rate: ${stats?.txPacketLossRate}") }
        }

        override fun onRemoteVideoStats(stats: RemoteVideoStats?) {
            runOnUiThread { updateTextView(message = "Remote Video Stats - UID: ${stats?.uid}, Width: ${stats?.width}, Height: ${stats?.height}, Received Bitrate: ${stats?.receivedBitrate}") }
        }

    }

    private fun initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, "<--Add your App Id here-->", mRtcEventHandler)
            println("\nmRtcEngine initialized")
        } catch (e: Exception) {
            println("Exception while initializing AgoraRtcEngine: $e")
        }
    }

    private fun setupVideoProfile() {
        mRtcEngine!!.enableVideo()

        mRtcEngine!!.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }

    private fun setupLocalVideo() {
        val container = findViewById<View>(R.id.local_video_view_container) as FrameLayout
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)
        mRtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun joinChannel() {
        mRtcEngine!!.joinChannel(
            null,
            "test",
            null,
            0
        )
    }

    private fun setupRemoteVideo(uid: Int) {
        val container =
            findViewById<View>(R.id.remote_video_view_container) as FrameLayout
        if (container.childCount >= 1) {
            return
        }
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        container.addView(surfaceView)
        mRtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        surfaceView.tag = uid
    }

    private fun leaveChannel() {
        mRtcEngine!!.leaveChannel()
    }

    private fun onRemoteUserLeft() {
        val container =
            findViewById<View>(R.id.remote_video_view_container) as FrameLayout
        container.removeAllViews()
    }

    fun onLocalAudioMuteClicked(view: View?) {
        val iv = view as ImageView
        if (iv.isSelected) {
            iv.isSelected = false
            iv.clearColorFilter()
        } else {
            iv.isSelected = true
            iv.setColorFilter(resources.getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY)
        }

        // Stops/Resumes sending the local audio stream.
        mRtcEngine!!.muteLocalAudioStream(iv.isSelected)
    }

    fun updateTextView(message: String?){
        val tv = findViewById<View>(R.id.rtc_text) as TextView
        tv.text = message
    }


    fun onSwitchCameraClicked(view: View?) {
        mRtcEngine!!.switchCamera()
    }

    fun onEncCallClicked(view: View?) {
        finish()
    }
}