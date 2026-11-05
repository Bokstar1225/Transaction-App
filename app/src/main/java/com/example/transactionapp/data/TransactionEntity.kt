package com.example.transactionapp.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val transactionType: String,
    val category: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
)
