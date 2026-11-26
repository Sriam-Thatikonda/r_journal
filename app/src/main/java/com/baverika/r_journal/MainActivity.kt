// app/src/main/java/com/baverika/r_journal/MainActivity.kt

package com.baverika.r_journal

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.repository.ChallengeRepository
import com.baverika.r_journal.repository.CravingQuestRepository
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.PasswordRepository
import com.baverika.r_journal.repository.QuickNoteRepository
import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.ui.components.AmbientBackdrop
import com.baverika.r_journal.ui.components.FloatingBottomDock
import com.baverika.r_journal.ui.components.PremiumDrawerContent
import com.baverika.r_journal.ui.components.PremiumFab
import com.baverika.r_journal.ui.components.PremiumLockScreen
import com.baverika.r_journal.ui.components.PremiumSpeedDialSheet
import com.baverika.r_journal.ui.components.PremiumTopBar
import com.baverika.r_journal.ui.screens.*
import com.baverika.r_journal.ui.theme.AppFontStyle
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.RJournalTheme
import com.baverika.r_journal.ui.theme.RouteTransitions
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModel
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModelFactory
import com.baverika.r_journal.ui.viewmodel.EventViewModelFactory
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import com.baverika.r_journal.ui.viewmodel.HabitViewModelFactory
import com.baverika.r_journal.ui.viewmodel.JournalViewModelFactory
import com.baverika.r_journal.ui.viewmodel.PasswordViewModelFactory
import com.baverika.r_journal.ui.viewmodel.QuickNoteViewModelFactory
import com.baverika.r_journal.ui.viewmodel.SearchViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    // Define Bottom Navigation Items
    sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
        data object Journal : BottomNavItem("archive", Icons.AutoMirrored.Filled.MenuBook, "Journal")
        data object QuickNotes : BottomNavItem("quick_notes", Icons.AutoMirrored.Filled.Note, "Notes")
        data object Tasks : BottomNavItem("tasks", Icons.Filled.Checklist, "Tasks")
    }

    private var isLocked by mutableStateOf(true)
    private var lockErrorMessage by mutableStateOf<String?>(null)
    private lateinit var settingsRepo: SettingsRepository

    companion object {
        fun updateSecureWindowFlag(activity: android.app.Activity?, enabled: Boolean) {
            activity ?: return
            if (enabled) {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateSecureWindowFlag(this, settingsRepo.isBiometricEnabled)
        if (isLocked && settingsRepo.isBiometricEnabled && com.baverika.r_journal.utils.BiometricHelper.isBiometricAvailable(this)) {
            lockErrorMessage = null
            com.baverika.r_journal.utils.BiometricHelper.authenticate(
                activity = this,
                onSuccess = {
                    isLocked = false
                    lockErrorMessage = null
                },
                onError = { /* Keep locked; user can tap Unlock */ }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (
            !isChangingConfigurations &&
            settingsRepo.isBiometricEnabled &&
            !com.baverika.r_journal.utils.ExternalLauncherGuard.consumeIfAwaiting()
        ) {
            isLocked = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val db = JournalDatabase.getDatabase(this)
        val journalRepo = JournalRepository(db.journalDao())
        val quickNoteRepo = QuickNoteRepository(db.quickNoteDao())
        val eventRepo = EventRepository(db.eventDao())
        val passwordRepo = PasswordRepository(db.passwordDao())
        settingsRepo = SettingsRepository(this)
        updateSecureWindowFlag(this, settingsRepo.isBiometricEnabled)
        
        // Quote feature repositories
        val quoteRepo = com.baverika.r_journal.quotes.data.QuoteRepository(db.quoteDao())
        val widgetSettingsDataStore = com.baverika.r_journal.quotes.settings.WidgetSettingsDataStore.getInstance(this)
        
        // Task feature repositories
        val taskRepo = com.baverika.r_journal.repository.TaskRepository(db.taskDao())

        // Life Tracker repository
        val lifeTrackerRepo = com.baverika.r_journal.repository.LifeTrackerRepository(db.lifeTrackerDao())

        // Trackers repository
        val trackerRepo = com.baverika.r_journal.repository.TrackerRepository(db.trackerDao())

        // Craving Quest repository
        val cravingRepo = CravingQuestRepository(db.cravingLogDao())

        // Challenge Tracker repository
        val challengeRepo = ChallengeRepository(db.challengeDao())


        // Biometric Lock State
        val biometricAvailable = com.baverika.r_journal.utils.BiometricHelper.isBiometricAvailable(this)
        if (!settingsRepo.isBiometricEnabled) {
            isLocked = false
        } else if (!biometricAvailable) {
            if (!settingsRepo.hasExplicitBiometricSetting()) {
                // First install on a device without screen lock configured
                settingsRepo.isBiometricEnabled = false
                updateSecureWindowFlag(this, false)
                isLocked = false
            } else {
                isLocked = true
                lockErrorMessage = "Enable a screen lock or biometric in Android Settings to unlock."
            }
        }



        // Schedule Daily Backup
        // Auto-backup disabled by user request
        androidx.work.WorkManager.getInstance(this).cancelUniqueWork("DailyBackup")

        // Initialize notification channels
        com.baverika.r_journal.notifications.AppNotificationManager.createChannels(this)

        // Schedule proactive notifications (Habits, Backup, Tasks, Journal, Events)
        com.baverika.r_journal.worker.DailyNudgeWorker.schedule(this)

        // Schedule periodic tasks checks & reminders
        com.baverika.r_journal.worker.TaskReminderWorker.scheduleDailySummary(this)

        // Ensure tasks for upcoming special dates (today and 1 day prior) are created
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.baverika.r_journal.utils.EventTaskGenerator.ensureEventTasksCreated(this@MainActivity)
        }

        setContent {
            var currentTheme by remember { mutableStateOf(settingsRepo.appTheme) }
            var currentFontStyle by remember { mutableStateOf(settingsRepo.appFontStyle) }

            RJournalTheme(theme = currentTheme, fontStyle = currentFontStyle) {

                if (isLocked) {
                    PremiumLockScreen(
                        errorMessage = lockErrorMessage,
                        onUnlockClick = {
                            if (com.baverika.r_journal.utils.BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                                lockErrorMessage = null
                                com.baverika.r_journal.utils.BiometricHelper.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isLocked = false
                                        lockErrorMessage = null
                                    },
                                    onError = {}
                                )
                            } else {
                                lockErrorMessage = "Enable a screen lock or biometric in Android Settings to unlock."
                            }
                        }
                    )
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
                        trackerRepo = trackerRepo,
                        cravingRepo = cravingRepo,
                        challengeRepo = challengeRepo,
                        settingsRepo = settingsRepo,
                        initialRoute = initialRoute,
                        onThemeChanged = { newTheme -> currentTheme = newTheme },
                        onFontStyleChanged = { newFontStyle -> currentFontStyle = newFontStyle }
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
    trackerRepo: com.baverika.r_journal.repository.TrackerRepository,
    cravingRepo: CravingQuestRepository,
    challengeRepo: ChallengeRepository,
    settingsRepo: SettingsRepository = SettingsRepository(LocalContext.current),
    initialRoute: String = "archive",
    onThemeChanged: (AppTheme) -> Unit = {},
    onFontStyleChanged: (AppFontStyle) -> Unit = {}
) {

    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val tokens = LocalDesignTokens.current

    // ViewModel for app-wide events (Birthday Easter Egg)
    val mainViewModel: com.baverika.r_journal.ui.viewmodel.MainViewModel = viewModel(
        factory = com.baverika.r_journal.ui.viewmodel.MainViewModelFactory(settingsRepo)
    )
    val showBirthdayEasterEgg by mainViewModel.showBirthdayEasterEgg.collectAsState()
    val userAge by mainViewModel.userAge.collectAsState()
    
    // Check birthday on app resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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

    // Define top-level routes where the drawer should be accessible via swipe
    val topLevelRoutes = setOf(
        "archive", "chat_input/mine", "quick_notes", "search", "dashboard",
        "calendar", "events", "export", "import", "settings", "habits", "quotes", "tasks", "life_trackers", "craving_quest", "challenges", "trackers", "password_generator"
    )
    val isDrawerGestureEnabled = currentRoute in topLevelRoutes

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = tokens.sheetContainer
            ) {
                PremiumDrawerContent(
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
                    }
                )
            }
        },
        drawerState = drawerState,
        gesturesEnabled = isDrawerGestureEnabled
    ) {
        var isSearchActive by remember { mutableStateOf(false) }
        var topBarSearchQuery by remember { mutableStateOf("") }

        LaunchedEffect(currentRoute) {
            isSearchActive = false
            topBarSearchQuery = ""
        }

        val searchFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        // Intercept back gesture when search is active to clear search instead of exiting the screen
        BackHandler(enabled = isSearchActive || topBarSearchQuery.isNotEmpty()) {
            isSearchActive = false
            topBarSearchQuery = ""
            keyboardController?.hide()
        }

        var isBiometricEnabled by remember(currentRoute) { mutableStateOf(settingsRepo.isBiometricEnabled) }
        var showSpeedDialSheet by remember { mutableStateOf(false) }

        val quickNoteViewModelForTopBar: com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel? =
            if (currentRoute == "quick_notes") {
                viewModel(factory = QuickNoteViewModelFactory(quickNoteRepo, context))
            } else null
        val quickNotesLayoutType = quickNoteViewModelForTopBar?.layoutType?.collectAsState()?.value

        val screenTitle = when {
            currentRoute == "archive" -> "R-Journal"
            currentRoute == "quick_notes" -> "Quick Notes"
            currentRoute == "tasks" -> "Tasks"
            currentRoute == "habits" -> "Habit Tracker"
            currentRoute == "password_generator" -> "Password Vault"
            currentRoute == "quotes" -> "Quotes"
            currentRoute == "calendar" -> "Calendar"
            currentRoute == "events" -> "Special Dates"
            currentRoute == "life_trackers" -> "Life Milestones"
            currentRoute == "trackers" -> "Trackers"
            currentRoute == "craving_quest" -> "Craving Quest"
            currentRoute == "challenges" -> "Challenges"
            currentRoute == "challenge_history" -> "Challenge History"
            currentRoute?.startsWith("challenge_detail") == true -> "Challenge Details"
            currentRoute == "year_in_pixels" -> "Mood Heatmap"
            currentRoute == "search" -> "Search"
            currentRoute == "dashboard" -> "Dashboard"
            currentRoute == "settings" -> "Settings"
            currentRoute?.startsWith("chat_input") == true -> {
                val backStackEntry = navController.currentBackStackEntry
                if (backStackEntry != null) {
                    val journalViewModel: com.baverika.r_journal.ui.viewmodel.JournalViewModel =
                        viewModel(viewModelStoreOwner = backStackEntry, factory = JournalViewModelFactory(journalRepo, eventRepo, context))
                    val entry = journalViewModel.currentEntry
                    if (entry.id == "mine") {
                        "Mine"
                    } else {
                        val dateText = entry.localDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                        if (journalViewModel.isCurrentEntryToday) "Today • $dateText" else dateText
                    }
                } else "Journal Entry"
            }
            else -> "R-Journal"
        }

        val bottomNavItems = listOf(
            MainActivity.BottomNavItem.Journal,
            MainActivity.BottomNavItem.QuickNotes,
            MainActivity.BottomNavItem.Tasks
        )
        val showBottomBar = currentRoute in bottomNavItems.map { it.route }

        val fabAction: (() -> Unit)?
        val fabIcon: ImageVector?
        val fabDesc: String?

        when (currentRoute) {
            "archive" -> {
                fabAction = { navController.navigate("chat_input") }
                fabIcon = Icons.AutoMirrored.Filled.Chat
                fabDesc = "New Journal Entry"
            }
            "quick_notes" -> {
                fabAction = { navController.navigate("new_quick_note") }
                fabIcon = Icons.Filled.Add
                fabDesc = "New Quick Note"
            }
            "tasks" -> {
                fabAction = { navController.navigate("add_task") }
                fabIcon = Icons.Filled.Add
                fabDesc = "New Task"
            }
            "trackers" -> {
                fabAction = { navController.navigate("add_tracker") }
                fabIcon = Icons.Filled.Add
                fabDesc = "New Tracker"
            }
            "craving_quest" -> {
                fabAction = { navController.navigate("add_craving") }
                fabIcon = Icons.Filled.Add
                fabDesc = "Log Craving"
            }
            "challenges" -> {
                fabAction = { navController.navigate("create_challenge") }
                fabIcon = Icons.Filled.Add
                fabDesc = "New Challenge"
            }
            "habits" -> {
                fabAction = { navController.navigate("add_habit") }
                fabIcon = Icons.Filled.Add
                fabDesc = "New Habit"
            }
            else -> {
                fabAction = null
                fabIcon = null
                fabDesc = null
            }
        }

        if (showSpeedDialSheet) {
            PremiumSpeedDialSheet(
                onDismiss = { showSpeedDialSheet = false },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val screensWithCustomHeader = setOf(
                    "new_quick_note",
                    "add_task",
                    "add_tracker",
                    "add_craving",
                    "create_challenge",
                    "image_viewer",
                    "search"
                )
                val hideGlobalTopBar = screensWithCustomHeader.contains(currentRoute) ||
                    currentRoute?.startsWith("search") == true ||
                    currentRoute?.startsWith("edit_quick_note") == true ||
                    currentRoute?.startsWith("edit_task") == true ||
                    currentRoute?.startsWith("add_habit") == true ||
                    currentRoute?.startsWith("edit_tracker") == true ||
                    currentRoute?.startsWith("habit_detail") == true ||
                    currentRoute?.startsWith("habit_year_overview") == true ||
                    currentRoute?.startsWith("tracker_detail") == true ||
                    currentRoute?.startsWith("craving_detail") == true

                if (!hideGlobalTopBar) {
                    PremiumTopBar(
                        currentRoute = currentRoute,
                        screenTitle = screenTitle,
                        isSearchActive = isSearchActive,
                        topBarSearchQuery = topBarSearchQuery,
                        searchFocusRequester = searchFocusRequester,
                        isBiometricEnabled = isBiometricEnabled,
                        quickNotesLayoutType = quickNotesLayoutType,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onBack = { navController.popBackStack() },
                        onActivateSearch = { isSearchActive = true },
                        onCloseSearch = {
                            isSearchActive = false
                            topBarSearchQuery = ""
                            keyboardController?.hide()
                        },
                        onSearchQueryChange = { topBarSearchQuery = it },
                        onSubmitGlobalSearch = {
                            val q = topBarSearchQuery.trim()
                            isSearchActive = false
                            topBarSearchQuery = ""
                            keyboardController?.hide()
                            if (q.isNotEmpty()) {
                                navController.navigate("search?query=${android.net.Uri.encode(q)}")
                            } else {
                                navController.navigate("search")
                            }
                        },
                        onOpenGlobalSearch = { navController.navigate("search") },
                        onHideKeyboard = { keyboardController?.hide() },
                        onToggleBiometric = { enabled ->
                            val activity = context as? FragmentActivity
                            if (enabled) {
                                if (com.baverika.r_journal.utils.BiometricHelper.isBiometricAvailable(context)) {
                                    isBiometricEnabled = true
                                    settingsRepo.isBiometricEnabled = true
                                    MainActivity.updateSecureWindowFlag(activity, true)
                                } else {
                                    Toast.makeText(context, "Set up a screen lock or biometric in Android Settings first.", Toast.LENGTH_SHORT).show()
                                }
                            } else if (activity != null && com.baverika.r_journal.utils.BiometricHelper.isBiometricAvailable(context)) {
                                com.baverika.r_journal.utils.BiometricHelper.authenticate(
                                    activity = activity,
                                    title = "Disable Biometric Lock",
                                    subtitle = "Confirm your identity to turn off app lock",
                                    onSuccess = {
                                        isBiometricEnabled = false
                                        settingsRepo.isBiometricEnabled = false
                                        MainActivity.updateSecureWindowFlag(activity, false)
                                    },
                                    onError = {}
                                )
                            } else {
                                isBiometricEnabled = false
                                settingsRepo.isBiometricEnabled = false
                                MainActivity.updateSecureWindowFlag(activity, false)
                            }
                        },
                        onToggleQuickNotesLayout = {
                            quickNoteViewModelForTopBar?.let { vm ->
                                val newLayout = if (quickNotesLayoutType == com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY) {
                                    com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_LIST
                                } else {
                                    com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY
                                }
                                vm.setLayoutType(newLayout)
                            }
                        },
                        onOpenQuoteSettings = { navController.navigate("quote_widget_settings") },
                        onOpenChallengeHistory = { navController.navigate("challenge_history") }
                    )
                }
            },
            bottomBar = {
                FloatingBottomDock(
                    visible = showBottomBar,
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onSelect = { item ->
                        navController.navigate(item.route) {
                            popUpTo("archive") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            },
            floatingActionButton = {
                PremiumFab(
                    fabAction = fabAction,
                    fabIcon = fabIcon,
                    fabDesc = fabDesc,
                    onLongPressSpeedDial = { showSpeedDialSheet = true }
                )
            }
        ) { padding ->
            AmbientBackdrop(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = initialRoute,
                    enterTransition = { RouteTransitions.enterTransition(this) },
                    exitTransition = { RouteTransitions.exitTransition(this) },
                    popEnterTransition = { RouteTransitions.popEnterTransition(this) },
                    popExitTransition = { RouteTransitions.popExitTransition(this) }
                ) {
                    // Archive screen (default/home)
                    composable("archive") {
                        JournalArchiveScreen(
                            journalRepo = journalRepo,
                            eventRepo = eventRepo,
                            onEntryClick = { entry ->
                                navController.navigate("chat_input/${entry.id}")
                            },
                            searchQuery = topBarSearchQuery
                        )
                    }

                    // Quick Notes
                    composable("quick_notes") {
                        val quickNoteViewModel: com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel = viewModel(
                            factory = QuickNoteViewModelFactory(quickNoteRepo, context)
                        )
                        LaunchedEffect(topBarSearchQuery) {
                            quickNoteViewModel.onSearchQueryChange(topBarSearchQuery)
                        }
                        QuickNotesScreen(
                            viewModel = quickNoteViewModel,
                            navController = navController
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            viewModel = viewModel(
                                factory = SearchViewModelFactory(
                                    journalRepo,
                                    quickNoteRepo,
                                    taskRepo,
                                    trackerRepo,
                                    quoteRepo,
                                    context
                                )
                            ),
                            navController = navController,
                            initialQuery = ""
                        )
                    }

                    composable(
                        route = "search?query={query}",
                        arguments = listOf(androidx.navigation.navArgument("query") {
                            type = androidx.navigation.NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val initialQuery = backStackEntry.arguments?.getString("query") ?: ""
                        SearchScreen(
                            viewModel = viewModel(
                                factory = SearchViewModelFactory(
                                    journalRepo,
                                    quickNoteRepo,
                                    taskRepo,
                                    trackerRepo,
                                    quoteRepo,
                                    context
                                )
                            ),
                            navController = navController,
                            initialQuery = initialQuery
                        )
                    }

                    // Dashboard
                    composable("dashboard") {
                        val habitViewModel: HabitViewModel = viewModel(
                            factory = HabitViewModelFactory(LocalContext.current.applicationContext as Application, journalRepo)
                        )
                        DashboardScreen(
                            journalRepo = journalRepo,
                            taskRepo = taskRepo,
                            habitViewModel = habitViewModel,
                            onYearInPixelsClick = { navController.navigate("year_in_pixels") },
                            onNavigate = { route -> navController.navigate(route) }
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
                        CalendarScreen(journalRepo, navController, eventRepo)
                    }

                    // Events (Special Dates)
                    composable("events") {
                        EventsScreen(
                            viewModel = viewModel(
                                factory = EventViewModelFactory(eventRepo, taskRepo, context)
                            ),
                            navController = navController
                        )
                    }

                    // Export
                    composable("export") {
                        ExportScreen(
                            journalRepo = journalRepo,
                            quickNoteRepo = quickNoteRepo,
                            taskRepo = taskRepo,
                            quoteRepo = quoteRepo,
                            lifeTrackerRepo = lifeTrackerRepo,
                            eventRepo = eventRepo,
                            passwordRepo = passwordRepo,
                            trackerRepo = trackerRepo,
                            challengeRepo = challengeRepo,
                            context = context
                        )
                    }

                    // Import
                    composable("import") {
                        ImportScreen(
                            journalRepo = journalRepo,
                            quickNoteRepo = quickNoteRepo,
                            taskRepo = taskRepo,
                            quoteRepo = quoteRepo,
                            lifeTrackerRepo = lifeTrackerRepo,
                            eventRepo = eventRepo,
                            passwordRepo = passwordRepo,
                            trackerRepo = trackerRepo,
                            challengeRepo = challengeRepo
                        )
                    }

                    // Chat input for today's entry
                    composable("chat_input") { backStackEntry ->
                        val journalViewModel: com.baverika.r_journal.ui.viewmodel.JournalViewModel =
                            viewModel(viewModelStoreOwner = backStackEntry, factory = JournalViewModelFactory(journalRepo, eventRepo, context))

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
                                viewModel(viewModelStoreOwner = backStackEntry, factory = JournalViewModelFactory(journalRepo, eventRepo, context))

                            LaunchedEffect(entryId) {
                                journalViewModel.loadEntryForEditing(entryId)
                            }

                            if (entryId == "mine") {
                                MineScreen(
                                    viewModel = journalViewModel,
                                    navController = navController
                                )
                            } else {
                                ChatInputScreen(
                                    viewModel = journalViewModel,
                                    navController = navController
                                )
                            }
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

                    // Habit Year Overview (New)
                    composable("habit_year_overview/{habitId}") { backStackEntry ->
                        val habitId = backStackEntry.arguments?.getString("habitId") ?: return@composable
                        val habitViewModel: HabitViewModel = viewModel(
                            factory = HabitViewModelFactory(LocalContext.current.applicationContext as Application, journalRepo)
                        )
                        HabitYearOverviewScreen(
                            viewModel = habitViewModel,
                            navController = navController,
                            habitId = habitId
                        )
                    }

                    // Habit Detail (New)
                    composable("habit_detail/{habitId}/{month}") { backStackEntry ->
                        val habitId = backStackEntry.arguments?.getString("habitId") ?: return@composable
                        val monthStr = backStackEntry.arguments?.getString("month")
                        val month = monthStr?.toIntOrNull() ?: java.time.LocalDate.now().monthValue
                        
                        val habitViewModel: HabitViewModel = viewModel(
                            factory = HabitViewModelFactory(LocalContext.current.applicationContext as Application, journalRepo)
                        )
                        HabitDetailScreen(
                            viewModel = habitViewModel,
                            navController = navController,
                            habitId = habitId,
                            initialMonth = month
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

                    // Edit quick note screen
                    composable("edit_quick_note/{noteId}") { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
                        val quickNoteViewModel: com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel =
                            viewModel(factory = QuickNoteViewModelFactory(quickNoteRepo, context))

                        EditNoteScreen(
                            noteId = noteId,
                            viewModel = quickNoteViewModel,
                            navController = navController
                        )
                    }

                    // Image viewer screen
                    composable("image_viewer/{encodedPath}") { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("encodedPath")
                        val decodedPath = remember(encodedPath) {
                            encodedPath?.takeIf { it.isNotBlank() }
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
                            journalRepo = journalRepo,
                            taskRepo = taskRepo,
                            navController = navController,
                            onThemeChanged = onThemeChanged,
                            onFontStyleChanged = onFontStyleChanged
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
                        LifeTrackersScreen(
                            viewModel = vm,
                            onTrackerClick = { id -> navController.navigate("tracker_detail/$id") }
                        )
                    }

                    composable("tracker_detail/{trackerId}") { backStackEntry ->
                        val trackerId = backStackEntry.arguments?.getString("trackerId") ?: return@composable
                        val vm: com.baverika.r_journal.ui.viewmodel.TrackerDetailViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TrackerDetailViewModelFactory(lifeTrackerRepo, trackerId)
                        )
                        TrackerDetailScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Trackers (Generic counters)
                    composable("trackers") {
                        val vm: com.baverika.r_journal.ui.viewmodel.TrackerViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TrackerViewModelFactory(context.applicationContext as Application, trackerRepo)
                        )
                        TrackersScreen(
                            viewModel = vm,
                            navController = navController,
                            searchQuery = topBarSearchQuery
                        )
                    }

                    composable("add_tracker") {
                        val vm: com.baverika.r_journal.ui.viewmodel.TrackerViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TrackerViewModelFactory(context.applicationContext as Application, trackerRepo)
                        )
                        AddEditTrackerScreen(
                            trackerId = null,
                            viewModel = vm,
                            navController = navController
                        )
                    }

                    composable("edit_tracker/{trackerId}") { backStackEntry ->
                        val trackerId = backStackEntry.arguments?.getString("trackerId") ?: return@composable
                        val vm: com.baverika.r_journal.ui.viewmodel.TrackerViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TrackerViewModelFactory(context.applicationContext as Application, trackerRepo)
                        )
                        AddEditTrackerScreen(
                            trackerId = trackerId,
                            viewModel = vm,
                            navController = navController
                        )
                    }

                    composable("tracker_details/{trackerId}") { backStackEntry ->
                        val trackerId = backStackEntry.arguments?.getString("trackerId") ?: return@composable
                        val vm: com.baverika.r_journal.ui.viewmodel.TrackerViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TrackerViewModelFactory(context.applicationContext as Application, trackerRepo)
                        )
                        TrackerDetailsScreen(
                            trackerId = trackerId,
                            viewModel = vm,
                            navController = navController
                        )
                    }

                    // Tasks
                    composable("tasks") {
                        val taskViewModel: com.baverika.r_journal.ui.viewmodel.TaskViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TaskViewModelFactory(
                                LocalContext.current.applicationContext as Application,
                                taskRepo
                            )
                        )
                        LaunchedEffect(topBarSearchQuery) {
                            taskViewModel.setSearchQuery(topBarSearchQuery)
                        }
                        TaskListScreen(
                            viewModel = taskViewModel,
                            navController = navController
                        )
                    }

                    // Add Task
                    composable("add_task") {
                        val taskViewModel: com.baverika.r_journal.ui.viewmodel.TaskViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.viewmodel.TaskViewModelFactory(
                                LocalContext.current.applicationContext as Application,
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
                                LocalContext.current.applicationContext as Application,
                                taskRepo
                            )
                        )
                        AddEditTaskScreen(
                            viewModel = taskViewModel,
                            navController = navController,
                            taskId = taskId
                        )
                    }

                    // Craving Quest
                    composable("craving_quest") {
                        val vm: CravingQuestViewModel = viewModel(
                            factory = CravingQuestViewModelFactory(cravingRepo)
                        )
                        CravingQuestScreen(viewModel = vm, navController = navController)
                    }

                    composable("add_craving") {
                        val vm: CravingQuestViewModel = viewModel(
                            factory = CravingQuestViewModelFactory(cravingRepo)
                        )
                        AddCravingQuestScreen(viewModel = vm, navController = navController)
                    }

                    composable("craving_detail/{logId}") { backStackEntry ->
                        val logId = backStackEntry.arguments?.getString("logId") ?: return@composable
                        val vm: CravingQuestViewModel = viewModel(
                            factory = CravingQuestViewModelFactory(cravingRepo)
                        )
                        CravingDetailScreen(logId = logId, viewModel = vm, navController = navController)
                    }

                    // Challenge Tracker
                    composable("challenges") {
                        val viewModel: com.baverika.r_journal.ui.challenge.viewmodel.ChallengeListViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.challenge.viewmodel.ChallengeViewModelFactory(challengeRepo)
                        )
                        com.baverika.r_journal.ui.challenge.screens.ChallengeListScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }

                    composable("challenge_history") {
                        val viewModel: com.baverika.r_journal.ui.challenge.viewmodel.ChallengeHistoryViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.challenge.viewmodel.ChallengeViewModelFactory(challengeRepo)
                        )
                        com.baverika.r_journal.ui.challenge.screens.ChallengeHistoryScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }

                    composable("create_challenge") {
                        val viewModel: com.baverika.r_journal.ui.challenge.viewmodel.CreateChallengeViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.challenge.viewmodel.ChallengeViewModelFactory(challengeRepo)
                        )
                        com.baverika.r_journal.ui.challenge.screens.CreateChallengeScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }

                    composable(
                        "challenge_detail/{challengeId}",
                        arguments = listOf(androidx.navigation.navArgument("challengeId") { type = androidx.navigation.NavType.LongType })
                    ) { backStackEntry ->
                        val viewModel: com.baverika.r_journal.ui.challenge.viewmodel.ChallengeDetailViewModel = viewModel(
                            factory = com.baverika.r_journal.ui.challenge.viewmodel.ChallengeViewModelFactory(challengeRepo)
                        )
                        com.baverika.r_journal.ui.challenge.screens.ChallengeDetailScreen(
                            viewModel = viewModel,
                            navController = navController
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
}


