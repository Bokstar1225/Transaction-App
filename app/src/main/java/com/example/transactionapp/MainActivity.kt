package com.example.transactionapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transactionapp.data.TransactionEntity
import com.example.transactionapp.ui.theme.TransactionAppTheme
import com.example.transactionapp.viewModel.TransactionViewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Facebook
import compose.icons.fontawesomeicons.brands.Instagram
import compose.icons.fontawesomeicons.brands.Tiktok
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransactionAppTheme {
                PocketLedgerApp()
            }
        }
    }
}

fun openInAppBrowser(context: android.content.Context, url: String) {
    try {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, url.toUri())
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun PocketLedgerApp(
    viewModel: TransactionViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf("home") } // "home", "add_transaction", "history", "settings"
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val transactions by viewModel.transactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()

    // Polished Delete Confirmation Dialog
    if (transactionToDelete != null) {
        DeleteConfirmationDialog(
            transaction = transactionToDelete!!,
            onConfirm = {
                transactionToDelete?.let { viewModel.deleteTransaction(it) }
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != "add_transaction") {
                PocketLedgerBottomNav(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it }
                )
            }
        },
        containerColor = Color(0xFFF7F8F6)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    transactions = transactions,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    currentBalance = currentBalance,
                    onAddTransactionClick = { currentScreen = "add_transaction" },
                    onTransactionLongClick = { transactionToDelete = it }
                )
                "add_transaction" -> AddTransactionScreen(
                    onBackClick = { currentScreen = "home" },
                    onSaveTransaction = { description, type, category, amount ->
                        viewModel.insertTransaction(
                            TransactionEntity(
                                description = description,
                                transactionType = type,
                                category = category,
                                amount = amount
                            )
                        )
                        currentScreen = "home"
                    }
                )
                "history" -> HistoryScreen(
                    transactions = transactions,
                    onBackClick = { currentScreen = "home" },
                    onTransactionLongClick = { transactionToDelete = it },
                    viewModel = viewModel
                )
                "settings" -> SettingsScreen(
                    onBackClick = { currentScreen = "home" }
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    transaction: TransactionEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("ZA").build())
    val isIncome = transaction.transactionType.equals("Income", ignoreCase = true)
    val amountText = (if (isIncome) "+ " else "- ") + currencyFormat.format(transaction.amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete transaction",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Are you sure you want to delete this transaction? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.description,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = transaction.category,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = amountText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) Color(0xFF0D5C3E) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Delete transaction",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151)),
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    currentBalance: Double,
    onAddTransactionClick: () -> Unit,
    onTransactionLongClick: (TransactionEntity) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("ZA").build())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8F6))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PocketLedger",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "Your personal finances",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5F5EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = "Wallet",
                    tint = Color(0xFF0A3A2A)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3B2C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Current balance",
                    fontSize = 14.sp,
                    color = Color(0xFFA7F3D0)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currencyFormat.format(currentBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Summary Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF164E3E))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF065F46)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Income",
                                    fontSize = 12.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currencyFormat.format(totalIncome),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    // Expenses Summary Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF164E3E))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7F1D1D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Expenses",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Expenses",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currencyFormat.format(totalExpense),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Transactions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Text(
                text = "Latest first",
                fontSize = 13.sp,
                color = Color(0xFF0A3A2A),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transactions List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    currencyFormat = currencyFormat,
                    onLongClick = { onTransactionLongClick(transaction) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add Transaction Button
        Button(
            onClick = onAddTransactionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3B2C))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add transaction",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    currencyFormat: NumberFormat,
    onLongClick: () -> Unit
) {
    val isIncome = transaction.transactionType.equals("Income", ignoreCase = true)
    val amountText = (if (isIncome) "+ " else "- ") + currencyFormat.format(transaction.amount)
    val amountColor = if (isIncome) Color(0xFF0D5C3E) else Color(0xFFDC2626)

    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) Color(0xFFE5F5EC) else Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.category.lowercase()) {
                            "food" -> Icons.Default.Restaurant
                            "transport" -> Icons.Default.DirectionsCar
                            "shopping" -> Icons.Default.ShoppingBag
                            "bills" -> Icons.Default.Receipt
                            "salary" -> Icons.Default.Work
                            else -> Icons.Default.AttachMoney
                        },
                        contentDescription = transaction.category,
                        tint = if (isIncome) Color(0xFF0D5C3E) else Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.category} • $dateString",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            Text(
                text = amountText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun AddTransactionScreen(
    onBackClick: () -> Unit,
    onSaveTransaction: (description: String, type: String, category: String, amount: Double) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("Income") } // "Income" or "Expense"
    var category by remember { mutableStateOf("") } // Typed by user as requested

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8F6))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF111827)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add transaction",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = "Record income or an expense.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description Field
        Text(
            text = "Description",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("e.g. Groceries, Salary, Taxi", color = Color(0xFF9CA3AF)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF0F3B2C),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Field
        Text(
            text = "Amount (ZAR)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            placeholder = { Text("R 0.00", color = Color(0xFF9CA3AF)) },
            leadingIcon = { Text("R", fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.padding(start = 12.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF0F3B2C),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction Type Selector
        Text(
            text = "Transaction type",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income Button
            OutlinedButton(
                onClick = { transactionType = "Income" },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (transactionType == "Income") Color.White else Color.Transparent,
                    contentColor = Color(0xFF111827)
                ),
                border = BorderStroke(
                    width = if (transactionType == "Income") 2.dp else 1.dp,
                    color = if (transactionType == "Income") Color(0xFF0F3B2C) else Color(0xFFE5E7EB)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Income",
                    tint = Color(0xFF0D5C3E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Income", fontWeight = FontWeight.SemiBold)
            }

            // Expense Button
            OutlinedButton(
                onClick = { transactionType = "Expense" },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (transactionType == "Expense") Color.White else Color.Transparent,
                    contentColor = Color(0xFF111827)
                ),
                border = BorderStroke(
                    width = if (transactionType == "Expense") 2.dp else 1.dp,
                    color = if (transactionType == "Expense") Color(0xFF0F3B2C) else Color(0xFFE5E7EB)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Expense",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Expense", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Field (Typed out by user as requested)
        Text(
            text = "Category",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            placeholder = { Text("e.g. Food, Transport, Salary", color = Color(0xFF9CA3AF)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF0F3B2C),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date and Time card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Date",
                    tint = Color(0xFF4B5563)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Date and time",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Automatically set when saved",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (description.isNotBlank() && amt > 0.0) {
                    onSaveTransaction(
                        description,
                        transactionType,
                        if (category.isBlank()) "Other" else category,
                        amt
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3B2C))
        ) {
            Text(
                text = "Save transaction",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    onBackClick: () -> Unit,
    onTransactionLongClick: (TransactionEntity) -> Unit,
    viewModel: TransactionViewModel = viewModel()
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("ZA").build())

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = "Delete All",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete all transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all transactions? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllTransactions()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Delete all",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAllDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151)),
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8F6))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction History",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            OutlinedButton(
                onClick = { showDeleteAllDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete All",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    currencyFormat = currencyFormat,
                    onLongClick = { onTransactionLongClick(transaction) }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val (versionName, versionCode) = remember(context) {
        try {
            val packageManager = context.packageManager
            val packageName = context.packageName

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            val versionNameVal = packageInfo.versionName ?: "Unknown"
            val versionCodeVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            Pair(versionNameVal, versionCodeVal.toString())
        } catch (e: PackageManager.NameNotFoundException) {
            Pair("Unknown", "Unknown")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8F6))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "PocketLedger App", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Version $versionName", fontSize = 14.sp, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Version code: $versionCode", fontSize = 14.sp, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Manage your personal finances with ease.", fontSize = 14.sp, color = Color(0xFF4B5563))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Our Socials",
                fontSize = 17.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        openInAppBrowser(context, "https://www.instagram.com")
                    },
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF6B7280))
                ) {
                    Icon(
                        imageVector = FontAwesomeIcons.Brands.Instagram,
                        contentDescription = "Instagram Icon",
                        tint = Color(0xFF6B7280)
                    )
                }

                IconButton(
                    onClick = {
                        openInAppBrowser(context, "https://www.tiktok.com")
                    },
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF6B7280))
                ) {
                    Icon(
                        imageVector = FontAwesomeIcons.Brands.Tiktok,
                        contentDescription = "TikTok Icon",
                        tint = Color(0xFF6B7280)
                    )
                }

                IconButton(
                    onClick = {
                        openInAppBrowser(context, "https://www.facebook.com")
                    },
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF6B7280))
                ) {
                    Icon(
                        imageVector = FontAwesomeIcons.Brands.Facebook,
                        contentDescription = "Facebook Icon",
                        tint = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
fun PocketLedgerBottomNav(
    currentScreen: String,
    onScreenSelected: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .height(106.dp)
            .navigationBarsPadding(),
        containerColor = Color.White,
        tonalElevation = 8.dp,
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = { onScreenSelected("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F3B2C),
                selectedTextColor = Color(0xFF0F3B2C),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
                indicatorColor = Color(0xFFE5F5EC)
            )
        )
        NavigationBarItem(
            selected = currentScreen == "history",
            onClick = { onScreenSelected("history") },
            icon = { Icon(Icons.Default.List, contentDescription = "History") },
            label = { Text("History") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F3B2C),
                selectedTextColor = Color(0xFF0F3B2C),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
                indicatorColor = Color(0xFFE5F5EC)
            )
        )
        NavigationBarItem(
            selected = currentScreen == "settings",
            onClick = { onScreenSelected("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0F3B2C),
                selectedTextColor = Color(0xFF0F3B2C),
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF),
                indicatorColor = Color(0xFFE5F5EC)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PocketLedgerPreview() {
    TransactionAppTheme {
        HomeScreen(
            transactions = emptyList(),
            totalIncome = 3000.00,
            totalExpense = 585.00,
            currentBalance = 2415.00,
            onAddTransactionClick = {},
            onTransactionLongClick = {}
        )
    }
}
