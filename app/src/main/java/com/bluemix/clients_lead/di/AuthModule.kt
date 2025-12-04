package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.AuthRepositoryImpl
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.usecases.HandleAuthRedirect
import com.bluemix.clients_lead.domain.usecases.IsLoggedIn
import com.bluemix.clients_lead.domain.usecases.ObserveAuthState
import com.bluemix.clients_lead.domain.usecases.SendMagicLink
import com.bluemix.clients_lead.domain.usecases.SignInWithEmail
import com.bluemix.clients_lead.domain.usecases.SignOut
import com.bluemix.clients_lead.domain.usecases.SignUpWithEmail
import com.bluemix.clients_lead.features.auth.vm.AuthViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId


val authModule = module {
    // Repository
    single<AuthRepository> {
        AuthRepositoryImpl(
            httpClient = get(),
            sessionManager = get(),
            tokenStorage = get()
        )
    }

    // Use Cases
    factory { SignUpWithEmail(get()) }
    factory { SignInWithEmail(get()) }
    factory { SendMagicLink(get()) }
    factory { HandleAuthRedirect(get()) }
    factory { IsLoggedIn(get()) }
    factory { SignOut(get()) }
    factory { GetCurrentUserId(get()) }  // ✅ Fixed naming
    factory { ObserveAuthState(get()) }

    // ViewModel
    viewModel {
        AuthViewModel(
            signUpWithEmail = get(),
            signInWithEmail = get(),
            sendMagicLink = get(),
            handleAuthRedirect = get(),
            isLoggedIn = get(),
            observeAuthState = get(),
            signOut = get(),
            authRedirectUrl = "clientslead://auth"
        )
    }
}
