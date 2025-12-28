package com.bluemix.clients_lead.di

import android.content.Context
import com.bluemix.clients_lead.core.common.utils.DefaultDispatchers
import com.bluemix.clients_lead.core.common.utils.DispatcherProvider
import com.bluemix.clients_lead.core.network.ApiClientProvider
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.core.network.TokenStorage
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.repository.IClientRepository
import com.bluemix.clients_lead.domain.repository.ILocationRepository
import com.bluemix.clients_lead.domain.repository.IProfileRepository
import com.bluemix.clients_lead.data.repository.AuthRepositoryImpl
import com.bluemix.clients_lead.data.repository.ClientRepositoryImpl
import com.bluemix.clients_lead.data.repository.LocationRepositoryImpl
import com.bluemix.clients_lead.data.repository.ProfileRepositoryImpl
import com.bluemix.clients_lead.domain.usecases.InsertLocationLog
import com.bluemix.clients_lead.features.auth.vm.SessionViewModel
import com.bluemix.clients_lead.features.map.vm.MapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<DispatcherProvider> { DefaultDispatchers }

    // Token + Session
    single { TokenStorage(androidContext()) }
    single { SessionManager(get()) }

    // HttpClient
    single {
        ApiClientProvider.create(
            baseUrl = ApiEndpoints.BASE_URL,
            tokenStorage = get(),
            sessionManager = get()
        )
    }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), androidContext()) }
    single<IClientRepository> { ClientRepositoryImpl(get()) }
    single<ILocationRepository> { LocationRepositoryImpl(get()) }
    single<IProfileRepository> { ProfileRepositoryImpl(get(), androidContext()) }

    // Use cases
    factory { InsertLocationLog(get()) }

    // ViewModels
    viewModel {
        SessionViewModel(
            get(), get()
        )
    }

    // Now inject LocationTrackingStateManager as third dependency
    viewModel {
        MapViewModel(
            get(), // GetClientsWithLocation
            get(), // GetCurrentUserId
            get(),
        )
    }
}
