package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.ProfileRepositoryImpl
import com.bluemix.clients_lead.domain.repository.IProfileRepository
import com.bluemix.clients_lead.domain.usecases.CreateUserProfile
import com.bluemix.clients_lead.domain.usecases.GetLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.GetUserProfile
import com.bluemix.clients_lead.domain.usecases.SaveLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.UpdateUserProfile
import com.bluemix.clients_lead.features.location.LocationTrackingManager
import com.bluemix.clients_lead.features.settings.vm.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    // Repository
    single<IProfileRepository> { ProfileRepositoryImpl(get(), androidContext()) }

    // Service Manager
    single { LocationTrackingManager(androidContext()) }

    // Use Cases
    factory { GetUserProfile(get()) }
    factory { UpdateUserProfile(get()) }
    factory { CreateUserProfile(get()) }
    factory { GetLocationTrackingPreference(get()) }
    factory { SaveLocationTrackingPreference(get()) }

    // ViewModel
    viewModel { ProfileViewModel(get(), get(), get(), get(), get(), get()) }
}
