package com.bluemix.clients_lead.features.auth.presentation.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bluemix.clients_lead.features.auth.vm.AuthEffect
import com.bluemix.clients_lead.features.auth.vm.AuthViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import ui.AppTheme
import ui.components.snackbar.SnackbarHost
import ui.components.snackbar.SnackbarHostState

private enum class AuthPage { Welcome, SignIn, SignUp, Magic }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = getViewModel()
) {
    val ui = viewModel.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(AuthPage.Welcome) }

    // navigate to Home when VM says we're signed in
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { if (it is AuthEffect.SignedIn) onSignedIn() }
    }

    // back behaviour: subpages -> welcome
    BackHandler(enabled = page != AuthPage.Welcome) {
        page = AuthPage.Welcome
    }

    // optional one-shot messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.value.error) {
        ui.value.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(ui.value.info) {
        ui.value.info?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },


        ) { innerPadding ->
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
        ) { screen ->
            when (screen) {
                AuthPage.Welcome -> WelcomeScreen(
                    onSignIn = { page = AuthPage.SignIn },
                    onSignUp = { page = AuthPage.SignUp },
                    onMagicLink = { page = AuthPage.Magic }
                )

                AuthPage.SignIn -> EmailPasswordScreen(
                    title = "Welcome back",
                    primaryCta = "Sign in",
                    secondaryPrompt = "New here?",
                    secondaryCta = "Create account",
                    email = ui.value.email,
                    password = ui.value.password,
                    loading = ui.value.loading,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = { viewModel.doSignIn() },
                    onSecondary = { page = AuthPage.SignUp },
                    onBack = { page = AuthPage.Welcome }
                )

                AuthPage.SignUp -> EmailPasswordScreen(
                    title = "Create your account",
                    primaryCta = "Sign up",
                    secondaryPrompt = "Already have an account?",
                    secondaryCta = "Sign in",
                    email = ui.value.email,
                    password = ui.value.password,
                    loading = ui.value.loading,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = { viewModel.doSignUp() },
                    onSecondary = { page = AuthPage.SignIn },
                    onBack = { page = AuthPage.Welcome }
                )

                AuthPage.Magic -> MagicLinkScreen(
                    email = ui.value.email,
                    loading = ui.value.loading,
                    onEmailChange = viewModel::onEmailChange,
                    onSend = { viewModel.sendMagicLink() },
                    onBack = { page = AuthPage.Welcome }
                )
            }
        }
    }
}

