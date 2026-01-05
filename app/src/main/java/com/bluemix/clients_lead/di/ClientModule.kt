package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.ClientRepositoryImpl
import com.bluemix.clients_lead.domain.repository.IClientRepository
import com.bluemix.clients_lead.domain.usecases.*
import com.bluemix.clients_lead.features.Clients.vm.ClientDetailViewModel
import com.bluemix.clients_lead.features.Clients.vm.ClientsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.bluemix.clients_lead.data.repository.OCRRepository
import com.bluemix.clients_lead.domain.repository.IQuickVisitRepository
import com.bluemix.clients_lead.data.repository.QuickVisitRepositoryImpl

val clientModule = module {

    // Repository
    single<IClientRepository> {
        ClientRepositoryImpl(
            httpClient = get()
        )
    }
    single<IQuickVisitRepository> {
        QuickVisitRepositoryImpl(httpClient = get())
    }

    // Use Cases
    factory {
        GetAllClients(repository = get())
    }

    factory {
        GetClientById(repository = get())
    }

    factory {
        GetClientsWithLocation(repository = get())
    }

    factory {
        SearchClients(repository = get())
    }

    factory { CreateQuickVisit(repository = get()) }

    // ✅ NEW: Remote search use case
    factory {
        SearchRemoteClients(repository = get())
    }

    factory { CreateClient(repository = get()) }
    factory { OCRRepository() }

    // ViewModels
    viewModel {
        ClientsViewModel(
            getAllClients = get(),
            searchRemoteClients = get(),
            tokenStorage = get(),
            getCurrentUserId = get(),
            locationTrackingStateManager = get(),
            context = get(),
            createClient = get(),
            sessionManager = get()// ✅ NEW: Inject application context,
        )
    }

    // ✅ ClientDetailViewModel does NOT take clientId in constructor
    // The clientId is passed via loadClient(clientId) method from UI
    viewModel {
        ClientDetailViewModel(
            getClientById = get()
        )
    }
}