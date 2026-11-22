package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.AppLanguage
import com.example.model.FontScaleOption
import com.example.model.SoundMode
import com.example.model.ThemeMode
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DuasScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreToolsHubScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrayersScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PriyoAzanTheme
import com.example.ui.theme.SoftGoldAccent
import com.example.ui.viewmodel.PriyoAzanUiState
import com.example.ui.viewmodel.PriyoAzanViewModel

object PriyoRoutes {
    const val HOME = "home"
    const val PRAYERS = "prayers"
    const val QIBLA = "qibla"
    const val QURAN = "quran"
    const val MORE = "more"
    const val TASBIH = "tasbih"
    const val DUAS = "duas"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
}

private data class NavDestinationItem(
    val route: String,
    val icon: ImageVector,
    val labelProvider: @Composable () -> String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PriyoAzanViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            var splashDone by remember { mutableStateOf(false) }

            val themeMode = ThemeMode.fromName(state.settings.themeMode)
            val language = AppLanguage.fromCode(state.settings.languageCode)
            val fontScale = FontScaleOption.fromName(state.settings.fontScaleOption)

            PriyoAzanTheme(
                themeMode = themeMode,
                language = language,
                fontScaleOption = fontScale
            ) {
                when {
                    !splashDone -> {
                        SplashScreen(onFinished = { splashDone = true })
                    }
                    state.isReady && !state.settings.onboardingCompleted -> {
                        OnboardingScreen(
                            currentLocationLabel = state.location?.name ?: "Dhaka",
                            onDetectGps = { viewModel.detectGpsLocation() },
                            onSelectCity = { viewModel.selectCityPreset(it) },
                            onComplete = { viewModel.completeOnboarding() }
                        )
                    }
                    else -> {
                        PriyoAzanMainScaffold(viewModel = viewModel, state = state)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriyoAzanMainScaffold(
    viewModel: PriyoAzanViewModel,
    state: PriyoAzanUiState
) {
    val strings = LocalAppStrings.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: PriyoRoutes.HOME

    // 5 spacious bottom bar items for phones; extended rail on >=600dp tablets
    val bottomDestinations = listOf(
        NavDestinationItem(PriyoRoutes.HOME, Icons.Rounded.Home) { strings.navHome },
        NavDestinationItem(PriyoRoutes.PRAYERS, Icons.Rounded.Schedule) { strings.navPrayers },
        NavDestinationItem(PriyoRoutes.QIBLA, Icons.Rounded.Explore) { strings.navQibla },
        NavDestinationItem(PriyoRoutes.QURAN, Icons.AutoMirrored.Rounded.MenuBook) { strings.navQuran },
        NavDestinationItem(PriyoRoutes.MORE, Icons.Rounded.GridView) { strings.navMore }
    )

    val railDestinations = listOf(
        NavDestinationItem(PriyoRoutes.HOME, Icons.Rounded.Home) { strings.navHome },
        NavDestinationItem(PriyoRoutes.PRAYERS, Icons.Rounded.Schedule) { strings.navPrayers },
        NavDestinationItem(PriyoRoutes.QIBLA, Icons.Rounded.Explore) { strings.navQibla },
        NavDestinationItem(PriyoRoutes.QURAN, Icons.AutoMirrored.Rounded.MenuBook) { strings.navQuran },
        NavDestinationItem(PriyoRoutes.CALENDAR, Icons.Rounded.CalendarMonth) { strings.navCalendar },
        NavDestinationItem(PriyoRoutes.MORE, Icons.Rounded.GridView) { strings.navMore },
        NavDestinationItem(PriyoRoutes.SETTINGS, Icons.Rounded.Settings) { strings.navSettings }
    )

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateSubRoute(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    val isMoreGroupSelected = currentRoute in setOf(
        PriyoRoutes.MORE,
        PriyoRoutes.TASBIH,
        PriyoRoutes.DUAS,
        PriyoRoutes.CALENDAR,
        PriyoRoutes.SETTINGS
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        bottomDestinations.forEach { dest ->
                            val label = dest.labelProvider()
                            val selected = if (dest.route == PriyoRoutes.MORE) isMoreGroupSelected else currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
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
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        railDestinations.forEach { dest ->
                            val label = dest.labelProvider()
                            NavigationRailItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = PriyoRoutes.HOME,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(PriyoRoutes.HOME) {
                            HomeScreen(
                                state = state,
                                onRefreshLocation = { viewModel.detectGpsLocation() },
                                onTogglePrayerAzan = { key, enabled ->
                                    viewModel.updatePrayerPreference(
                                        key = key,
                                        azanEnabled = enabled,
                                        notificationEnabled = enabled,
                                        soundMode = if (enabled) SoundMode.FULL_AZAN else SoundMode.MUTED
                                    )
                                },
                                onNavigateTasbih = { navigateSubRoute(PriyoRoutes.TASBIH) },
                                onNavigateDuas = { navigateSubRoute(PriyoRoutes.DUAS) },
                                onNavigateQibla = { navigateTopLevel(PriyoRoutes.QIBLA) },
                                onNavigateQuran = { navigateTopLevel(PriyoRoutes.QURAN) },
                                onNavigateCalendar = { navigateSubRoute(PriyoRoutes.CALENDAR) },
                                onNavigateSettings = { navigateSubRoute(PriyoRoutes.SETTINGS) },
                                onOpenPrayers = { navigateTopLevel(PriyoRoutes.PRAYERS) },
                                onTestAzanVoice = { viewModel.triggerTestAzanVoice() },
                                onStopAzanService = { viewModel.stopAzanService() }
                            )
                        }
                        composable(PriyoRoutes.PRAYERS) {
                            PrayersScreen(
                                state = state,
                                onSelectDate = { viewModel.setSelectedDate(it) },
                                onUpdatePrayerPref = { key, enabled, mode, offset ->
                                    viewModel.updatePrayerPreference(
                                        key = key,
                                        azanEnabled = enabled,
                                        notificationEnabled = enabled,
                                        soundMode = mode,
                                        manualOffsetMinutes = offset
                                    )
                                },
                                onPreviewAudio = { viewModel.previewAudio(it) }
                            )
                        }
                        composable(PriyoRoutes.QIBLA) {
                            QiblaScreen(
                                state = state,
                                onStartCompass = { viewModel.startCompass() },
                                onStopCompass = { viewModel.stopCompass() }
                            )
                        }
                        composable(PriyoRoutes.QURAN) {
                            QuranScreen(
                                state = state,
                                onToggleBookmark = { s, a, name, txt ->
                                    viewModel.toggleBookmark(s, a, name, txt)
                                },
                                onSaveProgress = { s, a, name ->
                                    viewModel.saveQuranProgress(s, a, name)
                                }
                            )
                        }
                        composable(PriyoRoutes.MORE) {
                            MoreToolsHubScreen(
                                state = state,
                                onOpenTasbih = { navigateSubRoute(PriyoRoutes.TASBIH) },
                                onOpenDuas = { navigateSubRoute(PriyoRoutes.DUAS) },
                                onOpenCalendar = { navigateSubRoute(PriyoRoutes.CALENDAR) },
                                onOpenSettings = { navigateSubRoute(PriyoRoutes.SETTINGS) }
                            )
                        }
                        composable(PriyoRoutes.TASBIH) {
                            TasbihScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onTapTasbih = { viewModel.tapTasbih() },
                                onResetTasbih = { viewModel.resetTasbih() },
                                onSelectTarget = { viewModel.setTasbihTarget(it) },
                                onSelectDhikr = { viewModel.selectDhikr(it) }
                            )
                        }
                        composable(PriyoRoutes.DUAS) {
                            DuasScreen(onBack = { navController.popBackStack() })
                        }
                        composable(PriyoRoutes.CALENDAR) {
                            CalendarScreen(
                                state = state,
                                onUpdateHijriOffset = { viewModel.updateHijriOffset(it) }
                            )
                        }
                        composable(PriyoRoutes.SETTINGS) {
                            SettingsScreen(
                                state = state,
                                onDetectGps = { viewModel.detectGpsLocation() },
                                onSelectCity = { viewModel.selectCityPreset(it) },
                                onUpdateMethod = { viewModel.updateCalculationMethod(it) },
                                onUpdateAsr = { viewModel.updateAsrMethod(it) },
                                onUpdateHighLat = { viewModel.updateHighLatitudeRule(it) },
                                onSelectAudio = { id, isFajr -> viewModel.selectDefaultAudio(id, isFajr) },
                                onPreviewAudio = { viewModel.previewAudio(it) },
                                onTestAzanVoice = { viewModel.triggerTestAzanVoice() },
                                onStopAzanService = { viewModel.stopAzanService() },
                                onImportCustomAzan = { uri, title -> viewModel.importCustomAzanUri(uri, title) },
                                onRescheduleAlarms = { viewModel.rescheduleAlarmsNow() },
                                onOpenExactAlarmSettings = { viewModel.openExactAlarmSettings() },
                                onOpenBatterySettings = { viewModel.openBatteryOptimizationSettings() },
                                onUpdateTheme = { viewModel.updateThemeMode(it) },
                                onUpdateLanguage = { viewModel.updateLanguage(it) },
                                onUpdateFontScale = { viewModel.updateFontScale(it) },
                                onUpdateGitHubRepo = { owner, name -> viewModel.updateGitHubRepo(owner, name) },
                                onCheckUpdates = { viewModel.checkForUpdatesManually() },
                                onDownloadUpdate = { viewModel.downloadVerifiedUpdate() },
                                onInstallUpdate = { viewModel.installVerifiedApk() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Global Floating Now Playing Mosque Azan Mini-Bar
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isAnyAudioPlaying,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                                .testTag("global_azan_playing_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription = strings.playingAzanNow,
                                        tint = SoftGoldAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = strings.playingAzanNow,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        state.playingAudioLabel?.let { label ->
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SoftGoldAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.stopAzanService() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("global_stop_azan_btn")
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = strings.stopAzan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.stopAzan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
