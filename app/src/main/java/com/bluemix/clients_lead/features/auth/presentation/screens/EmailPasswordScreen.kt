package com.bluemix.clients_lead.features.auth.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.features.auth.presentation.isValidEmail
import kotlinx.coroutines.delay
import ui.AppTheme
import ui.components.Icon
import ui.components.IconButton
import ui.components.IconButtonVariant
import ui.components.Text
import ui.components.progressindicators.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EmailPasswordScreen(
    title: String,
    primaryCta: String,
    secondaryPrompt: String,
    secondaryCta: String,
    onSecondary: () -> Unit,
    email: String,
    password: String,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var showPassword by remember { mutableStateOf(false) }
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    val isEmailValid = remember(email) { email.isValidEmail() }
    val canSubmit = isEmailValid && password.length >= 6 && !loading

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f)) // slate-800 with transparency
                        .padding(1.dp) // Border width
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                            .padding(32.dp)
                    ) {
                        // Header
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = title,
                                style = AppTheme.typography.h1,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (title.contains("back", ignoreCase = true))
                                    "Sign in to continue"
                                else
                                    "Sign up to get started",
                                style = AppTheme.typography.body1,
                                color = Color(0xFF94A3B8) // slate-400
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

                        Spacer(Modifier.height(20.dp))

                        // Password Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Password",
                                style = AppTheme.typography.label1,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            ModernTextField(
                                value = password,
                                onValueChange = onPasswordChange,
                                placeholder = "Enter your password",
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showPassword = !showPassword },
                                        variant = IconButtonVariant.Ghost
                                    ) {
                                        Icon(
                                            if (showPassword) Icons.Outlined.VisibilityOff
                                            else Icons.Outlined.Visibility,
                                            contentDescription = if (showPassword) "Hide" else "Show",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (canSubmit) {
                                            keyboard?.hide()
                                            onSubmit()
                                        }
                                    }
                                )
                            )

                            // Password Strength Indicator
                            AnimatedVisibility(visible = password.isNotEmpty()) {
                                PasswordStrengthIndicator(password)
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Submit Button
                        ModernButton(
                            text = primaryCta,
                            onClick = {
                                keyboard?.hide()
                                onSubmit()
                            },
                            enabled = canSubmit,
                            loading = loading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        // Alternative Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = secondaryPrompt,
                                style = AppTheme.typography.body2,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = secondaryCta,
                                style = AppTheme.typography.body2,
                                color = Color(0xFF3B82F6), // blue-500
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onSecondary() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Security Badge
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)), // green-500
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Secure end-to-end encryption",
                        style = AppTheme.typography.body3,
                        color = Color(0xFF64748B) // slate-500
                    )
                }
            }
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
                .background(Color(0xFF3B82F6))
                .blur(100.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-80).dp)
                .size(384.dp * blob2Scale)
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6))
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
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
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
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        isError = isError,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF3B82F6), // blue-500
            unfocusedBorderColor = Color(0xFF334155), // slate-700
            errorBorderColor = Color(0xFFEF4444), // red-500
            focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            errorContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
            cursorColor = Color(0xFF3B82F6)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = remember(password) {
        when {
            password.length < 6 -> 0
            password.length < 10 -> 1
            password.any { it.isDigit() } && password.any { it.isLetter() } -> 2
            else -> 1
        }
    }

    val color = when (strength) {
        0 -> Color(0xFFEF4444) // red-500
        1 -> Color(0xFFFBBF24) // yellow-500
        else -> Color(0xFF10B981) // green-500
    }

    val label = when (strength) {
        0 -> "Weak"
        1 -> "Medium"
        else -> "Strong"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = AppTheme.typography.label3,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun ModernButton(
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
                            Color(0xFF2563EB), // blue-600
                            Color(0xFF3B82F6)  // blue-500
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
                    text = "Processing...",
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