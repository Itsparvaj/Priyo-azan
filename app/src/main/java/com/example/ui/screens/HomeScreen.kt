package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.PrayerKey
import com.example.model.SoundMode
import com.example.ui.theme.AmiriArabicFamily
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientMid
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.PriyoAzanEmblem
import com.example.ui.theme.SoftGoldAccent
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.drawIslamicGeometricLattice
import com.example.ui.viewmodel.PriyoAzanUiState
import com.example.updater.DownloadProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: PriyoAzanUiState,
    onRefreshLocation: () -> Unit,
    onTogglePrayerAzan: (PrayerKey, Boolean) -> Unit,
    onNavigateTasbih: () -> Unit,
    onNavigateDuas: () -> Unit,
    onNavigateQibla: () -> Unit,
    onNavigateQuran: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenPrayers: () -> Unit,
    onTestAzanVoice: () -> Unit = {},
    onStopAzanService: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    val gregDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH))
    val hijriMonthName = strings.hijriMonths.getOrElse(state.hijriToday.month - 1) { "" }
    val hijriFormatted = "${AppStrings.localizeDigits(state.hijriToday.day.toString(), lang)} $hijriMonthName ${AppStrings.localizeDigits(state.hijriToday.year.toString(), lang)}"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Greeting + Location Pill + Quick Settings Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        PriyoAzanEmblem(
                            modifier = Modifier.size(38.dp),
                            starColor = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.greeting,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$hijriFormatted • $gregDateStr",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onRefreshLocation() }
                                .minimumInteractiveComponentSize()
                                .testTag("home_location_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = strings.settingsLocation,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = state.location?.name ?: "Dhaka",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(
                            onClick = onNavigateSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = strings.navSettings,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Serene Hero Next Prayer Card
            item {
                val upcoming = state.upcomingPrayer
                val nextName = upcoming?.let { strings.prayerNames[it.key] } ?: "-"
                val nextTime = upcoming?.time?.format(timeFormatter) ?: "--:--"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPrayers() }
                        .testTag("home_hero_next_prayer_card"),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd)
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawIslamicGeometricLattice(
                                lineColor = SoftGoldAccent.copy(alpha = 0.11f),
                                cellSizePx = 96f
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SoftGoldAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = strings.nextPrayerLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SoftGoldAccent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                                Text(
                                    text = AppStrings.localizeDigits(state.currentTimeFormatted, lang),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = nextName,
                                        style = MaterialTheme.typography.displayLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = AppStrings.localizeDigits(nextTime, lang),
                                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                        color = SoftGoldAccent
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = strings.startsIn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = AppStrings.localizeDigits(state.countdownText, lang),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.testTag("home_countdown_text")
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { state.countdownProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = SoftGoldAccent,
                                trackColor = Color.White.copy(alpha = 0.18f)
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val milestones = state.dailyMilestones
                                val alarmReady = state.azanStatus.exactAlarmAllowed && state.azanStatus.notificationsAllowed
                                Column {
                                    if (milestones != null) {
                                        Text(
                                            text = "${strings.sehriLabel}: ${AppStrings.localizeDigits(milestones.sehriEnd.format(timeFormatter), lang)}  •  ${strings.iftarLabel}: ${AppStrings.localizeDigits(milestones.iftar.format(timeFormatter), lang)}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (alarmReady) StatusSuccess else StatusWarning)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (alarmReady) strings.exactAlarmReady else strings.exactAlarmNeeded,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                val playing = state.isAnyAudioPlaying
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (playing) SoftGoldAccent else Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            if (playing) onStopAzanService() else onTestAzanVoice()
                                        }
                                        .minimumInteractiveComponentSize()
                                        .testTag(if (playing) "home_stop_azan" else "home_play_mosque_azan")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (playing) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = if (playing) strings.stopAzan else strings.testAzanVoice,
                                            tint = if (playing) HeroGradientStart else SoftGoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (playing) strings.stopPreview else strings.previewAudio,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (playing) HeroGradientStart else Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Friendly Daily Tools Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickShortcutTile(
                        icon = Icons.Rounded.TouchApp,
                        label = strings.tasbihShortcut,
                        onClick = onNavigateTasbih,
                        tag = "shortcut_tasbih",
                        modifier = Modifier.weight(1f)
                    )
                    QuickShortcutTile(
                        icon = Icons.Rounded.Favorite,
                        label = strings.duasShortcut,
                        onClick = onNavigateDuas,
                        tag = "shortcut_duas",
                        modifier = Modifier.weight(1f)
                    )
                    QuickShortcutTile(
                        icon = Icons.Rounded.Explore,
                        label = strings.qiblaShortcut,
                        onClick = onNavigateQibla,
                        tag = "shortcut_qibla",
                        modifier = Modifier.weight(1f)
                    )
                    QuickShortcutTile(
                        icon = Icons.Rounded.CalendarMonth,
                        label = strings.calendarShortcut,
                        onClick = onNavigateCalendar,
                        tag = "shortcut_calendar",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Automatic Update Available Banner Card (shown automatically when GitHub Release is detected)
            state.availableUpdateManifest?.let { manifest ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_update_available_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.SystemUpdate,
                                        contentDescription = strings.updateAvailableTitle,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${strings.updateAvailableTitle} • v${manifest.version}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                TextButton(onClick = onDismissUpdate) {
                                    Text(strings.dismissLabel)
                                }
                            }
                            if (manifest.releaseNotes.isNotBlank()) {
                                Text(
                                    text = manifest.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            when (val dl = state.downloadProgress) {
                                is DownloadProgress.InProgress -> {
                                    val frac = if (dl.totalBytes > 0) dl.bytesRead.toFloat() / dl.totalBytes else 0f
                                    LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                                }
                                is DownloadProgress.VerifiedReady -> {
                                    Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()) {
                                        Text(strings.installUpdate)
                                    }
                                }
                                is DownloadProgress.Failed -> {
                                    Text(dl.reason, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                    Button(onClick = onDownloadUpdate, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()) {
                                        Text(strings.downloadAndVerify)
                                    }
                                }
                                null -> {
                                    Button(onClick = onDownloadUpdate, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()) {
                                        Text(strings.downloadAndVerify)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Continue Reading Card if progress exists
            state.quranProgress?.let { progress ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateQuran() }
                            .testTag("home_continue_reading_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = strings.continueReading,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.continueReading,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${progress.surahName} • ${AppStrings.localizeDigits(progress.ayahNumber.toString(), lang)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Quranic Reflection Card
            item {
                val verse = state.dailyVerse
                val meaning = if (lang == AppLanguage.BENGALI) verse.meaningBn else verse.meaningEn
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_daily_verse_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.dailyReflectionTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = verse.arabic,
                                fontFamily = AmiriArabicFamily,
                                fontSize = 21.sp,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (lang != AppLanguage.ARABIC) {
                            Text(
                                text = meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = verse.reference,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Today's Prayer Schedule Heading
            item {
                Text(
                    text = strings.todaysSchedule,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val entries = state.todaySchedule?.entries.orEmpty()
            items(entries, key = { it.key.name }) { entry ->
                val pref = state.prayerPreferences[entry.key]
                val azanOn = (pref?.azanEnabled == true || pref?.notificationEnabled == true) &&
                    pref.soundMode != SoundMode.MUTED.name
                val bgColor by animateColorAsState(
                    targetValue = if (entry.isNext) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    label = "prayerRowBg"
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_prayer_row_${entry.key.name.lowercase()}"),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(18.dp),
                    border = if (entry.isNext) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = if (entry.isNext) 2.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.prayerNames[entry.key] ?: entry.key.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (entry.hasPassed && !entry.isNext) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (entry.isNext) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = strings.nextPrayerLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (entry.key.isObligatoryAzanPrayer) {
                                Text(
                                    text = if (azanOn) strings.azanActive else strings.azanPaused,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = AppStrings.localizeDigits(entry.time.format(timeFormatter), lang),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (entry.isNext) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = { onTogglePrayerAzan(entry.key, !azanOn) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_toggle_azan_${entry.key.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = if (azanOn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                    contentDescription = if (azanOn) strings.azanActive else strings.azanPaused,
                                    tint = if (azanOn) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .minimumInteractiveComponentSize()
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
