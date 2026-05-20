package com.busraknya.moodjournal.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles on-device sentiment analysis using a bundled TensorFlow Lite model.
 *
 * This class is provided as a Singleton by Hilt, ensuring the TFLite model is loaded
 * into memory only once for the entire app lifecycle. It is designed to be robust and
 * fail-safe; if model initialization fails, the app will continue to function by
 * returning a default "Neutral" sentiment.
 */
@Singleton
class SentimentAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // The 'lateinit' keyword promises that this variable will be initialized before it's used.
    // This avoids making it nullable, which cleans up the 'analyze' function.
    private lateinit var interpreter: Interpreter

    // 'isInitialized' acts as a flag to prevent re-initialization and to check status.
    @Volatile
    private var isInitialized = false

    companion object {
        const val SENTIMENT_POSITIVE = "Positive"
        const val SENTIMENT_NEGATIVE = "Negative"
        const val SENTIMENT_NEUTRAL = "Neutral"

        // --- DEVELOPER CUSTOMIZATION POINT ---
        // To use your own TFLite model:
        // 1. Place your `.tflite` file in the `assets` folder of the `app` module.
        // 2. Update the filename in this constant.
        private const val MODEL_FILE_NAME = "sentiment_model.tflite"

        // These thresholds define the sensitivity of the sentiment detection.
        // They should be calibrated based on your specific model's output range.
        private const val POSITIVE_THRESHOLD = 2.0f
        private const val NEGATIVE_THRESHOLD = -2.0f
    }

    init {
        initializeInterpreter()
    }

    /**
     * Loads the TFLite model from the `assets` folder and prepares the interpreter.
     * This operation includes an optional GPU delegate for improved performance on supported devices.
     */
    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelFile(MODEL_FILE_NAME)
            val options = Interpreter.Options()
            // Optional: Add GPU delegate for hardware acceleration.
            // This can significantly speed up inference on devices with a compatible GPU.
            // options.addDelegate(GpuDelegate())
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
        } catch (e: Exception) {
            // Log the error for debugging purposes but don't crash the app.
            // The `isInitialized` flag will remain false, and `analyze()` will handle it.
            e.printStackTrace()
        }
    }

    private fun loadModelFile(modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        return FileInputStream(assetFileDescriptor.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    /**
     * Analyzes the given text and returns a sentiment string.
     * Returns `SENTIMENT_NEUTRAL` as a safe fallback if the interpreter is not initialized or an error occurs.
     */
    fun analyze(text: String): String {
        // Guard clause for efficiency and to handle initialization failure.
        if (!isInitialized || text.isBlank()) {
            return SENTIMENT_NEUTRAL
        }

        return try {
            // The model expects a specific input shape (e.g., Array<String> with one element).
            val inputArray = arrayOf(text)
            // The model produces a specific output shape (e.g., a 1x1 float array for the score).
            val outputArray = Array(1) { FloatArray(1) }

            // Run inference.
            interpreter.run(inputArray, outputArray)

            val score = outputArray[0][0]

            // Classify the score based on the defined thresholds.
            when {
                score > POSITIVE_THRESHOLD -> SENTIMENT_POSITIVE
                score < NEGATIVE_THRESHOLD -> SENTIMENT_NEGATIVE
                else -> SENTIMENT_NEUTRAL
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If any error occurs during inference, fall back to the safe default.
            SENTIMENT_NEUTRAL
        }
    }
}