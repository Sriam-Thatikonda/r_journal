// app/src/main/java/com/baverika/r_journal/MainActivity.kt

package com.baverika.r_journal

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.data.remote.RetrofitClient
import com.baverika.r_journal.data.remote.ServerPrefs
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.QuickNoteRepository

import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.repository.PasswordRepository

import com.baverika.r_journal.ui.screens.*
import com.baverika.r_journal.ui.theme.RJournalTheme
import com.baverika.r_journal.ui.viewmodel.EventViewModelFactory
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import com.baverika.r_journal.ui.viewmodel.HabitViewModelFactory
import com.baverika.r_journal.ui.viewmodel.JournalViewModelFactory
import com.baverika.r_journal.ui.viewmodel.QuickNoteViewModelFactory

import com.baverika.r_journal.ui.viewmodel.SearchViewModelFactory
import com.baverika.r_journal.ui.viewmodel.PasswordViewModelFactory

import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = JournalDatabase.getDatabase(this)
        val journalRepo = JournalRepository(db.journalDao())
        val quickNoteRepo = QuickNoteRepository(db.quickNoteDao())
        val eventRepo = EventRepository(db.eventDao())
        val passwordRepo = PasswordRepository(db.passwordDao())
        val settingsRepo = SettingsRepository(this)
        
        // Quote feature repositories
        val quoteRepo = com.baverika.r_journal.quotes.data.QuoteRepository(db.quoteDao())
        val widgetSettingsDataStore = com.baverika.r_journal.quotes.settings.WidgetSettingsDataStore.getInstance(this)
        
        // Task feature repositories
        val taskRepo = com.baverika.r_journal.repository.TaskRepository(db.taskDao())

        // Life Tracker repository
        val lifeTrackerRepo = com.baverika.r_journal.repository.LifeTrackerRepository(db.lifeTrackerDao())


        // Biometric Lock State
        var isLocked by mutableStateOf(true)

        // Check if biometric is available, if not, unlock immediately
        if (!settingsRepo.isBiometricEnabled || !com.baverika.r_journal.utils.BiometricHelper.isBiometricAvailable(this)) {
            isLocked = false
        } else {
            // Prompt for auth
            com.baverika.r_journal.utils.BiometricHelper.authenticate(
                activity = this,
                onSuccess = { isLocked = false },
                onError = { /* Keep locked, maybe show retry button */ }
            )
        }

        // Initialize Biometric
        val biometricHelper = com.baverika.r_journal.utils.BiometricHelper

        // Schedule Daily Backup
        val backupRequest = androidx.work.PeriodicWorkRequestBuilder<com.baverika.r_journal.worker.BackupWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyBackup",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )

        setContent {
            var currentTheme by remember { mutableStateOf(settingsRepo.appTheme) }

            RJournalTheme(theme = currentTheme) {

                if (isLocked) {
                    // Lock Screen
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Journal Locked", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = {
                                com.baverika.r_journal.utils.BiometricHelper.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = { isLocked = false },
                                    onError = {}
                                )
                            }) {
                                Text("Unlock")
                            }
                        }
                    }
                } else {
                    // Check if launched from widget with navigation intent
                    val initialRoute = intent?.getStringExtra("navigate_to") ?: "archive"
                    
                    MainApp(
                        journalRepo = journalRepo,
                        quickNoteRepo = quickNoteRepo,
                        eventRepo = eventRepo,
                        passwordRepo = passwordRepo,
                        quoteRepo = quoteRepo,
                        widgetSettingsDataStore = widgetSettingsDataStore,
                        taskRepo = taskRepo,
                        lifeTrackerRepo = lifeTrackerRepo,
                        settingsRepo = settingsRepo,
                        initialRoute = initialRoute,
                        onThemeChanged = { newTheme -> currentTheme = newTheme }
                    )
                }


            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    journalRepo: JournalRepository,
    quickNoteRepo: QuickNoteRepository,
    eventRepo: EventRepository,
    passwordRepo: PasswordRepository,
    quoteRepo: com.baverika.r_journal.quotes.data.QuoteRepository,
    widgetSettingsDataStore: com.baverika.r_journal.quotes.settings.WidgetSettingsDataStore,
    taskRepo: com.baverika.r_journal.repository.TaskRepository,
    lifeTrackerRepo: com.baverika.r_journal.repository.LifeTrackerRepository,
    settingsRepo: SettingsRepository = SettingsRepository(LocalContext.current),
    initialRoute: String = "archive",
    onThemeChanged: (com.baverika.r_journal.ui.theme.AppTheme) -> Unit = {}
) {

    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    // ViewModel for app-wide events (Birthday Easter Egg)
    val mainViewModel: com.baverika.r_journal.ui.viewmodel.MainViewModel = viewModel(
        factory = com.baverika.r_journal.ui.viewmodel.MainViewModelFactory(settingsRepo)
    )
    val showBirthdayEasterEgg by mainViewModel.showBirthdayEasterEgg.collectAsState()
    val userAge by mainViewModel.userAge.collectAsState()
    
    // Check birthday on app resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.checkBirthday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Track current route for FAB visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 🔧 state to show/hide server settings dialog
    var showServerDialog by remember { mutableStateOf(false) }

    // Define top-level routes where the drawer should be accessible via swipe
    val topLevelRoutes = setOf(
        "archive", "quick_notes", "search", "dashboard",
        "calendar", "events", "export", "import", "settings", "habits", "quotes", "tasks", "life_trackers"
    )
    val isDrawerGestureEnabled = currentRoute in topLevelRoutes

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "R-Journal",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )

                    Divider()

                    DrawerContent(
                        currentRoute = currentRoute,
                        onScreenSelected = { route ->
                            navController.navigate(route) {
                                popUpTo("archive") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        onServerSettingsClick = {
                            showServerDialog = true
                        }
                    )
                }
            }
        },
        drawerState = drawerState,
        gesturesEnabled = isDrawerGestureEnabled
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("r_journal") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (currentRoute == "archive") {
                            // Search Icon
                            IconButton(onClick = { navController.navigate("search") }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Journals",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Biometric Toggle
                            var isBiometricEnabled by remember { mutableStateOf(settingsRepo.isBiometricEnabled) }
                            
                            IconToggleButton(
                                checked = isBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    isBiometricEnabled = enabled
                                    settingsRepo.isBiometricEnabled = enabled
                                }
                            ) {
                                Icon(
                                    imageVector = if (isBiometricEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isBiometricEnabled) "Biometric Lock On" else "Biometric Lock Off",
                                    tint = if (isBiometricEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Navigation content
                NavHost(
                    navController = navController,
                    startDestination = initialRoute
                ) {
                    // Archive screen (default/home)
                    composable("archive") {
                        JournalArchiveScreen(
                            journalRepo = journalRepo,
                            eventRepo = eventRepo,
                            onEntryClick = { entry ->
                                navController.navigate("chat_input/${entry.id}")
                            }
                        )
                    }

                    // Quick Notes
                    composable("quick_notes") {
                        QuickNotesScreen(
                            viewModel = viewModel(
                                factory = QuickNoteViewModelFactory(quickNoteRepo, context)
                            ),
                            navController = navController
                        )
                    }

                    // Search
                    composable("search") {
                        SearchScreen(
                            viewModel = viewModel(
                                factory = SearchViewModelFactory(journalRepo, context)
                            ),
                            navController = navController
                        )
                    }

                    // Dashboard
                    composable("dashboard") {
                        val habitViewModel: HabitViewModel = viewModel(
                            factory = HabitViewModelFactory(LocalContext.current.applicationContext as Application, journalRepo)
                        )
                        DashboardScreen(
                            journalRepo = journalRepo,
                            habitViewModel = habitViewModel,
                            onYearInPixelsClick = { navController.navigate("year_in_pixels") }
                        )
                    }

                    // Year in Pixels
                    composable("year_in_pixels") {
                        val viewModel: com.baverika.r_journal.ui.viewmodel.YearInPixelsViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.YearInPixelsViewModelFactory(journalRepo)
                        )
                        YearInPixelsScreen(viewModel = viewModel, navController = navController)
                    }

                    // Calendar
                    composable("calendar") {
                        CalendarScreen(journalRepo, navController)
                    }

                    // Events (Special Dates)
                    composable("events") {
                        EventsScreen(
                            viewModel = viewModel(
                                factory = EventViewModelFactory(eventRepo)
                            ),
                            navController = navController
                        )
                    }

                    // Export
                    composable("export") {
                        ExportScreen(
                            journalRepo = journalRepo,
                            quickNoteRepo = quickNoteRepo,
                            context = context
                        )
                    }

                    // Import
                    composable("import") {
                        ImportScreen(
                            journalRepo = journalRepo,
                            quickNoteRepo = quickNoteRepo
                        )
                    }

                    // Chat input for today's entry
                    composable("chat_input") {
                        val journalViewModel: com.baverika.r_journal.ui.viewmodel.JournalViewModel =
                            viewModel(factory = JournalViewModelFactory(journalRepo, eventRepo, context))

                        LaunchedEffect(Unit) {
                            journalViewModel.loadTodaysEntry()
                        }

                        ChatInputScreen(
                            viewModel = journalViewModel,
                            navController = navController
                        )
                    }

                    // Chat input for specific entry by ID
                    composable("chat_input/{entryId}") { backStackEntry ->
                        val entryId = backStackEntry.arguments?.getString("entryId")
                        if (entryId != null) {
                            val journalViewModel: com.baverika.r_journal.ui.viewmodel.JournalViewModel =
                                viewModel(factory = JournalViewModelFactory(journalRepo, eventRepo, context))

                            LaunchedEffect(entryId) {
                                journalViewModel.loadEntryForEditing(entryId)
                            }

                            ChatInputScreen(
                                viewModel = journalViewModel,
                                navController = navController
                            )
                        } else {
                            // Invalid entry ID, go back to archive
                            LaunchedEffect(Unit) {
                                navController.navigate("archive") {
                                    popUpTo("archive") { inclusive = true }
                                }
                            }
                        }
                    }

                    // Habits
                    composable("habits") {
                        val habitViewModel: HabitViewModel = viewModel(
                            factory = HabitViewModelFactory(LocalContext.current.applicationContext as Application, journalRepo)
                        )
                        HabitTrackerScreen(
                            viewModel = habitViewModel,
                            navController = navController
                        )
                    }

                    // Add/Edit Habit
                    composable("add_habit?habitId={habitId}") { backStackEntry ->
                        val habitId = backStackEntry.arguments?.getString("habitId")
                        val habitViewModel: HabitViewModel = viewModel(
                            factory = HabitViewModelFactory(LocalContext.current.applicationContext as Application, journalRepo)
                        )
                        AddEditHabitScreen(
                            viewModel = habitViewModel,
                            navController = navController,
                            habitId = habitId
                        )
                    }

                    // New quick note screen
                    composable("new_quick_note") {
                        val quickNoteViewModel: com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel =
                            viewModel(factory = QuickNoteViewModelFactory(quickNoteRepo, context))

                        NewQuickNoteScreen(
                            viewModel = quickNoteViewModel,
                            navController = navController
                        )
                    }

                    // Image viewer screen
                    composable("image_viewer/{encodedPath}") { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("encodedPath")
                        encodedPath?.let {
                            val decodedPath = remember(it) {
                                try {
                                    java.net.URLDecoder.decode(it, "UTF-8")
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            when {
                                decodedPath != null -> {
                                    ImageViewerScreen(
                                        imageUri = decodedPath,
                                        onDismiss = { navController.popBackStack() }
                                    )
                                }
                                else -> {
                                    LaunchedEffect(Unit) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }

                    // Password Generator
                    composable("password_generator") {
                        val viewModel: com.baverika.r_journal.ui.viewmodel.PasswordViewModel = viewModel(
                            factory = PasswordViewModelFactory(passwordRepo)
                        )
                        PasswordGeneratorScreen(
                            viewModel = viewModel
                        )
                    }

                    // Settings
                    composable("settings") {
                        SettingsScreen(
                            settingsRepo = settingsRepo,
                            passwordRepo = passwordRepo,
                            navController = navController,
                            onThemeChanged = onThemeChanged
                        )
                    }

                    // Motivational Quotes
                    composable("quotes") {
                        val quotesViewModel: com.baverika.r_journal.quotes.ui.QuotesViewModel = viewModel(
                            factory = com.baverika.r_journal.quotes.ui.QuotesViewModelFactory(quoteRepo, context)
                        )
                        com.baverika.r_journal.quotes.ui.QuotesScreen(
                            viewModel = quotesViewModel,
                            navController = navController
                        )
                    }

                    // Quote Widget Settings
                    composable("quote_widget_settings") {
                        com.baverika.r_journal.quotes.settings.WidgetSettingsScreen(
                            settingsDataStore = widgetSettingsDataStore,
                            navController = navController
                        )
                    }

                    // Life Trackers
                    composable("life_trackers") {
                        val vm: com.baverika.r_journal.ui.viewmodel.LifeTrackerViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.LifeTrackerViewModelFactory(lifeTrackerRepo)
                        )
                        com.baverika.r_journal.ui.screens.LifeTrackersScreen(
                            viewModel = vm,
                            onTrackerClick = { id -> navController.navigate("tracker_detail/$id") }
                        )
                    }

                    composable("tracker_detail/{trackerId}") { backStackEntry ->
                        val trackerId = backStackEntry.arguments?.getString("trackerId") ?: return@composable
                        val vm: com.baverika.r_journal.ui.viewmodel.TrackerDetailViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TrackerDetailViewModelFactory(lifeTrackerRepo, trackerId)
                        )
                        com.baverika.r_journal.ui.screens.TrackerDetailScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Tasks
                    composable("tasks") {
                        val taskViewModel: com.baverika.r_journal.ui.viewmodel.TaskViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TaskViewModelFactory(
                                LocalContext.current.applicationContext as android.app.Application,
                                taskRepo
                            )
                        )
                        TaskListScreen(
                            viewModel = taskViewModel,
                            navController = navController
                        )
                    }

                    // Add Task
                    composable("add_task") {
                        val taskViewModel: com.baverika.r_journal.ui.viewmodel.TaskViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TaskViewModelFactory(
                                LocalContext.current.applicationContext as android.app.Application,
                                taskRepo
                            )
                        )
                        AddEditTaskScreen(
                            viewModel = taskViewModel,
                            navController = navController,
                            taskId = null
                        )
                    }

                    // Edit Task
                    composable("edit_task/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")
                        val taskViewModel: com.baverika.r_journal.ui.viewmodel.TaskViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TaskViewModelFactory(
                                LocalContext.current.applicationContext as android.app.Application,
                                taskRepo
                            )
                        )
                        AddEditTaskScreen(
                            viewModel = taskViewModel,
                            navController = navController,
                            taskId = taskId
                        )
                    }
                }

                if (currentRoute == "archive") {
                    FloatingActionButton(
                        onClick = { navController.navigate("chat_input") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 36.dp, bottom = 80.dp)
                            .size(72.dp)
                    ) {
                        Icon(
                            Icons.Filled.Chat,
                            contentDescription = "New Journal Entry",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }


                if (showBirthdayEasterEgg) {
                    com.baverika.r_journal.ui.components.BirthdayEasterEggOverlay(
                        age = userAge,
                        onFinished = { mainViewModel.markBirthdayShown() }
                    )
                }
            }
        }
    }

    // 🔧 Server settings dialog, triggered from drawer
    if (showServerDialog) {
        ServerConfigDialog(
            onClose = { showServerDialog = false },
            onSave = { hostPort ->
                val trimmed = hostPort.trim()
                if (trimmed.isNotEmpty()) {
                    ServerPrefs.setHostPort(context, trimmed)
                    RetrofitClient.setHostPort(trimmed)
                }
                showServerDialog = false
            }
        )
    }
}

@Composable
fun DrawerContent(
    currentRoute: String?,
    onScreenSelected: (String) -> Unit,
    onServerSettingsClick: () -> Unit
) {
    Column {
        DrawerItem(
            icon = Icons.Filled.Book,
            label = "Journal Archive",
            isSelected = currentRoute == "archive",
            onClick = { onScreenSelected("archive") }
        )
        DrawerItem(
            icon = Icons.Filled.Note,
            label = "Notes",
            isSelected = currentRoute == "quick_notes",
            onClick = { onScreenSelected("quick_notes") }
        )
        DrawerItem(
            icon = Icons.Filled.BarChart,
            label = "Dashboard",
            isSelected = currentRoute == "dashboard",
            onClick = { onScreenSelected("dashboard") }
        )
        DrawerItem(
            icon = Icons.Filled.CheckCircle,
            label = "Habits",
            isSelected = currentRoute == "habits",
            onClick = { onScreenSelected("habits") }
        )
        DrawerItem(
            icon = Icons.Filled.DateRange, 
            label = "Life Trackers",
            isSelected = currentRoute == "life_trackers",
            onClick = { onScreenSelected("life_trackers") }
        )
        DrawerItem(
            icon = Icons.Filled.Checklist,
            label = "Tasks",
            isSelected = currentRoute == "tasks",
            onClick = { onScreenSelected("tasks") }
        )
        DrawerItem(
            icon = Icons.Filled.FormatQuote,
            label = "Quotes",
            isSelected = currentRoute == "quotes",
            onClick = { onScreenSelected("quotes") }
        )
        DrawerItem(
            icon = Icons.Filled.CalendarMonth,
            label = "Calendar",
            isSelected = currentRoute == "calendar",
            onClick = { onScreenSelected("calendar") }
        )

        DrawerItem(
            icon = Icons.Filled.VpnKey,
            label = "Password Generator",
            isSelected = currentRoute == "password_generator",
            onClick = { onScreenSelected("password_generator") }
        )

        DrawerItem(
            icon = Icons.Filled.Event,

            label = "Special Dates",
            isSelected = currentRoute == "events",
            onClick = { onScreenSelected("events") }
        )

        DrawerItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            isSelected = currentRoute == "settings",
            onClick = { onScreenSelected("settings") }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // 🔧 NEW: Server settings item
        DrawerItem(
            icon = Icons.Filled.Details,
            label = "Server Details",
            isSelected = false, // not a screen, just a dialog
            onClick = { onServerSettingsClick() }
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun ServerConfigDialog(
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var text by remember {
        mutableStateOf(ServerPrefs.getHostPort(context))
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Server address") },
        text = {
            Column {
                Text(
                    "Enter IP:port of your Flask server.\n\n" +
                            "Examples:\n127.0.0.1:5000 (same phone)\n192.168.x.x:5000 (Wi-Fi / hotspot)",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("IP:port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        }
    )
}
