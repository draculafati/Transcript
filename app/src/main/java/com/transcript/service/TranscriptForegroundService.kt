package com.transcript.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.transcript.core.audio.InternalAudioRecorder
import com.transcript.core.inference.WhisperSpeechRecognizer
import com.transcript.core.lifecycle.DemandDrivenPowerController
import com.transcript.core.overlay.ComposeOverlayPresenter
import com.transcript.core.translation.MlKitOnDeviceTranslator
import com.transcript.core.vad.SileroVoiceActivityDetector
import com.transcript.core.vad.VadAudioGater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TranscriptForegroundService : Service() {

    private lateinit var overlayPresenter: ComposeOverlayPresenter
    private lateinit var powerController: DemandDrivenPowerController

    private lateinit var audioRecorder: InternalAudioRecorder
    private lateinit var vadDetector: SileroVoiceActivityDetector
    private lateinit var audioGater: VadAudioGater

    private lateinit var speechRecognizer: WhisperSpeechRecognizer
    private lateinit var translator: MlKitOnDeviceTranslator

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pipelineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        initializeAudioPipeline()
        initializeInferencePipeline()
        initializeOverlayAndPowerController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> stopSelf()
            ACTION_TOGGLE_OVERLAY -> {
                if (overlayPresenter.isVisible()) {
                    overlayPresenter.hideCapsule()
                } else {
                    overlayPresenter.showCapsule()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeAudioPipeline() {
        audioRecorder = InternalAudioRecorder()
        vadDetector = SileroVoiceActivityDetector(context = this)
        audioGater = VadAudioGater(audioProvider = audioRecorder, vad = vadDetector)
    }

    private fun initializeInferencePipeline() {
        // Quantized Whisper with NNAPI / GPU hardware delegate (ADR-0002 Rule 4)
        speechRecognizer = WhisperSpeechRecognizer(context = this)
        // On-Device ML Kit translation with zero recurring cost (ADR-0001)
        translator = MlKitOnDeviceTranslator()
    }

    private fun initializeOverlayAndPowerController() {
        powerController = DemandDrivenPowerController(
            context = this,
            onPipelineActivated = {
                startSpeechPipeline()
            },
            onPipelineSuspended = {
                stopSpeechPipeline()
            }
        )

        overlayPresenter = ComposeOverlayPresenter(
            context = this,
            onListeningToggled = { isListening ->
                powerController.setUserListeningRequested(isListening)
            },
            onOverlayVisibilityChanged = { isVisible ->
                powerController.setOverlayVisible(isVisible)
            },
            onTranslateScreenRequested = {
                triggerScreenTranslation()
            }
        )

        powerController.startMonitoring()
        overlayPresenter.showCapsule()
    }

    private fun triggerScreenTranslation() {
        serviceScope.launch {
            val accessibilityService = TranscriptAccessibilityService.instance
            if (accessibilityService == null) {
                overlayPresenter.updateSubtitle("Enable Transcript in Accessibility Settings to translate screen")
                return@launch
            }

            overlayPresenter.updateSubtitle("Reading screen text...")
            val extracted = accessibilityService.extractScreenText()
            if (extracted.isEmpty()) {
                overlayPresenter.updateSubtitle("No text elements found in active window")
                return@launch
            }

            val rawCombinedText = extracted.take(15).joinToString(" | ")
            val translated = translator.translate(rawCombinedText)
            overlayPresenter.updateSubtitle("Screen: $translated")
        }
    }

    private fun startSpeechPipeline() {
        if (pipelineJob?.isActive == true) return

        pipelineJob = serviceScope.launch {
            // Step 1: Collect gated speech stream (Silero VAD drops non-speech, 0% CPU for Whisper)
            audioGater.getSpeechStream().collect { speechChunk ->
                // Step 2: Transcribe via Quantized Whisper (TFLite over NNAPI/GPU)
                speechRecognizer.transcribe(speechChunk).collect { transcript ->
                    if (transcript.isNotBlank()) {
                        // Step 3: Neural translation via ML Kit
                        val translated = translator.translate(transcript)
                        // Step 4: Stream live result to Compose Overlay HUD
                        overlayPresenter.updateSubtitle(translated)
                    }
                }
            }
        }
    }

    private fun stopSpeechPipeline() {
        pipelineJob?.cancel()
        pipelineJob = null
        audioRecorder.stopCapture()
        vadDetector.resetState()
    }

    private fun startInForeground() {
        val channelId = "transcript_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transcript Translation Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running real-time battery-efficient audio and screen translation"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Transcript Active")
            .setContentText("Monitoring speech and screen text")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpeechPipeline()
        powerController.stopMonitoring()
        overlayPresenter.hideCapsule()
        speechRecognizer.release()
        translator.release()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.transcript.action.STOP_SERVICE"
        const val ACTION_TOGGLE_OVERLAY = "com.transcript.action.TOGGLE_OVERLAY"
    }
}
