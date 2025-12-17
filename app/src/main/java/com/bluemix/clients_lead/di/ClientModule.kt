package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.ClientRepositoryImpl
import com.bluemix.clients_lead.domain.repository.IClientRepository
import com.bluemix.clients_lead.domain.usecases.GetAllClients
import com.bluemix.clients_lead.domain.usecases.GetClientById
import com.bluemix.clients_lead.domain.usecases.GetClientsWithLocation
import com.bluemix.clients_lead.domain.usecases.SearchClients
import com.bluemix.clients_lead.features.Clients.vm.ClientDetailViewModel
import com.bluemix.clients_lead.features.Clients.vm.ClientsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import org.koin.dsl.module

val clientModule = module {
    // Repository
    single<IClientRepository> { ClientRepositoryImpl(get()) }

    // Use Cases
    factory { GetAllClients(get()) }
    factory { GetClientById(get()) }
    factory { GetClientsWithLocation(get()) }
    factory { SearchClients(get()) }
    factory { GetCurrentUserId(get()) }


    // ViewModels
    viewModel {
        ClientsViewModel(
            getAllClients = get(),
            tokenStorage = get(),
            getCurrentUserId = get(),
            locationTrackingStateManager = get()
        )
    }

    viewModel { ClientDetailViewModel(get()) }
}
