package com.bluemix.clients_lead.features.auth.presentation.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.features.auth.presentation.PasswordHintRow
import com.bluemix.clients_lead.features.auth.presentation.SmallTopBar
import com.bluemix.clients_lead.features.auth.presentation.isValidEmail
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Icon
import ui.components.IconButton
import ui.components.IconButtonVariant
import ui.components.Text
import ui.components.progressindicators.CircularProgressIndicator
import ui.components.textfield.OutlinedTextField

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
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var showPassword by remember { mutableStateOf(false) }

    val isEmailValid = remember(email) { email.isValidEmail() }
    val canSubmit = isEmailValid && password.length >= 6 && !loading

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
                .background(AppTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Spacer(Modifier.height(24.dp))
            SmallTopBar(title = title, onBack = onBack)
            Spacer(Modifier.height(12.dp))


            Column(Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your email") },
                    isError = email.isNotBlank() && !isEmailValid
                )
                if (email.isNotBlank() && !isEmailValid) {
                    Text(
                        "Enter a valid email address",
                        style = AppTheme.typography.label2,
                        color = AppTheme.colors.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            variant = IconButtonVariant.PrimaryGhost
                        ) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSubmit) {
                                keyboard?.hide()
                                onSubmit()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your password") }
                )

                PasswordHintRow(password)

                Spacer(Modifier.height(16.dp))

                Button(
                    variant = ButtonVariant.Primary,
                    onClick = {
                        keyboard?.hide()
                        onSubmit()
                    },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(primaryCta)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(secondaryPrompt)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = secondaryCta,
                        color = AppTheme.colors.tertiary,
                        modifier = Modifier.clickable { onSecondary() }
                    )
                }
            }

        }

        // Loading veil
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "loadingVeil"
        ) { isLoading ->
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .alpha(0.85f)
                        .clickable(enabled = false) {}
                ) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}