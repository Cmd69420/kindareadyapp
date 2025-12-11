package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.ExpenseRepositoryImpl
import com.bluemix.clients_lead.domain.repository.ExpenseRepository
import com.bluemix.clients_lead.domain.usecases.*
import com.bluemix.clients_lead.features.expense.vm.TripExpenseViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val expenseModule = module {
    // Repository
    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }


    // Use Cases
    factory { SubmitTripExpenseUseCase(get()) }
    factory { GetTripExpensesUseCase(get()) }
    factory { GetExpenseByIdUseCase(get()) }
    factory { UpdateTripExpenseUseCase(get()) }
    factory { DeleteTripExpenseUseCase(get()) }
    factory { UploadReceiptUseCase(get()) }
    factory { GetTotalExpenseUseCase(get()) }

    // ViewModel
    viewModel {
        TripExpenseViewModel(
            submitExpense = get(),
            uploadReceipt = get(),
            sessionManager = get()
        )
    }
}