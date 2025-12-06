package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.AuthRepositoryImpl
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
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

val authModule = module {
    // Repository - already provided in appModule, so remove this
    // single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }

    // Use Cases
    factory { SignUpWithEmail(get()) }
    factory { SignInWithEmail(get()) }
    factory { SendMagicLink(get()) }
    factory { HandleAuthRedirect(get()) }
    factory { IsLoggedIn(get()) }
    factory { ObserveAuthState(get()) }
    factory { SignOut(get()) }
    factory { GetCurrentUserId(get()) }

    // ViewModel - removed sessionManager and tokenStorage parameters
    viewModel {
        AuthViewModel(
            signUpWithEmail = get(),
            signInWithEmail = get(),
            sendMagicLink = get(),
            handleAuthRedirect = get(),
            isLoggedIn = get(),
            signOut = get(),
            authRedirectUrl = "clientslead://auth"
        )
    }
}