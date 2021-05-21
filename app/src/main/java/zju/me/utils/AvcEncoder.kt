package zju.me.utils

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.ArrayBlockingQueue

class AvcEncoder constructor(width : Int, height : Int) {
    private val TAG = "AvcEncoder"
    private val BUFFER_SIZE : Int = 8
    private val formatType = "video/avc"
    private var mediaCodec : MediaCodec = MediaCodec.createByCodecName(formatType)
    private var mediaFormat: MediaFormat

    private lateinit var videoEncoderHandler : Handler
    private var videoEncoderHandlerThread = HandlerThread(TAG)
    private val inputDataQueue = ArrayBlockingQueue<ByteArray>(BUFFER_SIZE)
    private val outputDataQueue = ArrayBlockingQueue<ByteArray>(BUFFER_SIZE)

    init {
        videoEncoderHandlerThread.start()
        videoEncoderHandler = Handler(videoEncoderHandler.looper)
        mediaFormat = MediaFormat.createVideoFormat(formatType, width, height)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width*height)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    public fun startEncoder() {
        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(p0: MediaCodec, p1: Int) {
                var inputBuffer = p0.getInputBuffer(p1)
                inputBuffer?.clear()
                var srcData = inputDataQueue.poll()
                if (srcData != null)
                    inputBuffer?.put(srcData)
                mediaCodec.queueInputBuffer(p1, 0, srcData!!.size, 0, 0)
            }

            override fun onOutputBufferAvailable(
                p0: MediaCodec,
                p1: Int,
                p2: MediaCodec.BufferInfo
            ) {
                var outputBuffer = p0.getOutputBuffer(p1)
                if (outputBuffer != null && p2.size > 0) {
                    var output : ByteArray = ByteArray(outputBuffer.remaining())
                    outputBuffer.get(output)
                    outputDataQueue.offer(output)
                }
                mediaCodec.releaseOutputBuffer(p1, true)
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {}
            override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {}
        }, videoEncoderHandler)
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()
    }

    public fun stopEncoder() {
        mediaCodec.stop()
        mediaCodec.setCallback(null)
    }

    public fun release() {
        inputDataQueue.clear()
        outputDataQueue.clear()
        mediaCodec.release()
    }

    public fun offerFrame(frameData : ByteArray) {
        inputDataQueue.offer(frameData)
    }

    public fun pollFrame() : ByteArray? {
        return inputDataQueue.poll()
    }
}