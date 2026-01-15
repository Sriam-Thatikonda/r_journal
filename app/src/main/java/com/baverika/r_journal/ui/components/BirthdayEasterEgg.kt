package com.baverika.r_journal.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Configuration
private const val CONFETTI_COUNT = 50
private const val ANIMATION_DURATION_MS = 8000L

@Composable
fun BirthdayEasterEggOverlay(
    age: Int,
    onFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val alphaAnim = remember { Animatable(0f) }
    val view = LocalView.current

    // Confetti State
    val particles = remember { List(CONFETTI_COUNT) { ConfettiParticle() } }
    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "time"
    )

    // Lifecycle
    LaunchedEffect(Unit) {
        // Haptic feedback start
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        // Fade in text
        alphaAnim.animateTo(1f, animationSpec = tween(1000))
        
        // Wait for remainder of duration
        delay(ANIMATION_DURATION_MS - 1000) // Total 3 seconds
        
        // Fade out
        alphaAnim.animateTo(0f, animationSpec = tween(500))
        
        isVisible = false
        onFinished()
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // Dim background slightly
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Confetti Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                particles.forEach { particle ->
                    // Simple logic to make them fall
                    // y = initialY + speed * time + constant movement
                    // We use `withFrameNanos` or just reliable drawing loop.
                    // Since we want strict 3 seconds, we can just use the animation frame time or similar.
                    // But infiniteTransition is cyclic. 
                    // Let's use a simpler approach: Calculate position based on system time offset or just pseudo-random fall.
                    
                    // Actually, for a 3 sec one-shot, we can use a basic update loop or just pure random drawing? 
                    // No, they need to fall.
                    
                    // Let's use the `time` from infinite transition to drive a simple wrapping fall
                    // y = (particle.speed * time * height) % height
                    // But we want them to fall continuously.
                    
                    // We can simulate y based on `System.currentTimeMillis()`?
                    // Better: standard animation loop.
                    
                    // Let's just draw them at positions relative to a "progress" derived differently.
                    // But for simplicity/robustness:
                    
                    val progress = (System.currentTimeMillis() % 3000) / 3000f // 3 sec loop roughly
                    // No, that jumps.
                    
                    // Just draw static? No, "Slow vertical fall".
                    
                    // Let's use specific offsets.
                    val yOffset = (particle.speed * (System.currentTimeMillis() % 10000) / 10f) % (canvasHeight + 100) - 50
                    
                    drawCircle(
                        color = particle.color,
                        radius = particle.size,
                        center = Offset(
                            x = particle.x * canvasWidth,
                            y = yOffset
                        ),
                        alpha = 0.6f
                    )
                }
            }

            // Message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer { alpha = alphaAnim.value }
            ) {
                Text(
                    text = "🎂 Today is your day.",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Thank you for showing up for yourself.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You turned $age years old.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFFFD700), // Gold
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float = Random.nextFloat(), // 0..1
    val speed: Float = Random.nextFloat() * 2f + 1f,
    val size: Float = Random.nextFloat() * 6f + 4f,
    val color: Color = listOf(
        Color(0xFFFFC107), // Amber
        Color(0xFF81C784), // Green
        Color(0xFF64B5F6), // Blue
        Color(0xFFE57373), // Red
        Color(0xFFBA68C8), // Purple
        Color.White
    ).random()
)
