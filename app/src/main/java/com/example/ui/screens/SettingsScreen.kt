package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.database.AzanAudioEntity
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AsrMethod
import com.example.model.CalculationMethod
import com.example.model.CityPreset
import com.example.model.FontScaleOption
import com.example.model.HighLatitudeRule
import com.example.model.ThemeMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.viewmodel.PriyoAzanUiState
import com.example.updater.DownloadProgress

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: PriyoAzanUiState,
    onDetectGps: () -> Unit,
    onSelectCity: (CityPreset) -> Unit,
    onUpdateMethod: (CalculationMethod) -> Unit,
    onUpdateAsr: (AsrMethod) -> Unit,
    onUpdateHighLat: (HighLatitudeRule) -> Unit,
    onSelectAudio: (String, Boolean) -> Unit,
    onPreviewAudio: (AzanAudioEntity) -> Unit,
    onTestAzanVoice: () -> Unit = {},
    onStopAzanService: () -> Unit = {},
    onImportCustomAzan: (android.net.Uri, String) -> Unit = { _, _ -> },
    onRescheduleAlarms: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateFontScale: (FontScaleOption) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current

    var showCityPicker by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) onDetectGps()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        onRescheduleAlarms()
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportCustomAzan(uri, "Custom Mosque Azan")
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close)
                        }
                    }
                    Text(strings.navSettings, style = MaterialTheme.typography.headlineMedium)
                }
            }

            // LOCATION
            item {
                SettingsSectionCard(title = strings.settingsLocation) {
                    val loc = state.location
                    Text(
                        text = loc?.let { "${it.name}${if (it.country.isNotBlank()) ", ${it.country}" else ""} (${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)})" }
                            ?: "-",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = loc?.timeZoneId ?: "",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.locationStatusMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.GpsFixed, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.refreshGps)
                        }
                        OutlinedButton(
                            onClick = { showCityPicker = true },
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.LocationCity, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.chooseCity)
                        }
                    }
                }
            }

            // PRAYER CALCULATION
            item {
                SettingsSectionCard(title = strings.settingsPrayer) {
                    Text(strings.calculationMethodTitle, style = MaterialTheme.typography.titleMedium)
                    val currentMethod = CalculationMethod.fromName(state.settings.calculationMethod)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CalculationMethod.entries.forEach { method ->
                            FilterChip(
                                selected = currentMethod == method,
                                onClick = { onUpdateMethod(method) },
                                label = { Text(strings.methodNames[method] ?: method.name) }
                            )
                        }
                    }

                    Text(strings.asrMethodTitle, style = MaterialTheme.typography.titleMedium)
                    val currentAsr = AsrMethod.fromName(state.settings.asrMethod)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsrMethod.entries.forEach { asr ->
                            FilterChip(
                                selected = currentAsr == asr,
                                onClick = { onUpdateAsr(asr) },
                                label = { Text(strings.asrNames[asr] ?: asr.name) }
                            )
                        }
                    }

                    Text(strings.highLatTitle, style = MaterialTheme.typography.titleMedium)
                    val currentHighLat = HighLatitudeRule.fromName(state.settings.highLatitudeRule)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HighLatitudeRule.entries.forEach { rule ->
                            FilterChip(
                                selected = currentHighLat == rule,
                                onClick = { onUpdateHighLat(rule) },
                                label = { Text(strings.highLatNames[rule] ?: rule.name) }
                            )
                        }
                    }
                }
            }

            // AZAN AUDIO & DIAGNOSTICS
            item {
                SettingsSectionCard(title = strings.settingsAzan) {
                    Text(strings.fullAzanAudio, style = MaterialTheme.typography.titleMedium)
                    state.azanAudioCatalog.forEach { audio ->
                        val selectedFull = state.settings.fullAzanAudioId == audio.id
                        val isPlaying = state.previewPlayingId == audio.id
                        val durationLabel = if (audio.durationSeconds > 0) {
                            val mins = audio.durationSeconds / 60
                            val secs = audio.durationSeconds % 60
                            AppStrings.localizeDigits(String.format("%d:%02d", mins, secs), lang)
                        } else {
                            "MP3"
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedFull) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (selectedFull) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAudio(audio.id, false) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (selectedFull) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = audio.title,
                                        tint = if (selectedFull) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(audio.title, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = durationLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onPreviewAudio(audio) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) strings.stopPreview else strings.previewAudio,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(strings.fajrAzanAudio, style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.azanAudioCatalog.forEach { audio ->
                            FilterChip(
                                selected = state.settings.fajrAzanAudioId == audio.id,
                                onClick = { onSelectAudio(audio.id, true) },
                                label = { Text(audio.title) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onTestAzanVoice,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_test_mosque_azan")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.testAzanVoice)
                        }
                        if (state.isAnyAudioPlaying) {
                            OutlinedButton(
                                onClick = onStopAzanService,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_stop_azan")
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = strings.stopAzan)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.stopAzan)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_import_custom_azan")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.importCustomAzan)
                    }

                    Text(strings.azanDiagnostics, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${strings.scheduledAlarmsCount}: ${AppStrings.localizeDigits(state.azanStatus.scheduledCount.toString(), lang)} • ${state.azanStatus.nextScheduledLabel ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onRescheduleAlarms,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.rescheduleAlarms)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenExactAlarmSettings,
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Alarm, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(strings.openExactAlarmSettings)
                        }
                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(strings.openBatterySettings)
                        }
                    }
                    if (!state.azanStatus.notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                        ) {
                            Text(strings.grantNotifications)
                        }
                    }
                    Text(
                        text = strings.forceStopHonestNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // APPEARANCE & LANGUAGE
            item {
                SettingsSectionCard(title = "${strings.settingsAppearance} & ${strings.settingsLanguage}") {
                    val curTheme = ThemeMode.fromName(state.settings.themeMode)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = curTheme == mode,
                                onClick = { onUpdateTheme(mode) },
                                label = { Text(strings.themeNames[mode] ?: mode.name) }
                            )
                        }
                    }

                    Text(strings.fontSizeLabel, style = MaterialTheme.typography.titleMedium)
                    val curScale = FontScaleOption.fromName(state.settings.fontScaleOption)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FontScaleOption.entries.forEach { opt ->
                            FilterChip(
                                selected = curScale == opt,
                                onClick = { onUpdateFontScale(opt) },
                                label = { Text(strings.fontScaleNames[opt] ?: opt.name) }
                            )
                        }
                    }

                    Text(strings.settingsLanguage, style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = lang == AppLanguage.ENGLISH,
                            onClick = { onUpdateLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                        FilterChip(
                            selected = lang == AppLanguage.BENGALI,
                            onClick = { onUpdateLanguage(AppLanguage.BENGALI) },
                            label = { Text("বাংলা") },
                            modifier = Modifier.testTag("settings_lang_bn")
                        )
                        FilterChip(
                            selected = lang == AppLanguage.ARABIC,
                            onClick = { onUpdateLanguage(AppLanguage.ARABIC) },
                            label = { Text("العربية") },
                            modifier = Modifier.testTag("settings_lang_ar")
                        )
                    }
                }
            }

            // UPDATES & ABOUT
            item {
                SettingsSectionCard(title = strings.settingsUpdates) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = strings.aboutPublisherText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = strings.autoUpdateActiveNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onCheckUpdates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_check_updates")
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.checkUpdatesNow)
                    }

                    state.updateStatusMessage?.let { raw ->
                        val msg = when {
                            raw == "CHECKING" -> strings.updateChecking
                            raw == "NO_INTERNET" -> strings.updateNoInternet
                            raw == "NOT_FOUND" -> strings.updateNotFoundRepo
                            raw.startsWith("UP_TO_DATE:") -> "${strings.updateUpToDate} (${raw.removePrefix("UP_TO_DATE:")})"
                            raw.startsWith("AVAILABLE:") -> "${strings.updateAvailableTitle}: ${raw.removePrefix("AVAILABLE:")}"
                            raw.startsWith("ERROR:") -> raw.removePrefix("ERROR:")
                            else -> raw
                        }
                        val msgColor = if (raw.startsWith("ERROR:")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = msgColor)
                    }

                    state.availableUpdateManifest?.let { manifest ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${strings.updateAvailableTitle}: ${manifest.version}", style = MaterialTheme.typography.titleMedium)
                                if (manifest.releaseNotes.isNotBlank()) {
                                    Text(manifest.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                                }
                                when (val dl = state.downloadProgress) {
                                    is DownloadProgress.InProgress -> {
                                        val frac = if (dl.totalBytes > 0) dl.bytesRead.toFloat() / dl.totalBytes else 0f
                                        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                                    }
                                    is DownloadProgress.VerifiedReady -> {
                                        Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth()) {
                                            Text(strings.installUpdate)
                                        }
                                    }
                                    is DownloadProgress.Failed -> {
                                        Text(dl.reason, color = MaterialTheme.colorScheme.error)
                                        Button(onClick = onDownloadUpdate) { Text(strings.downloadAndVerify) }
                                    }
                                    null -> {
                                        Button(onClick = onDownloadUpdate, modifier = Modifier.fillMaxWidth()) {
                                            Text(strings.downloadAndVerify)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { showPrivacyDialog = true }) {
                            Text(strings.privacyPolicy)
                        }
                        TextButton(onClick = { showAboutDialog = true }) {
                            Text(strings.aboutPriyoAzan)
                        }
                    }
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerSheet(
            onDismiss = { showCityPicker = false },
            onSelectCity = {
                onSelectCity(it)
                showCityPicker = false
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyPolicy) },
            text = { Text(strings.privacySummary) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.aboutPriyoAzan) },
            text = {
                Text("${strings.appName} v${BuildConfig.VERSION_NAME}\n\n${strings.tagline}\n\n${strings.aboutPublisherText}\n\n${strings.offlineImportNotice}")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(strings.close) }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
