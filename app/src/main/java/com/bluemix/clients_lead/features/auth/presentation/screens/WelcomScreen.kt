package com.bluemix.clients_lead.features.auth.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onMagicLink: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {
        Spacer(Modifier.height(48.dp))

        // Branding / headline
        Text(
            text = "FieldsOps Tracker",
            style = AppTheme.typography.h1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in or create an account to continue.",
            style = AppTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.8f)
        )
        Spacer(Modifier.height(32.dp))

        // Primary actions

        Column(Modifier.padding(20.dp)) {
            Button(
                variant = ButtonVariant.Primary,
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign in with email") }

            Spacer(Modifier.height(12.dp))

            Button(
                variant = ButtonVariant.PrimaryOutlined,
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create a new account") }

            Spacer(Modifier.height(12.dp))

            Button(
                variant = ButtonVariant.PrimaryGhost,
                onClick = onMagicLink,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Send me a magic link")
            }
        }


        Spacer(Modifier.height(24.dp))
    }
}