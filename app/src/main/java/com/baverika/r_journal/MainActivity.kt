// app/src/main/java/com/baverika/r_journal/MainActivity.kt

package com.baverika.r_journal

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.QuickNoteRepository

import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.repository.PasswordRepository
import com.baverika.r_journal.repository.CravingQuestRepository
import com.baverika.r_journal.repository.ChallengeRepository
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModel
import com.baverika.r_journal.ui.viewmodel.CravingQuestViewModelFactory

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
    // Define Bottom Navigation Items
    sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
        data object Journal : BottomNavItem("archive", Icons.AutoMirrored.Filled.MenuBook, "Journal")
        data object QuickNotes : BottomNavItem("quick_notes", Icons.AutoMirrored.Filled.Note, "Notes")
        data object Tasks : BottomNavItem("tasks", Icons.Filled.Checklist, "Tasks")
    }
    
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

        // Trackers repository
        val trackerRepo = com.baverika.r_journal.repository.TrackerRepository(db.trackerDao())

        // Craving Quest repository
        val cravingRepo = CravingQuestRepository(db.cravingLogDao())

        // Challenge Tracker repository
        val challengeRepo = ChallengeRepository(db.challengeDao())


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



        // Schedule Daily Backup
        // Auto-backup disabled by user request
        androidx.work.WorkManager.getInstance(this).cancelUniqueWork("DailyBackup")

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
                        trackerRepo = trackerRepo,
                        cravingRepo = cravingRepo,
                        challengeRepo = challengeRepo,
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
    trackerRepo: com.baverika.r_journal.repository.TrackerRepository,
    cravingRepo: CravingQuestRepository,
    challengeRepo: ChallengeRepository,
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
        "calendar", "events", "export", "import", "settings", "habits", "quotes", "tasks", "life_trackers", "craving_quest", "challenges", "trackers"
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

                    HorizontalDivider()

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
                        }
                    )
                }
            }
        },
        drawerState = drawerState,
        gesturesEnabled = isDrawerGestureEnabled
    ) {
        @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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

        Scaffold(
            topBar = {
                val screensWithCustomHeader = setOf(
                    "new_quick_note",
                    "add_task",
                    "add_tracker",
                    "add_craving",
                    "create_challenge",
                    "image_viewer"
                )
                val hideGlobalTopBar = screensWithCustomHeader.contains(currentRoute) ||
                        currentRoute?.startsWith("edit_quick_note") == true ||
                        currentRoute?.startsWith("edit_task") == true ||
                        currentRoute?.startsWith("add_habit") == true ||
                        currentRoute?.startsWith("edit_tracker") == true ||
                        currentRoute?.startsWith("habit_detail") == true ||
                        currentRoute?.startsWith("habit_year_overview") == true ||
                        currentRoute?.startsWith("tracker_detail") == true ||
                        currentRoute?.startsWith("craving_detail") == true

                if (!hideGlobalTopBar) {
                    if (isSearchActive) {
                        TopAppBar(
                            title = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (topBarSearchQuery.isEmpty()) {
                                        val placeholderText = when (currentRoute) {
                                            "archive" -> "Search journals..."
                                            "quick_notes" -> "Search notes..."
                                            "tasks" -> "Search tasks..."
                                            "trackers" -> "Search trackers..."
                                            else -> "Search..."
                                        }
                                        Text(
                                            text = placeholderText,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    BasicTextField(
                                        value = topBarSearchQuery,
                                        onValueChange = { topBarSearchQuery = it },
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    topBarSearchQuery = ""
                                    keyboardController?.hide()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                                }
                            },
                            actions = {
                                if (topBarSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { topBarSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                                if (currentRoute == "quick_notes") {
                                    val quickNoteViewModel: com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel = viewModel(
                                        factory = QuickNoteViewModelFactory(quickNoteRepo, context)
                                    )
                                    val layoutType by quickNoteViewModel.layoutType.collectAsState()
                                    IconButton(
                                        onClick = {
                                            val newLayout = if (layoutType == com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY) {
                                                com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_LIST
                                            } else {
                                                com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY
                                            }
                                            quickNoteViewModel.setLayoutType(newLayout)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (layoutType == com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY) {
                                                Icons.Default.ViewAgenda
                                            } else {
                                                Icons.Default.GridView
                                            },
                                            contentDescription = "Toggle Layout"
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        val screenTitle = when {
                            currentRoute == "archive" -> "R-Journal"
                            currentRoute == "quick_notes" -> "Quick Notes"
                            currentRoute == "tasks" -> "Tasks"
                            currentRoute == "habits" -> "Habit Tracker"
                            currentRoute == "password_generator" -> "Password Generator"
                            currentRoute == "quotes" -> "Motivational Quotes"
                            currentRoute == "calendar" -> "Calendar"
                            currentRoute == "events" -> "Special Dates"
                            currentRoute == "life_trackers" -> "Life Trackers"
                            currentRoute == "trackers" -> "Trackers"
                            currentRoute == "craving_quest" -> "Craving Quest"
                            currentRoute == "challenges" -> "Challenge Tracker"
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

                        val showBackButton = currentRoute != "archive"

                        TopAppBar(
                            title = { 
                                Text(
                                    text = screenTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            navigationIcon = {
                                if (showBackButton) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                } else {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                }
                            },
                            actions = {
                                if (currentRoute == "archive" || currentRoute == "quick_notes" || currentRoute == "tasks" || currentRoute == "trackers") {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (currentRoute == "quotes") {
                                    IconButton(onClick = { navController.navigate("quote_widget_settings") }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Widget Settings",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (currentRoute == "archive") {
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
                                if (currentRoute == "quick_notes") {
                                    val quickNoteViewModel: com.baverika.r_journal.ui.viewmodel.QuickNoteViewModel = viewModel(
                                        factory = QuickNoteViewModelFactory(quickNoteRepo, context)
                                    )
                                    val layoutType by quickNoteViewModel.layoutType.collectAsState()
                                    IconButton(
                                        onClick = {
                                            val newLayout = if (layoutType == com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY) {
                                                com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_LIST
                                            } else {
                                                com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY
                                            }
                                            quickNoteViewModel.setLayoutType(newLayout)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (layoutType == com.baverika.r_journal.data.local.QuickNotesPreferences.LAYOUT_MASONRY) {
                                                Icons.Default.ViewAgenda
                                            } else {
                                                Icons.Default.GridView
                                            },
                                            contentDescription = "Toggle Layout"
                                        )
                                    }
                                }
                                if (currentRoute == "challenges") {
                                    IconButton(onClick = { navController.navigate("challenge_history") }) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Challenge History",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            },
        bottomBar = {
            val bottomNavItems = listOf(
                MainActivity.BottomNavItem.Journal,
                MainActivity.BottomNavItem.QuickNotes,
                MainActivity.BottomNavItem.Tasks
            )
            val showBottomBar = currentRoute in bottomNavItems.map { it.route }
            
            if (showBottomBar) {
                NavigationBar {
                    val haptic = LocalHapticFeedback.current
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = isSelected,
                            alwaysShowLabel = false,
                            onClick = {
                                if (currentRoute != item.route) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate(item.route) {
                                        popUpTo("archive") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
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
                // Explicitly hide FAB for note creation/editing
                "new_quick_note", "edit_quick_note/{noteId}" -> {
                    fabAction = null
                    fabIcon = null
                    fabDesc = null
                }
                else -> {
                    fabAction = null
                    fabIcon = null
                    fabDesc = null
                }
            }

            var showSpeedDialSheet by remember { mutableStateOf(false) }

            if (showSpeedDialSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSpeedDialSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Quick Action Speed Dial",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Long-press shortcut to create content from anywhere",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val speedDialItems = listOf(
                            Triple("New Journal Entry", Icons.AutoMirrored.Filled.Chat) { navController.navigate("chat_input") },
                            Triple("New Quick Note", Icons.AutoMirrored.Filled.NoteAdd) { navController.navigate("new_quick_note") },
                            Triple("New Task", Icons.Default.CheckCircle) { navController.navigate("add_task") },
                            Triple("New Tracker", Icons.Default.BarChart) { navController.navigate("add_tracker") },
                            Triple("Log Craving", Icons.Default.Add) { navController.navigate("add_craving") },
                            Triple("New Habit", Icons.Default.Add) { navController.navigate("add_habit") }
                        )

                        speedDialItems.forEach { (label, icon, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showSpeedDialSheet = false
                                        action()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            val haptic = LocalHapticFeedback.current

            if (fabAction != null && fabIcon != null) {
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .combinedClickable(
                            onClick = fabAction,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSpeedDialSheet = true
                            }
                        ),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(fabIcon, contentDescription = fabDesc, modifier = Modifier.size(36.dp))
                    }
                }
            }
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
                            taskRepo = taskRepo,
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
                            journalRepo = journalRepo,
                            taskRepo = taskRepo,
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

@Composable
fun DrawerContent(
    currentRoute: String?,
    onScreenSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Section: Reference
        Text(
            text = "Reference",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)
        )
        DrawerItem(
            icon = Icons.Filled.FormatQuote,
            label = "Quotes",
            isSelected = currentRoute == "quotes",
            onClick = { onScreenSelected("quotes") }
        )
        DrawerItem(
            icon = Icons.Filled.BarChart,
            label = "Dashboard",
            isSelected = currentRoute == "dashboard",
            onClick = { onScreenSelected("dashboard") }
        )
        DrawerItem(
            icon = Icons.Filled.FitnessCenter,
            label = "Craving Quest",
            isSelected = currentRoute == "craving_quest",
            onClick = { onScreenSelected("craving_quest") }
        )
        DrawerItem(
            icon = Icons.Filled.CalendarMonth,
            label = "Calendar",
            isSelected = currentRoute == "calendar",
            onClick = { onScreenSelected("calendar") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Utilities
        Text(
            text = "Utilities",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)
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
            icon = Icons.Filled.DateRange, 
            label = "Life Tracker",
            isSelected = currentRoute == "life_trackers",
            onClick = { onScreenSelected("life_trackers") }
        )
        DrawerItem(
            icon = Icons.Filled.AddCircle,
            label = "Trackers",
            isSelected = currentRoute == "trackers",
            onClick = { onScreenSelected("trackers") }
        )
        DrawerItem(
            icon = Icons.Filled.CheckCircle,
            label = "Habits",
            isSelected = currentRoute == "habits",
            onClick = { onScreenSelected("habits") }
        )
        DrawerItem(
            icon = Icons.Filled.Star,
            label = "Challenge Tracker",
            isSelected = currentRoute == "challenges",
            onClick = { onScreenSelected("challenges") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Personal
        Text(
            text = "Personal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)
        )
        DrawerItem(
            icon = Icons.Filled.FolderSpecial,
            label = "Mine",
            isSelected = currentRoute == "chat_input/mine",
            onClick = { onScreenSelected("chat_input/mine") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: System
        Text(
            text = "System",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)
        )
        DrawerItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            isSelected = currentRoute == "settings",
            onClick = { onScreenSelected("settings") }
        )

        Spacer(modifier = Modifier.height(16.dp))

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


