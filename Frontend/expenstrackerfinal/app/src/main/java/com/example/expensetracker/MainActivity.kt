package com.example.expensetracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.drawer.DrawerContent
import com.example.expensetracker.ui.navigation.BottomNavBar
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.navigation.Screen
import com.example.expensetracker.ui.sms.NotificationHelper
import com.example.expensetracker.ui.sms.SmsParser
import com.example.expensetracker.ui.sms.SmsReceiver
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    internal val SMS_PERMISSION_CODE = 100

    private var smsReceiver: SmsReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpenseTrackerTheme {
                val mainViewModel: MainViewModel = viewModel()
                ExpenseTrackerApp(mainViewModel)
            }
        }
    }


    fun registerSmsReceiverIfNeeded() {
        if (smsReceiver == null) {
            smsReceiver = SmsReceiver()
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            try {
                registerReceiver(smsReceiver, filter)
            } catch (e: Exception) {
                // ignore registration errors if any
                smsReceiver = null
            }
        }
    }


    fun unregisterSmsReceiverIfNeeded() {
        smsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
            }
            smsReceiver = null
        }
    }

    override fun onDestroy() {
        unregisterSmsReceiverIfNeeded()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var sessionChecked by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf("login") }

    val context = LocalContext.current
    val activity = (context as? Activity)
    val mainActivity = activity as? MainActivity

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[android.Manifest.permission.READ_SMS] == true
        val receiveGranted = permissions[android.Manifest.permission.RECEIVE_SMS] == true
        if (readGranted && receiveGranted) {
            mainActivity?.registerSmsReceiverIfNeeded()
        } else {
        }
    }

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        android.util.Log.d("MainActivity", "POST_NOTIFICATIONS granted? $granted")
    }

    LaunchedEffect(Unit) {
        viewModel.restoreSession { loggedIn ->
            startDestination = if (loggedIn) Screen.Dashboard.route else "login"
            sessionChecked = true
        }
    }

    LaunchedEffect(sessionChecked, viewModel.currentUser) {
        if (sessionChecked && viewModel.currentUser != null && mainActivity != null) {
            val readSmsGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val receiveSmsGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!readSmsGranted || !receiveSmsGranted) {
                smsPermissionLauncher.launch(
                    arrayOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS)
                )
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val notifGranted = ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!notifGranted) {
                        requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                mainActivity.registerSmsReceiverIfNeeded()
            }
        } else {
            mainActivity?.unregisterSmsReceiverIfNeeded()
        }
    }

    val handledIds = remember { mutableStateOf(mutableSetOf<String>()) }
    val uiScope = rememberCoroutineScope()

    DisposableEffect(key1 = Unit) {
        val filter = android.content.IntentFilter(NotificationHelper.ACTION_EXPENSE_ADDED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    val idempotency = intent.getStringExtra(NotificationHelper.EXTRA_IDEMPOTENCY) ?: ""
                    // dedupe
                    if (idempotency.isNotBlank() && handledIds.value.contains(idempotency)) {
                        android.util.Log.d("ExpenseBroadcast", "Skipping already-handled idempotency=$idempotency")
                        return
                    }

                    val amount = intent.getDoubleExtra(NotificationHelper.EXTRA_AMOUNT, 0.0)
                    val receiverName = intent.getStringExtra(NotificationHelper.EXTRA_RECEIVER) ?: ""
                    val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: ""
                    val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE) ?: ""
                    val direction = intent.getStringExtra(NotificationHelper.EXTRA_DIRECTION) ?: "debit"

                    android.util.Log.d("ExpenseBroadcast", "Received expense broadcast: id=$idempotency amt=$amount receiver=$receiverName category=$category date=$dateIso dir=$direction")

                    // mark handled early to avoid duplicates from multiple broadcasts
                    if (idempotency.isNotBlank()) {
                        handledIds.value.add(idempotency)
                    }

                    // call ViewModel to update in-memory state and also attempt backend sync
                    uiScope.launch {
                        try {
                            if (direction.equals("credit", ignoreCase = true)) {
                                viewModel.addManualCredit(
                                    title = if (receiverName.isBlank()) "Income" else receiverName,
                                    amount = kotlin.math.abs(amount),
                                    date = dateIso
                                )
                            } else {
                                viewModel.addManualExpense(
                                    title = if (receiverName.isBlank()) "Expense" else receiverName,
                                    amount = kotlin.math.abs(amount),
                                    date = dateIso
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ExpenseBroadcast", "Error handling expense broadcast", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExpenseBroadcast", "onReceive error", e)
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, /*permission*/ null, /*handler*/ null, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            android.util.Log.d("ExpenseBroadcast", "Registered internal expense receiver")
        } catch (e: Exception) {
            android.util.Log.e("ExpenseBroadcast", "Failed to register receiver", e)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
                android.util.Log.d("ExpenseBroadcast", "Unregistered internal expense receiver")
            } catch (ignored: Exception) {
                // ignore
            }
        }
    }

    // If session not checked yet, show loading
    if (!sessionChecked) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6366F1))
            }
        }
        return
    }

    // Build UI after session check
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route
    val hideAppChromeRoutes = listOf("login", "signup", "auth")
    val showScaffold = currentRoute !in hideAppChromeRoutes

    if (showScaffold) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    navController = navController,
                    viewModel = viewModel,
                    onDestinationClicked = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Hi, ${viewModel.currentUser?.name ?: "User"}",
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
                    )
                },
                bottomBar = { BottomNavBar(navController) }
            ) { innerPadding ->
                NavGraph(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    viewModel = viewModel,
                    startDestination = startDestination
                )
            }
        }
    } else {
        NavGraph(navController, Modifier, viewModel, startDestination)
    }
}

