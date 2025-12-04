package com.bluemix.clients_lead.features.auth.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.features.auth.presentation.SmallTopBar
import com.bluemix.clients_lead.features.auth.presentation.isValidEmail
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Icon
import ui.components.Text
import ui.components.textfield.OutlinedTextField


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

    var sent by remember { mutableStateOf(false) }
    var cooldown by remember { mutableStateOf(0) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val isEmailValid = remember(email) { email.isValidEmail() }
    val canSend = isEmailValid && !loading && cooldown == 0

    LaunchedEffect(sent) {
        if (sent && timerJob == null) {
            timerJob = scope.launch {
                cooldown = 30
                while (cooldown > 0) {
                    delay(1000)
                    cooldown--
                }
                timerJob = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        SmallTopBar(title = "Magic link", onBack = onBack)
        Spacer(Modifier.height(12.dp))


        Column(Modifier.padding(20.dp)) {

            Text(
                "We’ll email you a secure sign-in link. Tap it on this device to jump back into the app.",
                style = AppTheme.typography.body2,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                isError = email.isNotBlank() && !isEmailValid,
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
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your email") }
            )
            if (email.isNotBlank() && !isEmailValid) {
                Text(
                    "Enter a valid email address",
                    style = AppTheme.typography.label2,
                    color = AppTheme.colors.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    keyboard?.hide()
                    onSend()
                    sent = true
                },
                enabled = canSend,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (cooldown > 0) "Resend in ${cooldown}s" else "Send magic link")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                variant = ButtonVariant.PrimaryElevated,
                onClick = {

                    // Handy shortcut to open mail app
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
                    runCatching { ctx.startActivity(intent) }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Open email app")
            }
        }

    }
}
