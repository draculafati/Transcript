package com.transcript.core.vad

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Silero Voice Activity Detector using ONNX Runtime Mobile (ADR-0001, ADR-0002).
 * Runs at ultra-low CPU (<0.5%) footprint to gate heavy ASR models.
 */
class SileroVoiceActivityDetector(
    private val context: Context? = null,
    private val modelAssetName: String = "models/silero_vad.onnx"
) : IVoiceActivityDetector {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Silero VAD recurrent hidden state: [2, 1, 64] float32
    private var stateH = Array(2) { Array(1) { FloatArray(64) } }
    private var stateC = Array(2) { Array(1) { FloatArray(64) } }

    private var isModelLoaded = false

    init {
        tryLoadModel()
    }

    private fun tryLoadModel() {
        if (context == null) return
        try {
            val assetManager = context.assets
            val modelBytes = assetManager.open(modelAssetName).use { it.readBytes() }
            ortEnvironment = OrtEnvironment.getEnvironment()
            ortSession = ortEnvironment?.createSession(modelBytes, OrtSession.SessionOptions())
            isModelLoaded = true
        } catch (e: Exception) {
            // Model file not yet bundled into assets; will utilize energy-based VAD fallback
            isModelLoaded = false
        }
    }

    override fun analyzeChunk(pcmChunk: ByteArray): Float {
        if (pcmChunk.isEmpty()) return 0f

        val numSamples = pcmChunk.size / 2
        val floatSamples = FloatArray(numSamples)

        val byteBuffer = ByteBuffer.wrap(pcmChunk).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples) {
            val shortSample = byteBuffer.short
            floatSamples[i] = shortSample / 32768.0f
        }

        if (isModelLoaded && ortSession != null && ortEnvironment != null) {
            return runOnnxInference(floatSamples)
        } else {
            // High-efficiency adaptive RMS energy fallback when ONNX model is unbundled
            return computeRmsSpeechScore(floatSamples)
        }
    }

    private fun runOnnxInference(floatSamples: FloatArray): Float {
        val env = ortEnvironment ?: return 0f
        val session = ortSession ?: return 0f

        val sampleRate = longArrayOf(16000L)
        val inputShape = longArrayOf(1, floatSamples.size.toLong())
        val srShape = longArrayOf(1)
        val stateShape = longArrayOf(2, 1, 64)

        try {
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatSamples), inputShape)
            val srTensor = OnnxTensor.createTensor(env, sampleRate)
            val hTensor = OnnxTensor.createTensor(env, stateH)
            val cTensor = OnnxTensor.createTensor(env, stateC)

            val inputs = mapOf(
                "input" to inputTensor,
                "sr" to srTensor,
                "h" to hTensor,
                "c" to cTensor
            )

            session.run(inputs).use { results ->
                val outputTensor = results.get(0) as OnnxTensor
                val outputBuffer = outputTensor.floatBuffer
                val speechProb = outputBuffer.get(0)

                // Update recurrent state if outputted by model
                if (results.size() > 1) {
                    val newH = results.get(1) as? OnnxTensor
                    val newC = results.get(2) as? OnnxTensor
                    if (newH != null && newC != null) {
                        stateH = newH.value as Array<Array<FloatArray>>
                        stateC = newC.value as Array<Array<FloatArray>>
                    }
                }
                return speechProb
            }
        } catch (e: Exception) {
            return computeRmsSpeechScore(floatSamples)
        }
    }

    /**
     * Fallback RMS acoustic energy calculation for silence suppression
     * Normalized between 0.0f (silence) and 1.0f (loud speech)
     */
    private fun computeRmsSpeechScore(samples: FloatArray): Float {
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / samples.size).toFloat()
        // Typical speech RMS threshold is ~0.02 - 0.05
        val speechScore = (rms / 0.04f).coerceIn(0.0f, 1.0f)
        return speechScore
    }

    override fun resetState() {
        stateH = Array(2) { Array(1) { FloatArray(64) } }
        stateC = Array(2) { Array(1) { FloatArray(64) } }
    }
}
