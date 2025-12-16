package com.bluemix.clients_lead.features.auth.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.features.auth.presentation.isValidEmail
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.AppTheme
import ui.components.Icon
import ui.components.IconButton
import ui.components.IconButtonVariant
import ui.components.Text
import ui.components.progressindicators.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField


@Composable
fun MagicLinkScreen(
    email: String,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var sent by remember { mutableStateOf(false) }
    var cooldown by remember { mutableStateOf(0) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        animateIn = true
    }

    LaunchedEffect(sent) {
        if (sent && timerJob == null) {
            timerJob = scope.launch {
                cooldown = 30
                while (cooldown > 0) {
                    delay(1000)
                    cooldown--
                }
                timerJob = null
                sent = false
            }
        }
    }

    val isEmailValid = remember(email) { email.isValidEmail() }
    val canSend = isEmailValid && !loading && cooldown == 0

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
        // Animated Background Blobs - Purple/Blue theme for magic link
        AnimatedBackgroundBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    variant = IconButtonVariant.Ghost
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF94A3B8) // slate-400
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Main Card
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6).copy(alpha = 0.2f), // purple-500
                                        Color(0xFF3B82F6).copy(alpha = 0.2f)  // blue-500
                                    )
                                )
                            )
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = "Magic Link",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFA78BFA) // purple-400
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Main Card Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                            .padding(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                .padding(32.dp)
                        ) {
                            // Header
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Magic Link",
                                    style = AppTheme.typography.h1,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "We'll email you a secure sign-in link",
                                    style = AppTheme.typography.body1,
                                    color = Color(0xFF94A3B8), // slate-400
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(32.dp))

                            // Email Field
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Email Address",
                                    style = AppTheme.typography.label1,
                                    color = Color(0xFFCBD5E1), // slate-300
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                ModernTextField(
                                    value = email,
                                    onValueChange = onEmailChange,
                                    placeholder = "you@example.com",
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Email,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (canSend) {
                                                keyboard?.hide()
                                                onSend()
                                                sent = true
                                            }
                                        }
                                    ),
                                    isError = email.isNotBlank() && !isEmailValid
                                )

                                AnimatedVisibility(visible = email.isNotBlank() && !isEmailValid) {
                                    Text(
                                        text = "Enter a valid email address",
                                        style = AppTheme.typography.label2,
                                        color = Color(0xFFEF4444), // red-500
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Send Button
                            MagicLinkButton(
                                text = if (cooldown > 0) "Resend in ${cooldown}s" else "Send Magic Link",
                                onClick = {
                                    keyboard?.hide()
                                    onSend()
                                    sent = true
                                },
                                enabled = canSend,
                                loading = loading,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(16.dp))

                            // Info Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.1f)) // purple-500
                                    .padding(1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.05f))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "💡 The magic link will expire in 15 minutes",
                                        style = AppTheme.typography.body2,
                                        color = Color(0xFFCBD5E1), // slate-300
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Success Message
            AnimatedVisibility(
                visible = sent && cooldown > 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f)) // green-500
                        .padding(16.dp)
                ) {
                    Text(
                        text = "✓ Magic link sent! Check your email",
                        style = AppTheme.typography.body2,
                        color = Color(0xFF10B981), // green-500
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnimatedBackgroundBlobs() {
    val infiniteTransition = rememberInfiniteTransition(label = "blob")

    val blob1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )

    val blob2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
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
        Box(
            modifier = Modifier
                .offset(x = 40.dp, y = 80.dp)
                .size(288.dp * blob1Scale)
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6)) // purple
                .blur(100.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-80).dp)
                .size(384.dp * blob2Scale)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6)) // blue
                .blur(100.dp)
        )
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = AppTheme.typography.body1.copy(color = Color.White),
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF64748B), // slate-500
                style = AppTheme.typography.body1
            )
        },
        leadingIcon = leadingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        isError = isError,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF8B5CF6), // purple-500
            unfocusedBorderColor = Color(0xFF334155), // slate-700
            errorBorderColor = Color(0xFFEF4444), // red-500
            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            errorContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            cursorColor = Color(0xFF8B5CF6)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun MagicLinkButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6), // purple-600
                            Color(0xFF3B82F6)  // blue-600
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF334155), // slate-700
                            Color(0xFF334155)
                        )
                    )
                }
            )
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Sending...",
                    style = AppTheme.typography.button,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = text,
                style = AppTheme.typography.button,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}