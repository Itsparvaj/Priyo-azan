package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.spiritual.MasnoonDua
import com.example.spiritual.SpiritualContent
import com.example.ui.theme.AmiriArabicFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.SoftGoldAccent
import com.example.ui.viewmodel.PriyoAzanUiState
import kotlinx.coroutines.delay

@Composable
fun MoreToolsHubScreen(
    state: PriyoAzanUiState,
    onOpenTasbih: () -> Unit,
    onOpenDuas: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val hijriMonthName = strings.hijriMonths.getOrElse(state.hijriToday.month - 1) { "" }
    val hijriSummary = "${AppStrings.localizeDigits(state.hijriToday.day.toString(), lang)} $hijriMonthName ${AppStrings.localizeDigits(state.hijriToday.year.toString(), lang)}"
    val preset = SpiritualContent.dhikrPresets.getOrElse(state.tasbih.selectedDhikrIndex) { SpiritualContent.dhikrPresets.first() }
    val presetName = if (lang == AppLanguage.BENGALI) preset.titleBn else preset.titleEn

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
                    text = strings.moreToolsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                ToolHubRowCard(
                    icon = Icons.Rounded.TouchApp,
                    title = strings.tasbihTitle,
                    subtitle = "$presetName • ${AppStrings.localizeDigits("${state.tasbih.count}/${state.tasbih.target}", lang)}",
                    onClick = onOpenTasbih,
                    tag = "more_open_tasbih"
                )
            }

            item {
                ToolHubRowCard(
                    icon = Icons.Rounded.Favorite,
                    title = strings.duasTitle,
                    subtitle = "${AppStrings.localizeDigits(SpiritualContent.masnoonDuas.size.toString(), lang)} • Sunnah & Quranic",
                    onClick = onOpenDuas,
                    tag = "more_open_duas"
                )
            }

            item {
                ToolHubRowCard(
                    icon = Icons.Rounded.CalendarMonth,
                    title = strings.calendarTitle,
                    subtitle = hijriSummary,
                    onClick = onOpenCalendar,
                    tag = "more_open_calendar"
                )
            }

            item {
                ToolHubRowCard(
                    icon = Icons.Rounded.Settings,
                    title = strings.navSettings,
                    subtitle = "${state.location?.name ?: ""} • ${strings.settingsAzan}",
                    onClick = onOpenSettings,
                    tag = "more_open_settings"
                )
            }
        }
    }
}

@Composable
private fun ToolHubRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .minimumInteractiveComponentSize()
            .testTag(tag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasbihScreen(
    state: PriyoAzanUiState,
    onBack: () -> Unit,
    onTapTasbih: () -> Unit,
    onResetTasbih: () -> Unit,
    onSelectTarget: (Int) -> Unit,
    onSelectDhikr: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val haptics = LocalHapticFeedback.current
    val preset = SpiritualContent.dhikrPresets.getOrElse(state.tasbih.selectedDhikrIndex) { SpiritualContent.dhikrPresets.first() }
    val progress = (state.tasbih.count.toFloat() / state.tasbih.target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 220),
        label = "tasbihProgress"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("tasbih_back")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close)
                        }
                        Text(strings.tasbihTitle, style = MaterialTheme.typography.headlineMedium)
                    }
                    OutlinedButton(
                        onClick = onResetTasbih,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("tasbih_reset_btn")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = strings.tasbihReset, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.tasbihReset)
                    }
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpiritualContent.dhikrPresets.forEachIndexed { idx, item ->
                        val label = when (lang) {
                            AppLanguage.BENGALI -> item.titleBn
                            AppLanguage.ARABIC -> item.arabic
                            AppLanguage.ENGLISH -> item.titleEn
                        }
                        FilterChip(
                            selected = idx == state.tasbih.selectedDhikrIndex,
                            onClick = { onSelectDhikr(idx) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("tasbih_preset_$idx")
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = preset.arabic,
                                fontFamily = AmiriArabicFamily,
                                fontSize = 30.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (lang == AppLanguage.BENGALI) preset.titleBn else preset.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(22.dp))

                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val finishingRound = state.tasbih.count + 1 >= state.tasbih.target
                                    haptics.performHapticFeedback(
                                        if (finishingRound) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                                    )
                                    onTapTasbih()
                                }
                                .testTag("tasbih_tap_circle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                drawCircle(
                                    color = trackColor,
                                    style = Stroke(width = 24f)
                                )
                                drawArc(
                                    color = primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                                )
                                drawCircle(
                                    color = SoftGoldAccent.copy(alpha = 0.35f),
                                    radius = size.minDimension / 2f - 28f,
                                    style = Stroke(width = 2f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = AppStrings.localizeDigits(state.tasbih.count.toString(), lang),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("tasbih_count_text")
                                )
                                Text(
                                    text = "/ ${AppStrings.localizeDigits(state.tasbih.target.toString(), lang)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = strings.tasbihTapHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${strings.tasbihCyclesLabel}: ${AppStrings.localizeDigits(state.tasbih.rounds.toString(), lang)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                            listOf(33, 99, 100).forEach { targetVal ->
                                FilterChip(
                                    selected = state.tasbih.target == targetVal,
                                    onClick = { onSelectTarget(targetVal) },
                                    label = { Text(AppStrings.localizeDigits(targetVal.toString(), lang)) }
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
fun DuasScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val clipboard = LocalClipboardManager.current
    var copiedDuaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedDuaId) {
        if (copiedDuaId != null) {
            delay(1800L)
            copiedDuaId = null
        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("duas_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close)
                    }
                    Text(strings.duasTitle, style = MaterialTheme.typography.headlineMedium)
                }
            }

            itemsIndexed(SpiritualContent.masnoonDuas, key = { _, item -> item.id }) { _, dua ->
                DuaCard(
                    dua = dua,
                    lang = lang,
                    isCopied = copiedDuaId == dua.id,
                    copyLabel = strings.copyDua,
                    copiedLabel = strings.copiedToast,
                    onCopy = {
                        val meaning = if (lang == AppLanguage.BENGALI) dua.meaningBn else dua.meaningEn
                        clipboard.setText(AnnotatedString("${dua.arabic}\n\n${dua.transliteration}\n$meaning\n(${dua.reference})"))
                        copiedDuaId = dua.id
                    }
                )
            }
        }
    }
}

@Composable
private fun DuaCard(
    dua: MasnoonDua,
    lang: AppLanguage,
    isCopied: Boolean,
    copyLabel: String,
    copiedLabel: String,
    onCopy: () -> Unit
) {
    val title = when (lang) {
        AppLanguage.BENGALI -> dua.titleBn
        AppLanguage.ARABIC -> dua.titleAr
        AppLanguage.ENGLISH -> dua.titleEn
    }
    val meaning = if (lang == AppLanguage.BENGALI) dua.meaningBn else dua.meaningEn

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dua_card_${dua.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = dua.reference,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = dua.arabic,
                    fontFamily = AmiriArabicFamily,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = dua.transliteration,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lang != AppLanguage.ARABIC) {
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    AnimatedVisibility(visible = isCopied) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, contentDescription = copiedLabel, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(copiedLabel)
                        }
                    }
                    AnimatedVisibility(visible = !isCopied) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = copyLabel, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(copyLabel)
                        }
                    }
                }
            }
        }
    }
}
