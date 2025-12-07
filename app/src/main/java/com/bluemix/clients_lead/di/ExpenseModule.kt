package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.features.expense.vm.TripExpenseViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val expenseModule = module {
    // ViewModel
    viewModel { TripExpenseViewModel() }

    // TODO: Add repository and use cases when implementing backend
    // single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }
    // factory { SubmitTripExpenseUseCase(get()) }
    // factory { GetTripExpensesUseCase(get()) }
}