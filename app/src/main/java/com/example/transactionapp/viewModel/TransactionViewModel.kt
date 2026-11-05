package com.example.transactionapp.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transactionapp.data.AppDatabase
import com.example.transactionapp.data.TransactionEntity
import com.example.transactionapp.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        // Truncate table on start to remove any previously seeded data as requested
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { it.transactionType.equals("Income", ignoreCase = true) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.transactionType.equals("Expense", ignoreCase = true) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }
}
