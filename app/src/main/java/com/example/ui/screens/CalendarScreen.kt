package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calculations.HijriCalendarEngine
import com.example.localization.AppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.SoftGoldAccent
import com.example.ui.viewmodel.PriyoAzanUiState

@Composable
fun CalendarScreen(
    state: PriyoAzanUiState,
    onUpdateHijriOffset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    var displayMonth by remember(state.hijriToday.month) { mutableIntStateOf(state.hijriToday.month) }
    var displayYear by remember(state.hijriToday.year) { mutableIntStateOf(state.hijriToday.year) }

    val cells = remember(displayYear, displayMonth, state.settings.hijriOffsetDays) {
        HijriCalendarEngine.buildHijriMonthGrid(
            hijriYear = displayYear,
            hijriMonth = displayMonth,
            offsetDays = state.settings.hijriOffsetDays
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = strings.calendarTitle,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Ramadan & Hijri Today Banner
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val monthName = strings.hijriMonths.getOrElse(state.hijriToday.month - 1) { "" }
                            Text(
                                text = "${AppStrings.localizeDigits(state.hijriToday.day.toString(), lang)} $monthName ${AppStrings.localizeDigits(state.hijriToday.year.toString(), lang)} AH",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = state.hijriToday.gregorianIso,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (state.hijriToday.isRamadan) strings.ramadanActive
                                else "${AppStrings.localizeDigits(state.daysToRamadan.toString(), lang)}d • ${strings.daysToRamadan}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Month Navigation + 7-Column Grid
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (displayMonth == 1) {
                                        displayMonth = 12
                                        displayYear -= 1
                                    } else {
                                        displayMonth -= 1
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("calendar_prev_month")
                            ) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = strings.prevDay)
                            }

                            Text(
                                text = "${strings.hijriMonths.getOrElse(displayMonth - 1) { "" }} ${AppStrings.localizeDigits(displayYear.toString(), lang)}",
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(
                                onClick = {
                                    if (displayMonth == 12) {
                                        displayMonth = 1
                                        displayYear += 1
                                    } else {
                                        displayMonth += 1
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("calendar_next_month")
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = strings.nextDay)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val leadingBlanks = if (cells.isNotEmpty()) {
                            // Day of week: Mon=1..Sun=7 -> shift so Sun=0..Sat=6
                            cells.first().gregorianDate.dayOfWeek.value % 7
                        } else 0
                        val totalSlots = leadingBlanks + cells.size
                        val rows = (totalSlots + 6) / 7

                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val idx = r * 7 + c - leadingBlanks
                                    if (idx in cells.indices) {
                                        val cell = cells[idx]
                                        val bg = when {
                                            cell.isToday -> MaterialTheme.colorScheme.primary
                                            cell.holidayKey != null -> SoftGoldAccent.copy(alpha = 0.25f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                        val fg = if (cell.isToday) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.95f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(bg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = AppStrings.localizeDigits(cell.hijri.day.toString(), lang),
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontFamily = JetBrainsMonoFamily,
                                                        fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = fg
                                                )
                                                Text(
                                                    text = "${cell.gregorianDate.dayOfMonth}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = fg.copy(alpha = 0.65f)
                                                )
                                                if (cell.holidayKey != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(SoftGoldAccent)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hijri Day Offset Control
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.hijriAdjustment, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateHijriOffset(state.settings.hijriOffsetDays - 1) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-")
                            }
                            Text(
                                text = AppStrings.localizeDigits(
                                    "${if (state.settings.hijriOffsetDays > 0) "+" else ""}${state.settings.hijriOffsetDays}",
                                    lang
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                            )
                            IconButton(
                                onClick = { onUpdateHijriOffset(state.settings.hijriOffsetDays + 1) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "+")
                            }
                        }
                    }
                }
            }

            // Moon sighting note
            item {
                Text(
                    text = strings.moonSightingNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }

            item {
                Text(
                    text = strings.keyIslamicDates,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(HijriCalendarEngine.importantDates, key = { it.key }) { holiday ->
                val mName = strings.hijriMonths.getOrElse(holiday.hijriMonth - 1) { "" }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.holidayNames[holiday.key] ?: holiday.key,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${AppStrings.localizeDigits(holiday.hijriDay.toString(), lang)} $mName",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
