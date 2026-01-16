// di/ExpenseModule.kt
package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.remote.NominatimApiService
import com.bluemix.clients_lead.data.repository.ExpenseRepositoryImpl
import com.bluemix.clients_lead.data.repository.LocationSearchRepository
import com.bluemix.clients_lead.data.repository.RouteCalculationRepository
import com.bluemix.clients_lead.domain.repository.ExpenseRepository
import com.bluemix.clients_lead.domain.usecases.*
import com.bluemix.clients_lead.features.expense.vm.TripExpenseViewModel
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.bluemix.clients_lead.data.repository.DraftExpenseRepository
import com.bluemix.clients_lead.features.expense.vm.MultiLegExpenseViewModel

val expenseModule = module {
    // Repository
    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }
    single { DraftExpenseRepository(androidContext()) }

    // ✅ Nominatim HTTP Client (separate from main API client)
    single<HttpClient>(qualifier = org.koin.core.qualifier.named("nominatim")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
        }
    }

    // ✅ Nominatim API Service
    single {
        NominatimApiService(
            httpClient = get(qualifier = org.koin.core.qualifier.named("nominatim"))
        )
    }

    // ✅ NEW: Route Calculation Repository
    single {
        RouteCalculationRepository(
            httpClient = get(qualifier = org.koin.core.qualifier.named("nominatim"))
        )
    }

    // ✅ UPDATED: Location Search Repository with route calculator
    single {
        LocationSearchRepository(
            context = androidContext(),
            nominatimApi = get(),
            routeCalculator = get() // ✅ Added dependency
        )
    }

    // Use Cases
    factory { SubmitTripExpenseUseCase(get()) }
    factory { GetTripExpensesUseCase(get()) }
    factory { GetExpenseByIdUseCase(get()) }
    factory { UpdateTripExpenseUseCase(get()) }
    factory { DeleteTripExpenseUseCase(get()) }
    factory { UploadReceiptUseCase(get()) }
    factory { GetTotalExpenseUseCase(get()) }

    // ViewModels
    viewModel {
        TripExpenseViewModel(
            submitExpense = get(),
            uploadReceipt = get(),
            sessionManager = get(),
            locationSearchRepo = get(),
            draftRepository = get()
        )
    }

    viewModel {
        MultiLegExpenseViewModel(
            submitExpense = get(),
            sessionManager = get(),
            locationSearchRepo = get(),
            draftRepository = get()
        )
    }
}