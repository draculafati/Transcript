package com.transcript.core.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimalist, battery-efficient floating capsule for live subtitle presentation.
 * Supports edge docking, draggable movement, demand-driven listening toggles,
 * and double-tap screen translation (Task 5.2).
 */
@Composable
fun OverlayCapsule(
    subtitleText: String,
    isListening: Boolean,
    isCollapsed: Boolean,
    onDragDelta: (Float, Float) -> Unit,
    onToggleListening: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onTranslateScreen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .clip(RoundedCornerShape(20.dp)),
        color = Color(0xDD121212), // Frosted dark obsidian
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        if (isCollapsed) {
            // Collapsed edge pill: double tap triggers on-screen text extraction (Task 5.2)
            Row(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onToggleCollapsed() },
                            onDoubleTap = { onTranslateScreen() }
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Status dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Color(0xFF00E676) else Color(0xFFFF5252))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "T",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        } else {
            // Expanded HUD mode: displays live translated speech & controls
            Column(
                modifier = Modifier
                    .widthIn(min = 230.dp, max = 340.dp)
                    .padding(10.dp)
            ) {
                // Top control bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulse / Active Status Indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isListening) Color(0xFF00E676) else Color(0xFFFF5252))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListening) "LISTENING" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isListening) Color(0xFF00E676) else Color(0xFFAAAAAA),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Screen Translate Action Trigger
                        IconButton(
                            onClick = onTranslateScreen,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                text = "🔤",
                                fontSize = 11.sp
                            )
                        }

                        // Toggle Audio Listening State
                        IconButton(
                            onClick = onToggleListening,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                text = if (isListening) "⏸" else "▶",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        // Collapse Pill
                        IconButton(
                            onClick = onToggleCollapsed,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                text = "—",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        // Dismiss
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                text = "✕",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Subtitle content stream
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = if (subtitleText.isNotBlank()) subtitleText else "Ready — speech & screen text appears here",
                        color = if (subtitleText.isNotBlank()) Color.White else Color(0xFF888888),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
