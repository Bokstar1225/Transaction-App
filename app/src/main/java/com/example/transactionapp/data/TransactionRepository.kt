package com.example.transactionapp.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao){
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    suspend fun getTransactionById(id: Int): TransactionEntity? = transactionDao.getTransactionById(id)

    suspend fun getTotalIncome(): Double? = transactionDao.getTotalIncome()
    suspend fun getTotalExpense(): Double? = transactionDao.getTotalExpense()

    suspend fun insertTransaction(transaction: TransactionEntity) = transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()
}
