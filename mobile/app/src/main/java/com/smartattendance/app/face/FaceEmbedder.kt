package com.smartattendance.app.face

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val MODEL_ASSET = "mobilefacenet.tflite"
private const val INPUT_SIZE = 112

/**
 * On-device face embedding (FR-06). Biometric data never leaves the
 * device: this class only ever produces a float vector held in memory /
 * encrypted prefs, and the raw face bitmap is discarded immediately after
 * inference.
 *
 * Degrades gracefully when `mobilefacenet.tflite` isn't bundled (see
 * assets/README.md) so the rest of the app — QR scan, liveness gate,
 * attendance marking — still builds and runs; [isAvailable] tells the UI
 * whether embedding-based matching is active.
 */
class FaceEmbedder(private val context: Context) {

    private val interpreter: Interpreter? by lazy { loadInterpreter() }
    private var outputSize: Int = 0

    val isAvailable: Boolean get() = interpreter != null

    private fun loadInterpreter(): Interpreter? {
        return runCatching {
            val assetFd = context.assets.openFd(MODEL_ASSET)
            val buffer = java.io.FileInputStream(assetFd.fileDescriptor).use { input ->
                input.channel.map(FileChannel.MapMode.READ_ONLY, assetFd.startOffset, assetFd.declaredLength)
            }
            val options = Interpreter.Options().apply { setNumThreads(4) }
            val interp = Interpreter(buffer, options)
            outputSize = interp.getOutputTensor(0).shape().last()
            interp
        }.getOrNull()
    }

    suspend fun embed(faceBitmap: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
        val interp = interpreter ?: return@withContext null
        val resized = if (faceBitmap.width != INPUT_SIZE || faceBitmap.height != INPUT_SIZE) {
            Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
        } else {
            faceBitmap
        }

        val input = bitmapToInputBuffer(resized)
        val output = Array(1) { FloatArray(outputSize) }
        interp.run(input, output)

        FaceMatcher.l2Normalize(output[0])
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            buffer.putFloat((r - 127.5f) / 127.5f)
            buffer.putFloat((g - 127.5f) / 127.5f)
            buffer.putFloat((b - 127.5f) / 127.5f)
        }
        buffer.rewind()
        return buffer
    }
}
