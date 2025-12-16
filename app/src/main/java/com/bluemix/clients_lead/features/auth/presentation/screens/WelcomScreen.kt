package com.bluemix.clients_lead.features.auth.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ui.AppTheme
import ui.components.Icon
import ui.components.Text

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onMagicLink: () -> Unit,
    onDemo: () -> Unit = {} // New parameter for demo
) {
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        animateIn = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // slate-900
                        Color(0xFF1E293B), // slate-800
                        Color(0xFF0F172A)  // slate-900
                    )
                )
            )
    ) {
        // Animated Background Blobs
        AnimatedBackgroundBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and Branding
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(600)) +
                        scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Logo Icon with Gradient
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6), // blue-500
                                        Color(0xFF8B5CF6)  // purple-600
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = "GeoTrack Logo",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // App Name
                    Text(
                        text = "GeoTrack",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Tagline
                    Text(
                        text = "Track your field operations, manage clients, and monitor team activity",
                        style = AppTheme.typography.body1,
                        color = Color(0xFF94A3B8), // slate-400
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // CTA Buttons
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Primary Button - Sign In
                    GradientButton(
                        text = "Sign In",
                        onClick = onSignIn,
                        gradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2563EB), // blue-600
                                Color(0xFF3B82F6)  // blue-500
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Secondary Button - Create Account
                    OutlinedButton(
                        text = "Create Account",
                        onClick = onSignUp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Bottom Row: Magic Link & Demo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Magic Link Button
                        GhostButton(
                            text = "Magic Link →",
                            onClick = onMagicLink,
                            modifier = Modifier.weight(1f)
                        )

                        // Demo Button
                        DemoButton(
                            text = "Try Demo",
                            onClick = onDemo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Footer
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
            ) {
                Text(
                    text = "Secure field operations management",
                    style = AppTheme.typography.body3,
                    color = Color(0xFF64748B), // slate-500
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AnimatedBackgroundBlobs() {
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")

    val blob1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )

    val blob2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.3f)
    ) {
        // Blue Blob
        Box(
            modifier = Modifier
                .offset(x = (40 + blob1Offset).dp, y = 80.dp)
                .size(288.dp)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6))
                .blur(100.dp)
        )

        // Purple Blob
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40 + blob2Offset).dp, y = (-80).dp)
                .size(384.dp)
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6))
                .blur(100.dp)
        )
    }
}

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.button,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

@Composable
private fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f)) // slate-800
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                .then(
                    Modifier
                        .matchParentSize()
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
                    .then(
                        Modifier.padding(1.dp)
                    )
            )
        }

        Text(
            text = text,
            style = AppTheme.typography.button,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTheme.typography.body1,
            color = Color(0xFF94A3B8), // slate-400
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DemoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.2f), // purple-600
                        Color(0xFF3B82F6).copy(alpha = 0.2f)  // blue-600
                    )
                )
            )
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Border effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.3f))
        )

        Text(
            text = text,
            style = AppTheme.typography.body2,
            color = Color(0xFFC4B5FD), // purple-300
            fontWeight = FontWeight.Medium
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}