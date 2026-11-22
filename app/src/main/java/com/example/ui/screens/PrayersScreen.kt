package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.database.AzanAudioEntity
import com.example.localization.AppStrings
import com.example.model.CalculationMethod
import com.example.model.PrayerKey
import com.example.model.SoundMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.viewmodel.PriyoAzanUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayersScreen(
    state: PriyoAzanUiState,
    onSelectDate: (LocalDate) -> Unit,
    onUpdatePrayerPref: (PrayerKey, Boolean?, SoundMode?, Int?) -> Unit,
    onPreviewAudio: (AzanAudioEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
    val method = CalculationMethod.fromName(state.settings.calculationMethod)
    val schedule = state.selectedDateSchedule

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSelectDate(state.selectedDate.minusDays(1)) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("prayers_prev_day")
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = strings.prevDay)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.selectedDate.format(dateFormatter),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${state.location?.name ?: ""} • ${strings.methodNames[method] ?: method.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = { onSelectDate(state.selectedDate.plusDays(1)) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("prayers_next_day")
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = strings.nextDay)
                            }
                        }
                        if (state.selectedDate != LocalDate.now()) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = { onSelectDate(LocalDate.now()) }) {
                                    Text(strings.today)
                                }
                            }
                        }
                    }
                }
            }

            state.selectedDateMilestones?.let { m ->
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayers_milestones_card")
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MilestonePill(strings.sehriLabel, AppStrings.localizeDigits(m.sehriEnd.format(timeFormatter), lang))
                            MilestonePill(strings.ishraqLabel, AppStrings.localizeDigits(m.ishraq.format(timeFormatter), lang))
                            MilestonePill(strings.zawalLabel, AppStrings.localizeDigits(m.zawal.format(timeFormatter), lang))
                            MilestonePill(strings.iftarLabel, AppStrings.localizeDigits(m.iftar.format(timeFormatter), lang))
                            MilestonePill(strings.tahajjudLabel, AppStrings.localizeDigits(m.tahajjudStart.format(timeFormatter), lang))
                        }
                    }
                }
            }

            items(schedule?.entries.orEmpty(), key = { it.key.name }) { entry ->
                val pref = state.prayerPreferences[entry.key]
                val enabled = (pref?.azanEnabled == true || pref?.notificationEnabled == true) &&
                    pref.soundMode != SoundMode.MUTED.name
                val currentMode = SoundMode.fromName(pref?.soundMode ?: SoundMode.FULL_AZAN.name)
                val offset = pref?.manualOffsetMinutes ?: 0
                val audioId = if (entry.key == PrayerKey.FAJR) state.settings.fajrAzanAudioId else state.settings.fullAzanAudioId
                val audioObj = state.azanAudioCatalog.firstOrNull { it.id == audioId }
                val isPreviewing = state.previewPlayingId == audioId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayers_card_${entry.key.name.lowercase()}"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.prayerNames[entry.key] ?: entry.key.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (entry.isNext && state.selectedDate == LocalDate.now()) {
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
                                Text(
                                    text = AppStrings.localizeDigits(entry.time.format(timeFormatter), lang),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked ->
                                    onUpdatePrayerPref(
                                        entry.key,
                                        checked,
                                        if (checked && currentMode == SoundMode.MUTED) SoundMode.FULL_AZAN else null,
                                        null
                                    )
                                },
                                modifier = Modifier.testTag("prayers_switch_${entry.key.name.lowercase()}")
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SoundMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = currentMode == mode,
                                    onClick = {
                                        onUpdatePrayerPref(
                                            entry.key,
                                            mode != SoundMode.MUTED,
                                            mode,
                                            null
                                        )
                                    },
                                    label = { Text(strings.soundModes[mode] ?: mode.name) }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${strings.offsetMinutesLabel}: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { onUpdatePrayerPref(entry.key, null, null, (offset - 1).coerceIn(-30, 30)) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "-")
                                }
                                Text(
                                    text = AppStrings.localizeDigits("${if (offset > 0) "+" else ""}$offset m", lang),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                                )
                                IconButton(
                                    onClick = { onUpdatePrayerPref(entry.key, null, null, (offset + 1).coerceIn(-30, 30)) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "+")
                                }
                            }

                            if (audioObj != null && entry.key.isObligatoryAzanPrayer) {
                                OutlinedButton(
                                    onClick = { onPreviewAudio(audioObj) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPreviewing) strings.stopPreview else strings.previewAudio
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isPreviewing) strings.stopPreview else strings.previewAudio)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonePill(label: String, timeText: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
