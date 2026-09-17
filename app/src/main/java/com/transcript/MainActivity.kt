package com.transcript

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.transcript.service.TranscriptAccessibilityService
import com.transcript.service.TranscriptForegroundService

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAccessibilityPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TranscriptTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        isServiceRunning = isServiceRunning,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestAccessibilityPermission = { requestAccessibilityPermission() },
                        onStartService = { startOverlayService() },
                        onStopService = { stopOverlayService() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = checkOverlayPermission()
        hasAccessibilityPermission = TranscriptAccessibilityService.isServiceEnabled()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, TranscriptForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
        isServiceRunning = true
    }

    private fun stopOverlayService() {
        val intent = Intent(this, TranscriptForegroundService::class.java).apply {
            action = TranscriptForegroundService.ACTION_STOP_SERVICE
        }
        startService(intent)
        isServiceRunning = false
    }
}

@Composable
fun TranscriptTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    isServiceRunning: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Transcript",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        )
        Text(
            text = "Real-Time, Cross-App Battery-Efficient Audio & Screen Translator",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Gray,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        // Overlay Permission
        if (!hasOverlayPermission) {
            Text(
                text = "Floating overlay permission is required to render subtitles over third-party apps.",
                style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = onRequestOverlayPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Grant Overlay Permission")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Accessibility Permission (Zero-OCR Screen Text Translation)
        if (!hasAccessibilityPermission) {
            Text(
                text = "Accessibility access enables instant screen translation without draining battery via OCR.",
                style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedButton(
                onClick = onRequestAccessibilityPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Enable Accessibility Service")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Service Control
        if (hasOverlayPermission) {
            if (!isServiceRunning) {
                Button(
                    onClick = onStartService,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Start Floating Translator")
                }
            } else {
                Button(
                    onClick = onStopService,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Stop Floating Translator", color = Color.White)
                }
            }
        }
    }
}
