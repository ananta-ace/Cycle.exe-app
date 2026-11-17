package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.CycleContainer
import com.example.ui.components.CycleBrandBadge
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.SignInDialog
import com.example.ui.components.SplashScreen
import com.example.ui.components.SuperAdminBootstrapBanner
import com.example.ui.components.WelcomeMembershipDialog
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ApplicationSheet
import com.example.ui.screens.CommunityFeedScreen
import com.example.ui.screens.CrewScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SuperAdminDashboardScreen
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.CycleTheme
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LocalCyclePalette
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AppSessionState
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.CrewViewModel
import com.example.ui.viewmodel.FeedViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.MissionsViewModel
import com.example.ui.viewmodel.NotificationsViewModel

private const val ROUTE_HOME = "home"
private const val ROUTE_MISSIONS = "missions"
private const val ROUTE_GALLERY = "gallery"
private const val ROUTE_CREW = "crew"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_ADMIN = "admin"
private const val ROUTE_SUPER_ADMIN = "superadmin"

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MAIN_TABS = listOf(
    BottomNavTab(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab(ROUTE_MISSIONS, "Missions", Icons.Filled.Flag, Icons.Outlined.Flag),
    BottomNavTab(ROUTE_GALLERY, "Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    BottomNavTab(ROUTE_CREW, "Crew", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavTab(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val container = remember { CycleContainer(applicationContext) }
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container))
            val session by appViewModel.sessionState.collectAsStateWithLifecycle()

            CycleTheme(darkTheme = session.isDarkTheme) {
                if (session.showSplash) {
                    SplashScreen()
                } else {
                    LaunchedEffect(Unit) {
                        if (session.user == null) {
                            appViewModel.attemptSilentSignInIfNeeded(this@MainActivity)
                        }
                    }
                    CycleAppShell(
                        activity = this@MainActivity,
                        container = container,
                        session = session,
                        appViewModel = appViewModel,
                        onSignOut = { appViewModel.signOut(this@MainActivity) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleAppShell(
    activity: Activity,
    container: CycleContainer,
    session: AppSessionState,
    appViewModel: AppViewModel,
    onSignOut: () -> Unit
) {
    val palette = LocalCyclePalette.current
    val uid = session.user?.uid.orEmpty()

    // Public-readable ViewModels (collections allow get/list: if true in firestore.rules)
    val feedViewModel: FeedViewModel = viewModel(
        key = "feed",
        factory = FeedViewModel.factory(container)
    )
    val missionsViewModel: MissionsViewModel = viewModel(
        key = "missions",
        factory = MissionsViewModel.factory(container)
    )
    val galleryViewModel: GalleryViewModel = viewModel(
        key = "gallery",
        factory = GalleryViewModel.factory(container)
    )
    val crewViewModel: CrewViewModel = viewModel(
        key = "crew",
        factory = CrewViewModel.factory(container)
    )

    // Strictly auth-gated ViewModels: NEVER instantiated when signed out
    val notificationsViewModel: NotificationsViewModel? = if (session.user != null) {
        viewModel(
            key = "notifs_$uid",
            factory = NotificationsViewModel.factory(container)
        )
    } else {
        null
    }

    val adminViewModel: AdminViewModel? = if (session.user != null && (session.isAdmin || session.isSuperAdmin)) {
        viewModel(
            key = "admin_${uid}_${session.isSuperAdmin}",
            factory = AdminViewModel.factory(container, session.isSuperAdmin)
        )
    } else {
        null
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    var signInDialogOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var applicationSheetOpen by remember { mutableStateOf(false) }

    // Automatically close the Sign-In modal when authentication completes
    LaunchedEffect(session.user) {
        if (session.user != null) {
            signInDialogOpen = false
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.background,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = palette.cardSurface,
                            titleContentColor = palette.textPrimary
                        ),
                        title = {
                            CycleBrandBadge(
                                compact = true,
                                onClick = { navigateTopLevel(ROUTE_HOME) }
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { appViewModel.toggleDarkTheme() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (session.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = if (session.isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                                    tint = palette.textPrimary
                                )
                            }
                            if (session.user != null) {
                                IconButton(
                                    onClick = { notificationsOpen = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("top_notifications_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (session.unreadNotificationsCount > 0) {
                                                Badge(
                                                    containerColor = GoldAccent,
                                                    contentColor = CharcoalInk
                                                ) {
                                                    Text(
                                                        text = session.unreadNotificationsCount.coerceAtMost(99).toString(),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "Notifications",
                                            tint = palette.textPrimary
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        appViewModel.clearSignInError()
                                        signInDialogOpen = true
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldAccent,
                                        contentColor = CharcoalInk
                                    ),
                                    border = BorderStroke(1.5.dp, CharcoalInk),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .testTag("top_sign_in_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(end = 2.dp)
                                    )
                                    Text(
                                        text = "Sign In",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(thickness = 2.dp, color = palette.border)
                }
            },
            bottomBar = {
                if (!useRail && currentRoute in MAIN_TABS.map { it.route }) {
                    Column {
                        HorizontalDivider(thickness = 2.dp, color = palette.border)
                        NavigationBar(
                            containerColor = palette.cardSurface,
                            tonalElevation = 0.dp
                        ) {
                            MAIN_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CharcoalInk,
                                        selectedTextColor = palette.textPrimary,
                                        indicatorColor = GoldAccent,
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = palette.cardSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MAIN_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CharcoalInk,
                                    selectedTextColor = palette.textPrimary,
                                    indicatorColor = GoldAccent,
                                    unselectedIconColor = palette.textSecondary,
                                    unselectedTextColor = palette.textSecondary
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 900.dp)
                            .fillMaxSize()
                    ) {
                        if (session.user != null && session.isSuperAdmin && !session.firestoreSuperAdminSynced && currentRoute != ROUTE_SUPER_ADMIN) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SuperAdminBootstrapBanner(
                                    email = session.user.email,
                                    emailVerified = session.user.isEmailVerified,
                                    profile = session.profile,
                                    firestoreSuperAdminSynced = session.firestoreSuperAdminSynced,
                                    onSyncClick = { appViewModel.syncSuperAdminAccess() }
                                )
                            }
                        }

                        if (session.user != null && session.isSuspended) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.errorBg,
                                border = BorderStroke(2.dp, palette.errorFg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Your account is currently suspended. Read-only access is active; contact a Super Admin via notifications.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.errorFg,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(ROUTE_HOME) {
                                CommunityFeedScreen(
                                    session = session,
                                    viewModel = feedViewModel,
                                    onOpenSignIn = {
                                        appViewModel.clearSignInError()
                                        signInDialogOpen = true
                                    },
                                    onOpenApply = {
                                        if (session.user == null) signInDialogOpen = true
                                        else applicationSheetOpen = true
                                    }
                                )
                            }
                            composable(ROUTE_MISSIONS) {
                                MissionsScreen(
                                    session = session,
                                    viewModel = missionsViewModel
                                )
                            }
                            composable(ROUTE_GALLERY) {
                                GalleryScreen(
                                    session = session,
                                    viewModel = galleryViewModel
                                )
                            }
                            composable(ROUTE_CREW) {
                                CrewScreen(
                                    session = session,
                                    viewModel = crewViewModel,
                                    onOpenSignIn = {
                                        appViewModel.clearSignInError()
                                        signInDialogOpen = true
                                    },
                                    onOpenApplication = {
                                        if (session.user == null) signInDialogOpen = true
                                        else applicationSheetOpen = true
                                    },
                                    onOpenSuperAdmin = {
                                        if (session.isSuperAdmin) {
                                            navController.navigate(ROUTE_SUPER_ADMIN) { launchSingleTop = true }
                                        }
                                    }
                                )
                            }
                            composable(ROUTE_PROFILE) {
                                ProfileScreen(
                                    session = session,
                                    onSignInClick = { appViewModel.signInWithGoogle(activity) },
                                    onBrowserSignInClick = { appViewModel.signInWithGoogleBrowser(activity) },
                                    onUploadAvatar = { uri -> appViewModel.uploadAvatar(uri) },
                                    onSaveProfile = { name, bio, photoUrl, customTitle ->
                                        appViewModel.updateProfile(name, bio, photoUrl, customTitle)
                                    },
                                    onToggleTheme = { appViewModel.toggleDarkTheme() },
                                    onOpenApplication = {
                                        if (session.user == null) signInDialogOpen = true
                                        else applicationSheetOpen = true
                                    },
                                    onOpenAdminDashboard = {
                                        if (session.isAdmin) {
                                            navController.navigate(ROUTE_ADMIN) { launchSingleTop = true }
                                        }
                                    },
                                    onOpenSuperAdminDashboard = {
                                        if (session.isSuperAdmin) {
                                            navController.navigate(ROUTE_SUPER_ADMIN) { launchSingleTop = true }
                                        }
                                    },
                                    onSignOut = onSignOut
                                )
                            }
                            composable(ROUTE_ADMIN) {
                                if (adminViewModel != null && session.isAdmin) {
                                    AdminDashboardScreen(
                                        session = session,
                                        viewModel = adminViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }
                            composable(ROUTE_SUPER_ADMIN) {
                                if (adminViewModel != null && session.isSuperAdmin) {
                                    SuperAdminDashboardScreen(
                                        session = session,
                                        viewModel = adminViewModel,
                                        onSyncSuperAdmin = { appViewModel.syncSuperAdminAccess() },
                                        onBack = { navController.popBackStack() }
                                    )
                                } else {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (session.user != null) {
        WelcomeMembershipDialog(
            uid = uid,
            profile = session.profile,
            isSuperAdmin = session.isSuperAdmin,
            dismissedKeys = session.dismissedWelcomeKeys,
            onDismissWithToken = { token -> appViewModel.dismissWelcomeModal(token) }
        )
    }

    if (notificationsOpen && notificationsViewModel != null && session.user != null) {
        val allNotifications by notificationsViewModel.allNotifications.collectAsStateWithLifecycle()
        val notifBusyId by notificationsViewModel.busyId.collectAsStateWithLifecycle()
        val userNotifications = remember(allNotifications, uid, session.isSuperAdmin, session.isAdmin) {
            notificationsViewModel.filterForUser(
                list = allNotifications,
                uid = uid,
                isSuperAdmin = session.isSuperAdmin,
                isAdmin = session.isAdmin
            )
        }
        NotificationsSheet(
            notifications = userNotifications,
            busyId = notifBusyId,
            onDismiss = { notificationsOpen = false },
            onMarkRead = { notif -> notificationsViewModel.markAsRead(notif) },
            onMarkAllRead = { notificationsViewModel.markAllRead(userNotifications) },
            onSendReply = { notice, text, onClear ->
                val senderName = session.profile?.displayName?.ifBlank { null }
                    ?: session.user.displayName
                    ?: "Cyclist"
                val senderPhoto = session.profile?.photoURL?.ifBlank { null }
                    ?: session.user.photoUrl?.toString()
                    ?: ""
                notificationsViewModel.sendReply(
                    notice = notice,
                    senderId = uid,
                    senderName = senderName,
                    senderEmail = session.user.email.orEmpty(),
                    senderRole = session.role.key,
                    senderPhotoURL = senderPhoto,
                    text = text,
                    isSuperAdmin = session.isSuperAdmin,
                    onSuccess = onClear
                )
            },
            onNavigateFromNotification = { notif ->
                notificationsOpen = false
                val link = notif.link.lowercase()
                when {
                    link.contains("superadmin") && session.isSuperAdmin ->
                        navController.navigate(ROUTE_SUPER_ADMIN) { launchSingleTop = true }
                    link.contains("admin") && session.isAdmin ->
                        navController.navigate(ROUTE_ADMIN) { launchSingleTop = true }
                    link.contains("mission") -> navigateTopLevel(ROUTE_MISSIONS)
                    link.contains("gallery") -> navigateTopLevel(ROUTE_GALLERY)
                    link.contains("crew") || link.contains("member") -> navigateTopLevel(ROUTE_CREW)
                    else -> navigateTopLevel(ROUTE_HOME)
                }
            }
        )
    }

    if (applicationSheetOpen && session.user != null) {
        ApplicationSheet(
            session = session,
            viewModel = crewViewModel,
            onDismiss = { applicationSheetOpen = false }
        )
    }

    if (signInDialogOpen && session.user == null) {
        SignInDialog(
            isBusy = session.authBusy,
            errorMessage = session.signInError,
            noticeMessage = session.signInNotice,
            onSignInClick = { appViewModel.signInWithGoogle(activity) },
            onBrowserSignInClick = { appViewModel.signInWithGoogleBrowser(activity) },
            onDismissError = { appViewModel.clearSignInError() },
            onDismiss = { signInDialogOpen = false }
        )
    }
}
