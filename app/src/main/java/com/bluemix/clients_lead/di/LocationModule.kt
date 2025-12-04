package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.LocationRepositoryImpl
import com.bluemix.clients_lead.domain.repository.ILocationRepository
import com.bluemix.clients_lead.domain.usecases.DeleteOldLocationLogs
import com.bluemix.clients_lead.domain.usecases.GetLocationLogs
import com.bluemix.clients_lead.domain.usecases.GetLocationLogsByDateRange
import com.bluemix.clients_lead.domain.usecases.InsertLocationLog
import com.bluemix.clients_lead.features.timesheet.vm.ActivityViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val locationModule = module {
    // Repository
    single<ILocationRepository> { LocationRepositoryImpl(get()) }

    // Use Cases
    factory { InsertLocationLog(get()) }
    factory { GetLocationLogs(get()) }
    factory { GetLocationLogsByDateRange(get()) }
    factory { DeleteOldLocationLogs(get()) }

    // ViewModel
    viewModel { ActivityViewModel(get(), get()) }  // ✅ Only here now
}

