package zju.me

import android.hardware.Camera
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.faucamp.simplertmp.RtmpHandler
import net.ossrs.yasea.SrsCameraView
import net.ossrs.yasea.SrsEncodeHandler
import net.ossrs.yasea.SrsPublisher
import net.ossrs.yasea.SrsRecordHandler
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.SocketException

class CameraActivity : AppCompatActivity(), RtmpHandler.RtmpListener,
    SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private val width : Int = 1920
    private val height : Int = 1080
    private lateinit var publisher : SrsPublisher
    private lateinit var cameraView : SrsCameraView
    private lateinit var address : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 相机预览
        cameraView = findViewById(R.id.cameraView)
        cameraView.setCameraCallbacksHandler(object : SrsCameraView.CameraCallbacksHandler() {
            override fun onCameraParameters(params: Camera.Parameters?) {
                super.onCameraParameters(params)
            }
        })
        // 视频流发布
        publisher = SrsPublisher(cameraView)
        publisher.setEncodeHandler(SrsEncodeHandler(this))
        publisher.setRtmpHandler(RtmpHandler(this))
        publisher.setRecordHandler(SrsRecordHandler(this))
        publisher.setPreviewResolution(width, height)
        publisher.setOutputResolution(height, width)
        publisher.switchToHardEncoder()
        publisher.setVideoHDMode()
        publisher.startCamera()
        // 视频流发布地址
        address = intent.extras?.get("address").toString()
        // 开始推流
        publisher.startPublish(address)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStart() {
        super.onStart()
        if (publisher.camera == null) publisher.startCamera()
    }

    override fun onDestroy() {
        publisher.stopPublish()
        super.onDestroy()
    }

    private fun handleException(e : Exception?) {
        Toast.makeText(this, "异常 " + e?.message, Toast.LENGTH_LONG)
        publisher.stopPublish()
    }

    override fun onRtmpConnecting(msg: String?) {
        Toast.makeText(this, "连接中", Toast.LENGTH_LONG).show()
    }
    override fun onRtmpConnected(msg: String?) {
        Toast.makeText(this, "连接成功", Toast.LENGTH_LONG).show()
    }
    override fun onRtmpVideoStreaming() {}
    override fun onRtmpAudioStreaming() {}
    override fun onRtmpStopped() {
        Toast.makeText(this, "停止推送", Toast.LENGTH_LONG).show()
    }
    override fun onRtmpDisconnected() {
        Toast.makeText(this, "断开连接", Toast.LENGTH_LONG).show()
    }
    override fun onRtmpVideoFpsChanged(fps: Double) {}
    override fun onRtmpVideoBitrateChanged(bitrate: Double) {}
    override fun onRtmpAudioBitrateChanged(bitrate: Double) {}
    override fun onRtmpSocketException(e: SocketException?) {}
    override fun onRtmpIOException(e: IOException?) {
        handleException(e)
    }
    override fun onRtmpIllegalArgumentException(e: IllegalArgumentException?) {
        handleException(e)
    }
    override fun onRtmpIllegalStateException(e: IllegalStateException?) {
        handleException(e)
    }
    override fun onRecordIllegalArgumentException(e: IllegalArgumentException?) {
        handleException(e)
    }
    override fun onRecordIOException(e: IOException?) {
        handleException(e)
    }
    override fun onEncodeIllegalArgumentException(e: IllegalArgumentException?) {
        handleException(e)
    }
    override fun onRecordPause() {}
    override fun onRecordResume() {}
    override fun onRecordStarted(msg: String?) {}
    override fun onRecordFinished(msg: String?) {}
    override fun onNetworkWeak() {
        Toast.makeText(this, "网络质量不佳", Toast.LENGTH_LONG).show()
    }
    override fun onNetworkResume() {
        Toast.makeText(this, "网络正常", Toast.LENGTH_LONG).show()
    }
}

/* import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import zju.me.utils.AvcEncoder

class CameraActivity : AppCompatActivity() {

    private val width = 1280
    private val height = 720

    private lateinit var executor : Executor
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView : PreviewView
    private lateinit var videoEncoder : AvcEncoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // 图像编码
        videoEncoder = AvcEncoder(width, height)

        // 图像预览
        previewView = findViewById(R.id.previewView)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
        // 图像分析
        executor = Executors.newSingleThreadExecutor()
        var imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(width, height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
            var yBuffer = image.planes[0].buffer
            var uBuffer = image.planes[1].buffer
            var vBuffer = image.planes[2].buffer
            var ySize = yBuffer.remaining()
            var uSize = uBuffer.remaining()
            var vSize = vBuffer.remaining()
            var yuv = ByteArray(ySize + uSize + vSize)
            yBuffer.get(yuv, 0, ySize)
            uBuffer.get(yuv, ySize, vSize)
            vBuffer.get(yuv, ySize + vSize, uSize)
            videoEncoder.offerFrame(yuv)
            image.close()
        })
        videoEncoder.startEncoder()
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        var preview : Preview = Preview.Builder()
            .build()
        var cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }
} */