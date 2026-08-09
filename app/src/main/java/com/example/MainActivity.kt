package com.example

import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalAppSettings
import com.example.omni.ui.CommandCentreViewModel
import com.example.omni.ui.CommandCentreScreen
import com.example.omni.ui.ProjectBuilderScreen
import com.example.omni.domain.model.SwarmAgentRole
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GlobalState {
    val savedManuals = mutableStateListOf(
        SavedManual("Prius High Voltage Service Manual", listOf("Prius", "High Voltage", "Hybrid"), true),
        SavedManual("Skyline GT-R RB26 Wiring Diagram", listOf("Nissan", "Skyline", "Engine"), true),
        SavedManual("Adrulee Module Guide", listOf("Adrulee", "Accessories"), true)
    )
    var selectedManual by mutableStateOf<SavedManual?>(null)
}

object StorageState {
    var isGoogleDriveConnected by mutableStateOf(true)
    var googleDriveAccount by mutableStateOf("dave.workshop@gmail.com")
    var isGoogleDriveAutoSync by mutableStateOf(true)
    var googleDriveLastSync by mutableStateOf("Synced 5 mins ago")

    var isOneDriveConnected by mutableStateOf(false)
    var oneDriveAccount by mutableStateOf("dave.tech@outlook.com")
    var isOneDriveAutoSync by mutableStateOf(false)
    var oneDriveLastSync by mutableStateOf("Not connected")

    var isDropboxConnected by mutableStateOf(false)
    var dropboxAccount by mutableStateOf("workshop_team@dropbox.com")
    var isDropboxAutoSync by mutableStateOf(false)

    var defaultStorageLocation by mutableStateOf("Internal App Storage")
    var externalFolderPath by mutableStateOf<String?>(null)

    var wifiOnlySync by mutableStateOf(true)
    var autoBackupScans by mutableStateOf(true)
    var encryptCloudBackups by mutableStateOf(true)

    var internalUsedMb by mutableIntStateOf(1420)
    var internalTotalMb by mutableIntStateOf(32000)
    var cloudUsedMb by mutableIntStateOf(4850)
    var cloudTotalMb by mutableIntStateOf(15000)
}

class SavedManual(
    val title: String,
    val tags: List<String>,
    val isEnhanced: Boolean = true,
    initialAiModsEnabled: Boolean = true,
    val localFilePath: String? = null
) {
    var aiModsEnabled by mutableStateOf(initialAiModsEnabled)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WorkshopApp()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object CommandCentre : Screen("command_centre", "Command Centre", Icons.Filled.Psychology)
    object ProjectBuilder : Screen("project_builder", "Project Builder", Icons.Filled.FolderZip)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Library : Screen("library", "Library", Icons.AutoMirrored.Filled.MenuBook)
    object RepairSearch : Screen("repair_search", "Repair Guides", Icons.Filled.Search)
    object WiringDiagrams : Screen("wiring", "Wiring Schematics", Icons.Filled.ElectricalServices)
    object DeepDive : Screen("deep_dive", "Deep Dive", Icons.Filled.Troubleshoot)
    object Storage : Screen("storage", "Storage", Icons.Filled.CloudSync)
    object ManualViewer : Screen("viewer", "Viewer", Icons.Filled.Visibility)
    object VINDecoder : Screen("vin_decoder", "VIN Decoder", Icons.Filled.DirectionsCar)
    object Collaboration : Screen("collab", "Teams", Icons.Filled.Group)
    object AI : Screen("ai", "AI Fault Assist", Icons.Filled.SmartToy)
    object Scanner : Screen("scanner", "Scanner", Icons.Filled.DocumentScanner)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Primary destinations in the bottom bar / rail
    val primaryBottomScreens = listOf(
        Screen.CommandCentre,
        Screen.ProjectBuilder,
        Screen.Dashboard,
        Screen.AI
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 8.dp,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                WorkshopDrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints {
                val isExpanded = maxWidth > 600.dp

                if (isExpanded) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Spacer(Modifier.height(8.dp))
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "All Tools Drawer")
                            }
                            Spacer(Modifier.height(16.dp))
                            primaryBottomScreens.forEach { screen ->
                                NavigationRailItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, maxLines = 1) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            NavigationRailItem(
                                icon = { Icon(Icons.Filled.Apps, contentDescription = "All Tools") },
                                label = { Text("More") },
                                selected = false,
                                onClick = { scope.launch { drawerState.open() } }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AppNavHost(
                                navController = navController,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 8.dp
                            ) {
                                primaryBottomScreens.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = {
                                            Text(
                                                screen.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                                NavigationBarItem(
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Text("10", fontSize = 9.sp)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Filled.Apps, contentDescription = "More Tools")
                                        }
                                    },
                                    label = { Text("More") },
                                    selected = currentRoute !in primaryBottomScreens.map { it.route },
                                    onClick = { scope.launch { drawerState.open() } }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AppNavHost(
                                navController = navController,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkshopDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NOVA-24 WORKSHOP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pro Diagnostic Suite",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onCloseDrawer) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Menu")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "AI Matrix & Search Grounded",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "v24.4",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp)
        ) {
            DrawerCategoryHeader(title = "OMNI SWARM BUILDER")

            DrawerMenuItem(
                screen = Screen.CommandCentre,
                subtitle = "Swarm Agents & Mutation Pipeline",
                isSelected = currentRoute == Screen.CommandCentre.route,
                badgeText = "Swarm",
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.ProjectBuilder,
                subtitle = "Projects, Files & Build Rollback",
                isSelected = currentRoute == Screen.ProjectBuilder.route,
                badgeText = "Builder",
                onNavigate = onNavigate
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            DrawerCategoryHeader(title = "WORKSHOP & DIAGNOSTICS")

            DrawerMenuItem(
                screen = Screen.Dashboard,
                subtitle = "Main Command Center",
                isSelected = currentRoute == Screen.Dashboard.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.AI,
                subtitle = "Grounded Gemini Fault AI",
                isSelected = currentRoute == Screen.AI.route,
                badgeText = "AI",
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.RepairSearch,
                subtitle = "OEM Repair Guides & TSBs",
                isSelected = currentRoute == Screen.RepairSearch.route,
                badgeText = "Live",
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.WiringDiagrams,
                subtitle = "Interactive Wiring & Pinouts",
                isSelected = currentRoute == Screen.WiringDiagrams.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.DeepDive,
                subtitle = "Scope & Signal Telemetry",
                isSelected = currentRoute == Screen.DeepDive.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.VINDecoder,
                subtitle = "Barcode & Spec Lookup",
                isSelected = currentRoute == Screen.VINDecoder.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.Scanner,
                subtitle = "OCR & Fault Code Reader",
                isSelected = currentRoute == Screen.Scanner.route,
                onNavigate = onNavigate
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            DrawerCategoryHeader(title = "SERVICE MANUALS & STORAGE")

            DrawerMenuItem(
                screen = Screen.Library,
                subtitle = "Saved Manuals & Guides",
                isSelected = currentRoute == Screen.Library.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.ManualViewer,
                subtitle = "Interactive Document Viewer",
                isSelected = currentRoute == Screen.ManualViewer.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.Storage,
                subtitle = "Drive, OneDrive & Sync",
                isSelected = currentRoute == Screen.Storage.route,
                badgeText = "Cloud",
                onNavigate = onNavigate
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            DrawerCategoryHeader(title = "WORKSHOP SYSTEM")

            DrawerMenuItem(
                screen = Screen.Collaboration,
                subtitle = "Technician Work Orders & Chat",
                isSelected = currentRoute == Screen.Collaboration.route,
                onNavigate = onNavigate
            )
            DrawerMenuItem(
                screen = Screen.Settings,
                subtitle = "Theme, Font & API Credentials",
                isSelected = currentRoute == Screen.Settings.route,
                onNavigate = onNavigate
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Nova-24 Cyber Diagnostics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Offline Mode OK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }
        }
    }
}

@Composable
private fun DrawerCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    screen: Screen,
    subtitle: String,
    isSelected: Boolean,
    badgeText: String? = null,
    onNavigate: (String) -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                screen.icon,
                contentDescription = screen.title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Column {
                Text(
                    text = screen.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        badge = badgeText?.let {
            {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        selected = isSelected,
        onClick = { onNavigate(screen.route) },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Screen.CommandCentre.route) {
        composable(Screen.CommandCentre.route) {
            val omniVm: CommandCentreViewModel = viewModel()
            CommandCentreScreen(
                viewModel = omniVm,
                onNavigateToProjectBuilder = { navController.navigate(Screen.ProjectBuilder.route) },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.ProjectBuilder.route) {
            val omniVm: CommandCentreViewModel = viewModel()
            ProjectBuilderScreen(
                viewModel = omniVm,
                onNavigateToCommandCentre = { navController.navigate(Screen.CommandCentre.route) },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToViewer = { navController.navigate(Screen.ManualViewer.route) },
                onNavigateToVIN = { navController.navigate(Screen.VINDecoder.route) },
                onNavigateToAI = { navController.navigate(Screen.AI.route) },
                onNavigateToDeepDive = { navController.navigate(Screen.DeepDive.route) },
                onNavigateToWiring = { navController.navigate(Screen.WiringDiagrams.route) },
                onNavigateToRepairSearch = { navController.navigate(Screen.RepairSearch.route) },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable(Screen.Library.route) { LibraryScreen(navController = navController) }
        composable(Screen.RepairSearch.route) { RepairSearchScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.WiringDiagrams.route) { WiringDiagramsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.DeepDive.route) { DeepDiveScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Storage.route) { StorageScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.ManualViewer.route) { ManualViewerScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Collaboration.route) { CollaborationScreen() }
        composable(Screen.AI.route) { AIScreen() }
        composable(Screen.VINDecoder.route) { VINDecoderScreen() }
        composable(Screen.Scanner.route) { ScannerScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToViewer: () -> Unit,
    onNavigateToVIN: () -> Unit,
    onNavigateToAI: () -> Unit,
    onNavigateToDeepDive: () -> Unit = {},
    onNavigateToWiring: () -> Unit = {},
    onNavigateToRepairSearch: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var dtcCodeInput by remember { mutableStateOf("") }
    val aiViewModel: AIChatViewModel = viewModel()

    val recentManuals = listOf(
        "Honda Civic 2018 Engine Manual" to "PDF • 12 MB • V2.1",
        "Yamaha R1 2020 Service Guide" to "Offline • Tagged: Engine",
        "Prius High Voltage Hybrid Battery" to "AI Enhanced • Live Diagnostic"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workshop Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Open Drawer Menu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Printing to connected printer...", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                    IconButton(onClick = onNavigateToAI) {
                        Icon(Icons.Filled.SmartToy, contentDescription = "AI Fault Assist", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToVIN,
                icon = { Icon(Icons.Filled.CenterFocusWeak, contentDescription = "Scan VIN") },
                text = { Text("Scan VIN") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search repair guides, torque specs, TSBs...") },
                    leadingIcon = {
                        IconButton(onClick = onNavigateToRepairSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = onNavigateToRepairSearch) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "Grounded Search", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRepairSearch() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Google Search Grounded Repair Guides",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Live OEM guides, troubleshooting steps, TSBs, recall bulletins & exact torque specs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Open Search",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Build, contentDescription = "AI Fault Scanner", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("AI Vehicle Fault Code & DTC Scanner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Enter an OBD-II Fault Code (e.g. P0300, P0A80, C1256) or describe symptoms for instant Gemini AI diagnostics.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = dtcCodeInput,
                                onValueChange = { dtcCodeInput = it.uppercase() },
                                placeholder = { Text("e.g. P0300 or Engine Misfire") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (dtcCodeInput.isNotBlank()) {
                                        aiViewModel.diagnoseDtcCode(dtcCodeInput)
                                        onNavigateToAI()
                                    }
                                },
                                enabled = dtcCodeInput.isNotBlank()
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Analyze")
                                Spacer(Modifier.width(4.dp))
                                Text("Diagnose")
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Popular Fault Presets:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("P0300 Misfire", "P0A80 Hybrid Battery", "C1256 Brake Press", "P0171 System Lean", "P0420 Catalyst").forEach { chip ->
                                SuggestionChip(
                                    onClick = {
                                        val code = chip.substringBefore(" ")
                                        aiViewModel.diagnoseDtcCode(code)
                                        onNavigateToAI()
                                    },
                                    label = { Text(chip, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToWiring() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ElectricalServices, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Interactive Wiring Schematics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Signal Tracing & DMM Probes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToDeepDive() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Troubleshoot, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Deep Dive Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("CAN Scope & 5-Why Tree", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Manuals",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(recentManuals) { (title, desc) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToViewer() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Open", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VINDecoderScreen() {
    var isDecoding by remember { mutableStateOf(false) }
    var decodedInfo by remember { mutableStateOf<String?>(null) }
    var showUpgrades by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            isDecoding = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI VIN Plate Decoder",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    try {
                        cameraLauncher.launch(null)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Camera preview opened.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (capturedImage != null) {
                Image(
                    bitmap = capturedImage!!.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isDecoding) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Upload Photo",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to capture VIN Plate Photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                try {
                    cameraLauncher.launch(null)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Camera preview triggered.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Capture Image with AI", style = MaterialTheme.typography.titleMedium)
        }

        LaunchedEffect(isDecoding) {
            if (isDecoding) {
                delay(1800)
                decodedInfo = "Vehicle: 1999 Nissan Skyline GT-R (R34)\nEngine: RB26DETT Twin-Turbo\nChassis: BNR34-123456\nColor: Midnight Purple II"
                showUpgrades = true
                isDecoding = false
            }
        }

        if (decodedInfo != null) {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Vehicle Specifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(decodedInfo!!, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
                }
            }
        }

        if (showUpgrades) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Recommended Major Upgrades",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            val upgrades = listOf(
                "N1 Twin Turbos" to "Increases boost pressure for higher top-end power.",
                "HKS Intercooler Piping" to "Improves airflow and cooling efficiency.",
                "Ohlins Road & Track Suspension" to "Superior handling and track performance.",
                "Brembo 6-Piston Big Brake Kit" to "Essential stopping power for high-hp builds."
            )

            upgrades.forEach { (upgrade, desc) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(upgrade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualViewerScreen(onBack: () -> Unit) {
    val manual = GlobalState.selectedManual
    val context = LocalContext.current
    var showAIChat by remember { mutableStateOf(manual?.aiModsEnabled == true) }
    var isSpeaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(manual?.title ?: "Manual Viewer", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Page 1 - Overview & Diagnostics", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Printing to connected printer...", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                    IconButton(onClick = {
                        if (isSpeaking) {
                            tts?.stop()
                            isSpeaking = false
                        } else {
                            tts?.speak(
                                "Reading page contents for ${manual?.title ?: "the manual"}.",
                                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                                null,
                                "TTS_ID"
                            )
                            isSpeaking = true
                        }
                    }) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Reading" else "Read Aloud",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    if (manual?.isEnhanced == true) {
                        IconButton(onClick = { showAIChat = !showAIChat }) {
                            Icon(Icons.Filled.SmartToy, contentDescription = "AI Assistant", tint = if (showAIChat) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        Switch(
                            checked = manual.aiModsEnabled,
                            onCheckedChange = {
                                manual.aiModsEnabled = it
                                if (!it) showAIChat = false
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(2f).fillMaxHeight().background(if (manual?.aiModsEnabled == true) MaterialTheme.colorScheme.surfaceVariant else Color.LightGray), contentAlignment = Alignment.Center) {
                        if (manual?.localFilePath != null) {
                            PdfRendererContent(manual.localFilePath)
                        } else {
                            DiagramContent(manual?.aiModsEnabled == true)
                        }
                    }
                    if (showAIChat && manual?.aiModsEnabled == true) {
                        Card(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp), shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)) {
                            AIChatContent()
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(if (showAIChat && manual?.aiModsEnabled == true) 1.2f else 2f).fillMaxWidth().background(if (manual?.aiModsEnabled == true) MaterialTheme.colorScheme.surfaceVariant else Color.LightGray), contentAlignment = Alignment.Center) {
                        if (manual?.localFilePath != null) {
                            PdfRendererContent(manual.localFilePath)
                        } else {
                            DiagramContent(manual?.aiModsEnabled == true)
                        }
                    }
                    if (showAIChat && manual?.aiModsEnabled == true) {
                        Card(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                            AIChatContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagramContent(isEnhanced: Boolean = false) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Icon(
            Icons.Filled.Image,
            contentDescription = "Diagram",
            modifier = Modifier.size(100.dp),
            tint = if (isEnhanced) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        if (isEnhanced) {
            Text("AI Enhanced Diagram (Colorized & Hotspot Active)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Interactive component pins active. Tap hotspot to inspect sensor status.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "AI Hotspot: High Voltage Inverter Relay Pin 12 - OK", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.ElectricalServices, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Inspect Wiring Hotspot")
            }
        } else {
            Text("Standard Diagram View")
            Text("(Black and white, no interactive pins)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavHostController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var importedFileName by remember { mutableStateOf<String?>(null) }
    var importedFilePath by remember { mutableStateOf<String?>(null) }
    val savedManuals = GlobalState.savedManuals

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            isScanning = true
            scanResult = null

            var name = "Imported Manual"
            var path: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        name = cursor.getString(nameIndex)
                    }
                }

                val ext = if (name.contains(".")) name.substringAfterLast(".").lowercase() else "pdf"
                val file = java.io.File(context.cacheDir, "imported_manual_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                path = file.absolutePath

                if (name.contains(".")) {
                    name = name.substringBeforeLast(".")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            importedFileName = name
            importedFilePath = path

            scope.launch {
                delay(2000)
                isScanning = false
                scanResult = "AI Manual Analysis Complete: Processed '$name'. Text indexed, wiring diagrams colorized, and fault lookup active."
            }
        }
    }

    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Manual Export")
        putExtra(android.content.Intent.EXTRA_TEXT, "Exported manual content from Workshop Copilot.")
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Library") },
                actions = {
                    IconButton(onClick = { navController?.navigate(Screen.Storage.route) }) {
                        Icon(Icons.Filled.CloudSync, contentDescription = "Storage & Cloud Drives")
                    }
                    IconButton(onClick = { fileLauncher.launch("*/*") }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "Import Manual")
                    }
                    IconButton(onClick = {
                        shareLauncher.launch(android.content.Intent.createChooser(shareIntent, "Export via..."))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Export via Bluetooth/WiFi/USB")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController?.navigate(Screen.Scanner.route) },
                icon = { Icon(Icons.Filled.DocumentScanner, "Scan") },
                text = { Text("Digitize Manual") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController?.navigate(Screen.Storage.route) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("On & Off-Device Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            if (StorageState.isGoogleDriveConnected) "Google Drive: ${StorageState.googleDriveLastSync} • SD Storage: ${if (StorageState.externalFolderPath != null) "Active" else "Default"}"
                            else "Cloud drives disconnected • Internal App Storage Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Open Storage", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Library & Categories", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                FilterChip(selected = false, onClick = {}, label = { Text("Engines") })
                FilterChip(selected = false, onClick = {}, label = { Text("Wiring") })
                FilterChip(selected = false, onClick = {}, label = { Text("Car Stereo") })
                FilterChip(selected = false, onClick = {}, label = { Text("Accessories") })
            }
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = { fileLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = "Import Manual")
                Spacer(Modifier.width(8.dp))
                Text("Import Manual (PDF, Docs, Images)", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { shareLauncher.launch(android.content.Intent.createChooser(shareIntent, "Export via...")) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Export Manuals")
                Spacer(Modifier.width(8.dp))
                Text("Export Manuals (Bluetooth, WiFi, USB)", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))
            if (isScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(16.dp))
                        Text("AI is scanning, indexing pages, and colorizing wiring diagrams...", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            } else if (scanResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Result", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(scanResult!!, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Indexed Contents & AI Enhancements:", style = MaterialTheme.typography.titleSmall)
                        Text("1. Wiring Diagram (Color Enhanced)\n2. Engine Specs (Text OCR restored)\n3. Integrated DTC Fault Code Lookup", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                scanResult = null
                                val titleToSave = importedFileName ?: "Newly Imported Manual"
                                val newManual = SavedManual(titleToSave, listOf("Imported", "AI Enhanced", "Service Guide"), true, true, importedFilePath)
                                savedManuals.add(0, newManual)
                                GlobalState.selectedManual = newManual
                                importedFileName = null
                                importedFilePath = null
                                navController?.navigate(Screen.ManualViewer.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save & Open Manual in Viewer")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Saved Manuals", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            savedManuals.forEach { manual ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        GlobalState.selectedManual = manual
                        navController?.navigate(Screen.ManualViewer.route)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Manual", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(manual.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (manual.isEnhanced) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("AI Mods", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Switch(
                                        checked = manual.aiModsEnabled,
                                        onCheckedChange = { manual.aiModsEnabled = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            manual.tags.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.bodySmall) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AIChatContent(viewModel: AIChatViewModel = viewModel()) {
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var useThinking by remember { mutableStateOf(false) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            attachedBitmap = bitmap
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    attachedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Gemini AI Fault Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Deep Thinking", style = MaterialTheme.typography.bodySmall, color = if (useThinking) MaterialTheme.colorScheme.primary else Color.Gray)
                Switch(checked = useThinking, onCheckedChange = { useThinking = it }, modifier = Modifier.scale(0.8f))
            }
        }
        
        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("P0300 Misfire", "P0A80 Hybrid Batt", "C1256 Brake Press", "P0171 Fuel Lean").forEach { chip ->
                AssistChip(
                    onClick = {
                        val code = chip.substringBefore(" ")
                        viewModel.diagnoseDtcCode(code)
                    },
                    label = { Text(chip, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Filled.ElectricalServices, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = "AI",
                            modifier = Modifier.size(24.dp).padding(top = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Column(
                        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (msg.imageBitmap != null) {
                            Image(
                                bitmap = msg.imageBitmap.asImageBitmap(),
                                contentDescription = "Attached Photo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(4.dp))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (msg.isUser) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(12.dp)
                        ) {
                            if (msg.isThinking) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Analyzing fault with Gemini...", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Text(
                                    msg.text,
                                    color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (!msg.isUser && !msg.isThinking && msg.text.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    tts?.stop()
                                    isSpeaking = false
                                } else {
                                    tts?.speak(msg.text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
                                    isSpeaking = true
                                }
                            },
                            modifier = Modifier.size(32.dp).padding(start = 4.dp, top = 4.dp)
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (attachedBitmap != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Image(
                    bitmap = attachedBitmap!!.asImageBitmap(),
                    contentDescription = "Attachment",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text("Photo attached for AI analysis", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { attachedBitmap = null }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Attach Photo", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = {
                    try {
                        cameraLauncher.launch(null)
                    } catch (e: Exception) {
                        photoPickerLauncher.launch("image/*")
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Take Photo", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask or describe fault...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendMessage(inputText, attachedBitmap, useThinking)
                    inputText = ""
                    attachedBitmap = null
                },
                enabled = (inputText.isNotBlank() || attachedBitmap != null) && !isLoading,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun AIScreen() {
    AIChatContent()
}

@Composable
fun CollaborationScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Team Workspaces", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Safety", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Safety Alert Mode Active", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Technician Mike flagged a critical high-voltage warning on the Prius manual.")
            }
        }
    }
}

@Composable
fun ScannerScreen(onBack: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var scannedPages by remember { mutableIntStateOf(0) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            isScanning = true
            scanComplete = false
            scope.launch {
                delay(1500)
                scannedPages++
                isScanning = false
            }
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Digitize Manual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (capturedImage != null && !isScanning && !scanComplete) {
                    Image(
                        bitmap = capturedImage!!.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (isScanning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Scanning page...", color = Color.White)
                    }
                } else if (scanComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, "Done", tint = Color.Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("$scannedPages pages digitized", color = Color.White)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DocumentScanner, "Scanner", tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Align manual page in frame", color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        try {
                            cameraLauncher.launch(null)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Camera preview triggered.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, "Capture")
                    Spacer(Modifier.width(8.dp))
                    Text("Capture Page")
                }

                if (scannedPages > 0) {
                    Button(
                        onClick = {
                            scanComplete = true
                            GlobalState.savedManuals.add(0, SavedManual("Digitized Manual ($scannedPages pages)", listOf("Scans", "AI Processed"), true))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.Save, "Save")
                        Spacer(Modifier.width(8.dp))
                        Text("Save as PDF")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSyncingGoogle by remember { mutableStateOf(false) }
    var isSyncingOneDrive by remember { mutableStateOf(false) }
    var showAddCloudDialog by remember { mutableStateOf(false) }
    var cloudProviderToAdd by remember { mutableStateOf("Google Drive") }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            StorageState.externalFolderPath = uri.path ?: uri.toString()
            StorageState.defaultStorageLocation = "SD Card / External Folder"
            android.widget.Toast.makeText(context, "External SD Card folder set: ${uri.lastPathSegment}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("On & Off-Device Storage", style = MaterialTheme.typography.titleMedium)
                        Text("Local Storage, Google Drive, OneDrive & Dropbox", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Filled.CloudSync, contentDescription = "Storage", modifier = Modifier.padding(start = 16.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSyncingGoogle = true
                        isSyncingOneDrive = StorageState.isOneDriveConnected
                        scope.launch {
                            delay(1800)
                            isSyncingGoogle = false
                            isSyncingOneDrive = false
                            StorageState.googleDriveLastSync = "Synced just now"
                            if (StorageState.isOneDriveConnected) StorageState.oneDriveLastSync = "Synced just now"
                            android.widget.Toast.makeText(context, "All connected cloud drives fully synced!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync All Drives")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Storage Overview", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Internal Device Storage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${(StorageState.internalUsedMb / 1000f).let { "%.2f".format(it) }} GB / ${(StorageState.internalTotalMb / 1000f).let { "%.1f".format(it) }} GB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { StorageState.internalUsedMb.toFloat() / StorageState.internalTotalMb.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cloud Drives Allocation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${(StorageState.cloudUsedMb / 1000f).let { "%.2f".format(it) }} GB / ${(StorageState.cloudTotalMb / 1000f).let { "%.1f".format(it) }} GB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { StorageState.cloudUsedMb.toFloat() / StorageState.cloudTotalMb.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                StorageState.internalUsedMb = maxOf(200, StorageState.internalUsedMb - 340)
                                android.widget.Toast.makeText(context, "Cleared 340 MB of temporary PDF render cache!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Temp Cache (340 MB)")
                        }
                    }
                }
            }

            Text("On-Device Local Storage", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = StorageState.defaultStorageLocation == "Internal App Storage",
                            onClick = { StorageState.defaultStorageLocation = "Internal App Storage" }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Internal App Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("/data/user/0/com.example/files/manuals/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Fastest access. Protected by Android app sandbox.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 48.dp, top = 4.dp))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = StorageState.defaultStorageLocation == "SD Card / External Folder",
                            onClick = {
                                if (StorageState.externalFolderPath == null) {
                                    folderPickerLauncher.launch(null)
                                } else {
                                    StorageState.defaultStorageLocation = "SD Card / External Folder"
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.SdCard, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SD Card / External Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(StorageState.externalFolderPath ?: "No external directory selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.padding(start = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (StorageState.externalFolderPath == null) "Select External SD Folder" else "Change SD Card Path")
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Off-Device Cloud Storage", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddCloudDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Provider", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (StorageState.isGoogleDriveConnected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Google Drive", tint = Color(0xFF4285F4), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (StorageState.isGoogleDriveConnected) "Connected as ${StorageState.googleDriveAccount}" else "Disconnected", style = MaterialTheme.typography.bodySmall, color = if (StorageState.isGoogleDriveConnected) Color(0xFF2E7D32) else Color.Gray)
                        }
                        Switch(
                            checked = StorageState.isGoogleDriveConnected,
                            onCheckedChange = { connected ->
                                StorageState.isGoogleDriveConnected = connected
                                if (connected) {
                                    android.widget.Toast.makeText(context, "Google Drive linked: ${StorageState.googleDriveAccount}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (StorageState.isGoogleDriveConnected) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Auto-Sync Manuals & Annotations", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = StorageState.isGoogleDriveAutoSync,
                                onCheckedChange = { StorageState.isGoogleDriveAutoSync = it }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Cloud Folder: /Google Drive/Workshop Manuals/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Last Sync: ${StorageState.googleDriveLastSync}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    isSyncingGoogle = true
                                    scope.launch {
                                        delay(1500)
                                        isSyncingGoogle = false
                                        StorageState.googleDriveLastSync = "Synced just now"
                                        android.widget.Toast.makeText(context, "Google Drive sync complete: 12 manuals updated", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSyncingGoogle,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncingGoogle) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Syncing...")
                                } else {
                                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sync Now")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    android.widget.Toast.makeText(context, "Restored 3 manuals from Google Drive backup", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (StorageState.isOneDriveConnected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, contentDescription = "OneDrive", tint = Color(0xFF0078D4), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Microsoft OneDrive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (StorageState.isOneDriveConnected) "Connected as ${StorageState.oneDriveAccount}" else "Not Connected", style = MaterialTheme.typography.bodySmall, color = if (StorageState.isOneDriveConnected) Color(0xFF2E7D32) else Color.Gray)
                        }
                        Switch(
                            checked = StorageState.isOneDriveConnected,
                            onCheckedChange = { connected ->
                                StorageState.isOneDriveConnected = connected
                                if (connected) {
                                    android.widget.Toast.makeText(context, "OneDrive connected successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (StorageState.isOneDriveConnected) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Auto-Backup to OneDrive", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = StorageState.isOneDriveAutoSync,
                                onCheckedChange = { StorageState.isOneDriveAutoSync = it }
                            )
                        }
                        Text("Cloud Folder: /OneDrive/Workshop Service Guides/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Last Sync: ${StorageState.oneDriveLastSync}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    isSyncingOneDrive = true
                                    scope.launch {
                                        delay(1500)
                                        isSyncingOneDrive = false
                                        StorageState.oneDriveLastSync = "Synced just now"
                                        android.widget.Toast.makeText(context, "OneDrive backup updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSyncingOneDrive,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncingOneDrive) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Syncing...")
                                } else {
                                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("OneDrive Sync")
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                StorageState.isOneDriveConnected = true
                                android.widget.Toast.makeText(context, "Microsoft OneDrive account linked!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Connect Microsoft OneDrive")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (StorageState.isDropboxConnected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudSync, contentDescription = "Dropbox", tint = Color(0xFF0061FE), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dropbox & Enterprise WebDAV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (StorageState.isDropboxConnected) "Connected as ${StorageState.dropboxAccount}" else "Optional Enterprise Sync", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = StorageState.isDropboxConnected,
                            onCheckedChange = { connected ->
                                StorageState.isDropboxConnected = connected
                                if (connected) {
                                    android.widget.Toast.makeText(context, "Dropbox account linked!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            Text("Sync Preferences & Security", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sync Over Wi-Fi Only", style = MaterialTheme.typography.titleMedium)
                            Text("Prevents downloading large PDFs over cellular data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = StorageState.wifiOnlySync,
                            onCheckedChange = { StorageState.wifiOnlySync = it }
                        )
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Backup Digitized Scans", style = MaterialTheme.typography.titleMedium)
                            Text("Automatically upload camera scans to Google Drive/OneDrive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = StorageState.autoBackupScans,
                            onCheckedChange = { StorageState.autoBackupScans = it }
                        )
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AES-256 Cloud Encryption", style = MaterialTheme.typography.titleMedium)
                            Text("Encrypt manuals before uploading to cloud storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = StorageState.encryptCloudBackups,
                            onCheckedChange = { StorageState.encryptCloudBackups = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddCloudDialog) {
        AlertDialog(
            onDismissRequest = { showAddCloudDialog = false },
            title = { Text("Add Cloud Storage Provider") },
            text = {
                Column {
                    Text("Select cloud service to connect:")
                    Spacer(Modifier.height(12.dp))
                    listOf("Google Drive", "Microsoft OneDrive", "Dropbox", "Custom WebDAV Server").forEach { provider ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cloudProviderToAdd = provider }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = cloudProviderToAdd == provider, onClick = { cloudProviderToAdd = provider })
                            Spacer(Modifier.width(8.dp))
                            Text(provider)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAddCloudDialog = false
                    when (cloudProviderToAdd) {
                        "Google Drive" -> StorageState.isGoogleDriveConnected = true
                        "Microsoft OneDrive" -> StorageState.isOneDriveConnected = true
                        "Dropbox" -> StorageState.isDropboxConnected = true
                        else -> android.widget.Toast.makeText(context, "WebDAV endpoint configured!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    android.widget.Toast.makeText(context, "$cloudProviderToAdd provider connected!", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Connect")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCloudDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(navController: NavHostController? = null) {
    val appSettings = LocalAppSettings.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))

        Text("Storage & Cloud Drives", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { navController?.navigate(Screen.Storage.route) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Filled.CloudSync, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Manage Storage, Google Drive & OneDrive")
        }

        Spacer(Modifier.height(24.dp))

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appSettings.themeMode.value == mode,
                    onClick = { appSettings.themeMode.value = mode },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Font", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFontFamily.entries.forEach { font ->
                FilterChip(
                    selected = appSettings.fontFamily.value == font,
                    onClick = { appSettings.fontFamily.value = font },
                    label = { Text(font.displayName) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepDiveScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("CAN Scope", "ECU Pinouts", "Torque Tree", "5-Why DTC", "Cyber Telemetry")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Deep Dive Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Component & Signal Micro-Inspection Studio", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Filled.Troubleshoot, contentDescription = null, modifier = Modifier.padding(start = 16.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Deep Dive Report exported to PDF", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                                1 -> Icon(Icons.Filled.ElectricalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                2 -> Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                                3 -> Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                else -> Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> CanBusScopeTab()
                    1 -> EcuPinoutTab()
                    2 -> TorqueTreeTab()
                    3 -> FiveWhyDtcTab()
                    4 -> CyberTelemetryTab()
                }
            }
        }
    }
}

@Composable
fun CanBusScopeTab() {
    val context = LocalContext.current
    var baudRate by remember { mutableStateOf("500 kbps") }
    var noiseLevel by remember { mutableFloatStateOf(0.1f) }
    var hasGlitch by remember { mutableStateOf(false) }
    var timeOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            timeOffset += 0.2f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101820))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CAN-BUS High & Low Oscilloscope", color = Color(0xFF00FF66), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Live Differential Voltage Signal Trace", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    Surface(
                        color = if (hasGlitch) Color(0xFFFF3333) else Color(0xFF00FF66),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            if (hasGlitch) "ERR FRAME DETECTED" else "BUS STABLE (OK)",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF080C10), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    val w = size.width
                    val h = size.height
                    val mid = h / 2f

                    for (i in 1..4) {
                        val y = h * (i / 5f)
                        drawLine(
                            color = Color(0xFF00FF66).copy(alpha = 0.15f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                    }

                    val pathHigh = Path()
                    val pathLow = Path()

                    val points = 60
                    val step = w / points

                    for (i in 0..points) {
                        val x = i * step
                        val bit = ((i + timeOffset.toInt()) % 6 < 3)
                        val noise = (kotlin.math.sin((i + timeOffset) * 2.0) * noiseLevel * 15).toFloat()
                        val glitchOffset = if (hasGlitch && i in 25..32) (kotlin.math.sin(i.toDouble()) * 25).toFloat() else 0f

                        val yHigh = if (bit) mid - 40f + noise + glitchOffset else mid - 10f + noise
                        val yLow = if (bit) mid + 40f - noise - glitchOffset else mid + 10f - noise

                        if (i == 0) {
                            pathHigh.moveTo(x, yHigh)
                            pathLow.moveTo(x, yLow)
                        } else {
                            pathHigh.lineTo(x, yHigh)
                            pathLow.lineTo(x, yLow)
                        }
                    }

                    drawPath(pathHigh, color = Color(0xFF00FF66), style = Stroke(width = 3f))
                    drawPath(pathLow, color = Color(0xFF00E5FF), style = Stroke(width = 3f))
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CAN High (Vdiff: +1.0V)", color = Color(0xFF00FF66), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("CAN Low (Vdiff: -1.0V)", color = Color(0xFF00E5FF), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Dominant: 2.0V Vdiff", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Signal Parameters & Glitch Injection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Text("Baud Rate Selection", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("125 kbps", "250 kbps", "500 kbps", "1 Mbps").forEach { rate ->
                        FilterChip(
                            selected = baudRate == rate,
                            onClick = { baudRate = rate },
                            label = { Text(rate) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Interference Noise Level: ${(noiseLevel * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = noiseLevel,
                    onValueChange = { noiseLevel = it },
                    valueRange = 0f..0.5f
                )

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            hasGlitch = !hasGlitch
                            android.widget.Toast.makeText(context, if (hasGlitch) "CAN Error Frame Injected!" else "CAN Bus Cleared", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasGlitch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (hasGlitch) "Clear Glitch" else "Inject Error Frame")
                    }
                }
            }
        }
    }
}

@Composable
fun EcuPinoutTab() {
    val context = LocalContext.current
    var selectedConnector by remember { mutableStateOf("PCM J1 - Engine Harness (34 Pin)") }
    var probedPin by remember { mutableStateOf<String?>(null) }

    val pins = remember {
        listOf(
            Triple("Pin 01", "Mass Air Flow (MAF)", "2.10V - 2.40V"),
            Triple("Pin 02", "Throttle Position Sensor (TPS)", "0.50V - 4.50V"),
            Triple("Pin 03", "Engine Coolant Temp (ECT)", "1.20V - 1.80V"),
            Triple("Pin 04", "Camshaft Position (CMP) Pulse", "0V - 5.0V Square Wave"),
            Triple("Pin 05", "Crankshaft Position (CKP) Pulse", "0V - 5.0V Square Wave"),
            Triple("Pin 06", "Injector #1 Control Gate", "12.0V Pulse Peak"),
            Triple("Pin 07", "O2 Sensor Bank 1 Sensor 1", "0.10V - 0.90V Oscillation"),
            Triple("Pin 08", "CAN High Differential Pulse", "2.50V - 3.50V")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ElectricalServices, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("ECU Connector Pinout & Voltage Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(8.dp))
                Text("Select connector block to view live pin assignments, harness wire colors, and target multimeter readings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("PCM J1 - Engine Harness (34 Pin)", "HV Battery Control Module J3", "ABS / VSC Module Harness B").forEach { conn ->
                FilterChip(
                    selected = selectedConnector == conn,
                    onClick = { selectedConnector = conn },
                    label = { Text(conn) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(selectedConnector, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()

                pins.forEach { (pin, name, spec) ->
                    val isProbed = probedPin == pin
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                probedPin = pin
                                android.widget.Toast.makeText(context, "Probing $pin ($name)... Signal Normal", android.widget.Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isProbed) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$pin • $name", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Target Range: $spec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    probedPin = pin
                                    android.widget.Toast.makeText(context, "Probe result for $pin: PASS (2.28V)", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isProbed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(if (isProbed) "PROBED (OK)" else "PROBE")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TorqueTreeTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Component Assembly Tree & Torque Chart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(8.dp))
                Text("Bolt tightening sequences, fastener sizes, and stage torque values.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        listOf(
            "Cylinder Head Assembly" to listOf(
                "Bolts 1-10 (M11x1.25)" to "Stage 1: 30 Nm • Stage 2: 60 Nm • Stage 3: +90° Angle",
                "Camshaft Cap Fasteners (M8)" to "16 Nm (Crosswise sequence 1->8)",
                "Exhaust Manifold Studs" to "45 Nm (Apply Anti-Seize)"
            ),
            "Hybrid Inverter Module" to listOf(
                "HV Busbar Bolts (Hex 10mm)" to "9.8 Nm (DO NOT OVERTORQUE)",
                "Coolant Line Clamps" to "3.5 Nm",
                "Module Cover Fasteners" to "8.0 Nm"
            )
        ).forEach { (assembly, fasteners) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(assembly, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    fasteners.forEach { (fastener, spec) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(fastener, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(spec, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FiveWhyDtcTab() {
    val context = LocalContext.current
    var selectedCode by remember { mutableStateOf("P0300") }

    val faultData = remember {
        mapOf(
            "P0300" to listOf(
                "Ignition Coil Insulation Breakdown" to 0.42f,
                "Intake Manifold Vacuum Leak" to 0.28f,
                "Fuel Injector Clog or Flow Restriction" to 0.18f,
                "Engine Cylinder Compression Loss" to 0.12f
            ),
            "P0A80" to listOf(
                "Hybrid Cell Module Voltage Delta > 0.3V" to 0.65f,
                "HV Battery Blower Fan Dust Clog" to 0.20f,
                "Battery Smart Unit Sense Wire Corrosion" to 0.15f
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("5-Why DTC Probabilistic Cause Matrix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.height(8.dp))
                Text("Probabilistic root cause decomposition powered by diagnostic telemetry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("P0300", "P0A80").forEach { code ->
                FilterChip(
                    selected = selectedCode == code,
                    onClick = { selectedCode = code },
                    label = { Text(if (code == "P0300") "P0300 (Misfire)" else "P0A80 (HV Battery)") }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Likelihood Breakdown for $selectedCode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                faultData[selectedCode]?.forEach { (cause, probability) ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cause, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${(probability * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { probability },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        android.widget.Toast.makeText(context, "Guided multimeter test procedure loaded!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Guided Test Routine")
                }
            }
        }
    }
}

@Composable
fun CyberTelemetryTab() {
    val context = LocalContext.current
    var isAuditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Terminal, contentDescription = null, tint = Color(0xFF00FF66))
                    Spacer(Modifier.width(8.dp))
                    Text("MatrixCore Cyber-Telemetry Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00FF66))
                }
                Spacer(Modifier.height(12.dp))

                listOf(
                    "MatrixCore Logic Engine" to "ACTIVE (0 ERRORS)",
                    "Devator Mutation Pipeline" to "MUTATION_APPROVED",
                    "Evaluateor Stability Score" to "98.4 / 100 (STABLE)",
                    "Consensus Engine Approval" to "4/4 NODES SYNCED",
                    "MandelaCore Reality Model" to "UI_REALITY_STABLE"
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(value, color = Color(0xFF00E5FF), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        isAuditing = true
                        scope.launch {
                            delay(1200)
                            isAuditing = false
                            android.widget.Toast.makeText(context, "MatrixCore Logic & Evaluateor score verified!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isAuditing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAuditing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Auditing Logic...")
                    } else {
                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Run Consensus Audit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiringDiagramsScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var selectedSystem by remember { mutableStateOf("Engine EFI System") }

    // Interactive Circuit States
    var ignitionOn by remember { mutableStateOf(true) }
    var mainFuseBlown by remember { mutableStateOf(false) }
    var efiFuseBlown by remember { mutableStateOf(false) }
    var badGround by remember { mutableStateOf(false) }
    var dmmActive by remember { mutableStateOf(false) }
    var selectedWire by remember { mutableStateOf<String?>(null) }
    var probedVoltage by remember { mutableStateOf<String?>("Tap wire or pin to test with DMM") }

    // Animated current flow offset
    var flowAnimOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(ignitionOn, mainFuseBlown, efiFuseBlown) {
        while (true) {
            delay(40)
            flowAnimOffset = (flowAnimOffset + 0.15f) % 1.0f
        }
    }

    val isPowered = ignitionOn && !mainFuseBlown && !efiFuseBlown

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Interactive Wiring Schematics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Vector Signal Tracing • Live DMM Probe • Circuit Fault Simulator", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Filled.ElectricalServices, contentDescription = null, modifier = Modifier.padding(start = 16.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(context, "Vector Wiring Diagram saved to gallery", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download Vector Diagram")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Circuit Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("Engine EFI System", "CAN-Bus Network", "HV Battery Interlock", "Starter & Charging").forEach { system ->
                    FilterChip(
                        selected = selectedSystem == system,
                        onClick = {
                            selectedSystem = system
                            selectedWire = null
                            probedVoltage = "Tap wire or pin to test with DMM"
                        },
                        label = { Text(system) },
                        leadingIcon = {
                            if (selectedSystem == system) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            // Interactive DMM Probe Toolbar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (dmmActive) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Speed,
                            contentDescription = null,
                            tint = if (dmmActive) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                if (dmmActive) "DMM PROBE MODE ACTIVE" else "Digital Multimeter (DMM)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (dmmActive) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(probedVoltage ?: "Idle", style = MaterialTheme.typography.bodySmall, color = if (dmmActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = dmmActive,
                        onCheckedChange = {
                            dmmActive = it
                            if (it) {
                                android.widget.Toast.makeText(context, "DMM Probes Ready. Tap any wire or pin on schematic.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Interactive Vector Schematic Canvas Board
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$selectedSystem Schematic",
                            color = Color(0xFF64DFDF),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (isPowered) Color(0xFF00FF66) else Color(0xFFFF3333),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (isPowered) "CIRCUIT POWERED (12.6V)" else "NO POWER / OPEN CIRCUIT",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Vector Canvas Drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Color(0xFF070A13), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw Grid Lines
                            for (i in 1..8) {
                                val x = w * (i / 9f)
                                drawLine(
                                    color = Color(0xFF1E293B),
                                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                                    end = androidx.compose.ui.geometry.Offset(x, h),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                                )
                            }
                            for (i in 1..5) {
                                val y = h * (i / 6f)
                                drawLine(
                                    color = Color(0xFF1E293B),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(w, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                                )
                            }

                            // Wire Paths
                            val wireBatToFuse = Path().apply {
                                moveTo(w * 0.12f, h * 0.3f)
                                lineTo(w * 0.3f, h * 0.3f)
                            }
                            val wireFuseToSw = Path().apply {
                                moveTo(w * 0.3f, h * 0.3f)
                                lineTo(w * 0.48f, h * 0.3f)
                            }
                            val wireSwToRelay = Path().apply {
                                moveTo(w * 0.48f, h * 0.3f)
                                lineTo(w * 0.68f, h * 0.3f)
                            }
                            val wireRelayToEcu = Path().apply {
                                moveTo(w * 0.68f, h * 0.3f)
                                lineTo(w * 0.88f, h * 0.3f)
                            }
                            val wireEcuToGnd = Path().apply {
                                moveTo(w * 0.88f, h * 0.65f)
                                lineTo(w * 0.88f, h * 0.85f)
                            }

                            // Draw Base Wires
                            val batColor = if (!mainFuseBlown) Color(0xFFFF3366) else Color.DarkGray
                            val swColor = if (ignitionOn && !mainFuseBlown) Color(0xFFFFD166) else Color.DarkGray
                            val ecuColor = if (isPowered) Color(0xFF00E5FF) else Color.DarkGray
                            val gndColor = if (!badGround) Color(0xFF8338EC) else Color(0xFFFF9F1C)

                            drawPath(wireBatToFuse, color = batColor, style = Stroke(width = 6f))
                            drawPath(wireFuseToSw, color = batColor, style = Stroke(width = 6f))
                            drawPath(wireSwToRelay, color = swColor, style = Stroke(width = 6f))
                            drawPath(wireRelayToEcu, color = ecuColor, style = Stroke(width = 6f))
                            drawPath(wireEcuToGnd, color = gndColor, style = Stroke(width = 6f))

                            // Animated Particles if Powered
                            if (isPowered) {
                                for (p in 0..4) {
                                    val frac = (flowAnimOffset + p * 0.2f) % 1.0f
                                    val px = w * 0.12f + (w * 0.76f) * frac
                                    drawCircle(
                                        color = Color(0xFF00FF66),
                                        radius = 5f,
                                        center = androidx.compose.ui.geometry.Offset(px, h * 0.3f)
                                    )
                                }
                            }
                        }

                        // Component Overlay Nodes (Clickable Interactive Targets)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. BATTERY NODE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedWire = "BATTERY_B+"
                                    probedVoltage = if (dmmActive) "DMM Reading: 12.62V DC (BAT+ Feed)" else "12V Main Battery (B+ Terminal)"
                                    if (dmmActive) android.widget.Toast.makeText(context, "B+ Battery: 12.62V OK", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3366))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.BatteryFull, contentDescription = null, tint = Color(0xFFFF3366), modifier = Modifier.size(24.dp))
                                        Text("BAT+", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text("12.6V", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // 2. MAIN FUSE NODE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedWire = "FUSE_30A"
                                    probedVoltage = if (dmmActive) {
                                        if (mainFuseBlown) "DMM Reading: 0.00V (Blown Fuse)" else "DMM Reading: 12.58V (Fuse Intact)"
                                    } else "30A Main EFI Fuse Block"
                                }
                            ) {
                                Surface(
                                    color = if (mainFuseBlown) Color(0xFF5A189A) else Color(0xFF005F73),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (mainFuseBlown) Color.Red else Color(0xFF00FF66))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.FlashOn, contentDescription = null, tint = if (mainFuseBlown) Color.Red else Color(0xFF00FF66), modifier = Modifier.size(20.dp))
                                        Text("FUSE 30A", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(if (mainFuseBlown) "BLOWN" else "OK", color = if (mainFuseBlown) Color.Red else Color(0xFF00FF66), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // 3. IGNITION SWITCH NODE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedWire = "IGNITION_SW"
                                    probedVoltage = if (dmmActive) {
                                        if (ignitionOn) "DMM Reading: 12.55V (Switch Closed)" else "DMM Reading: 0.00V (Switch Open)"
                                    } else "Ignition Key Switch"
                                }
                            ) {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (ignitionOn) Color(0xFFFFD166) else Color.Gray)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, tint = if (ignitionOn) Color(0xFFFFD166) else Color.Gray, modifier = Modifier.size(20.dp))
                                        Text("IGN SW", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(if (ignitionOn) "ON" else "OFF", color = if (ignitionOn) Color(0xFFFFD166) else Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // 4. MAIN RELAY NODE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedWire = "MAIN_RELAY"
                                    probedVoltage = if (dmmActive) {
                                        if (isPowered) "DMM Reading: 12.50V Terminal 87 Output" else "DMM Reading: 0.00V (Relay Open)"
                                    } else "Main EFI Power Relay"
                                }
                            ) {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPowered) Color(0xFF00E5FF) else Color.Gray)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.SettingsInputComponent, contentDescription = null, tint = if (isPowered) Color(0xFF00E5FF) else Color.Gray, modifier = Modifier.size(20.dp))
                                        Text("RELAY", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(if (isPowered) "CLOSED" else "OPEN", color = if (isPowered) Color(0xFF00E5FF) else Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // 5. ENGINE ECU & GROUND NODE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedWire = "ECU_J1"
                                    probedVoltage = if (dmmActive) {
                                        if (isPowered && !badGround) "DMM Reading: 12.48V VCC • 0.02V GND (PASS)"
                                        else if (badGround) "DMM Reading: 4.80V GND Drop (HIGH RESISTANCE FAULT!)"
                                        else "DMM Reading: 0.00V VCC (NO POWER)"
                                    } else "PCM / Engine ECU J1 Connector"
                                }
                            ) {
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (badGround) Color(0xFFFF9F1C) else Color(0xFF00FF66))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Memory, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(20.dp))
                                        Text("ECU J1", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(if (badGround) "GND FAULT" else "RUNNING", color = if (badGround) Color(0xFFFF9F1C) else Color(0xFF00FF66), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Circuit Simulation & Fault Injector Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Interactive Fault & Switch Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ignitionOn = !ignitionOn
                                probedVoltage = if (ignitionOn) "Ignition switched ON" else "Ignition switched OFF"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (ignitionOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (ignitionOn) "Ignition: ON" else "Ignition: OFF")
                        }

                        Button(
                            onClick = {
                                mainFuseBlown = !mainFuseBlown
                                probedVoltage = if (mainFuseBlown) "30A EFI Fuse BLOWN!" else "30A Fuse Replaced (OK)"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (mainFuseBlown) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (mainFuseBlown) "Fuse: BLOWN" else "Blow 30A Fuse")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                badGround = !badGround
                                probedVoltage = if (badGround) "Simulated 45Ω Corrosion on Chassis Ground G101!" else "Chassis Ground Cleared (0.1Ω)"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (badGround) Color(0xFFFF9F1C) else MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (badGround) "Ground: HIGH RES" else "Inject Bad Ground")
                        }
                    }
                }
            }

            // Wire DIN/SAE Color Code Key & Pin Reference
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DIN / SAE Wire Color Standards Legend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    listOf(
                        Triple("RED / BLU", "+12V Constant B+ Power Feed", Color(0xFFFF3366)),
                        Triple("YEL / WHT", "+12V Ignition Switched Feed", Color(0xFFFFD166)),
                        Triple("BLU / ORG", "5.0V Sensor Reference Voltage (VREF)", Color(0xFF00E5FF)),
                        Triple("BRN / BLK", "Chassis Ground (G101 / G102)", Color(0xFF8338EC)),
                        Triple("YEL (CAN-H)", "CAN High Bus Signal (2.5V - 3.5V)", Color(0xFF00FF66)),
                        Triple("GRN (CAN-L)", "CAN Low Bus Signal (1.5V - 2.5V)", Color(0xFF00E5FF))
                    ).forEach { (code, desc, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(code, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

