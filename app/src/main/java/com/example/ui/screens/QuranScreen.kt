package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PriyoAzanApp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.SurahInfo
import com.example.ui.theme.AmiriArabicFamily
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.viewmodel.PriyoAzanUiState

@Composable
fun QuranScreen(
    state: PriyoAzanUiState,
    onToggleBookmark: (Int, Int, String, String) -> Unit,
    onSaveProgress: (Int, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val quranRepo = (LocalContext.current.applicationContext as PriyoAzanApp).quranRepository

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }
    var showOfflineInfoDialog by remember { mutableStateOf(false) }

    if (selectedSurah != null) {
        val surah = selectedSurah!!
        BackHandler { selectedSurah = null }

        val ayahs = remember(surah.number) { quranRepo.getAyahsForSurah(surah.number) }
        val surahTitle = if (lang == AppLanguage.BENGALI) surah.bengaliName else surah.englishName

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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedSurah = null },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("quran_reader_back")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close)
                            }
                            Column {
                                Text(
                                    text = "${surah.number}. $surahTitle",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${surah.arabicName} • ${AppStrings.localizeDigits(surah.ayahCount.toString(), lang)} ${strings.ayahCountSuffix}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (ayahs.isNotEmpty() && surah.number != 1 && surah.number != 9) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontFamily = AmiriArabicFamily,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                if (ayahs.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(surah.arabicName, style = MaterialTheme.typography.headlineMedium, fontFamily = AmiriArabicFamily)
                                Text(strings.offlineImportNotice, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                } else {
                    items(ayahs, key = { "${it.surahNumber}:${it.ayahNumber}" }) { ayah ->
                        val isBookmarked = state.bookmarks.any { it.id == "${ayah.surahNumber}:${ayah.ayahNumber}" }
                        val isLastRead = state.quranProgress?.surahNumber == ayah.surahNumber &&
                            state.quranProgress.ayahNumber == ayah.ayahNumber
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                width = if (isLastRead) 1.5.dp else 1.dp,
                                color = if (isLastRead) MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = AppStrings.localizeDigits("${ayah.surahNumber}:${ayah.ayahNumber}", lang),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(
                                            onClick = { onSaveProgress(ayah.surahNumber, ayah.ayahNumber, surahTitle) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            if (isLastRead) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = strings.markLastRead,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(strings.markLastRead, style = MaterialTheme.typography.labelMedium)
                                        }
                                        IconButton(
                                            onClick = {
                                                onToggleBookmark(
                                                    ayah.surahNumber,
                                                    ayah.ayahNumber,
                                                    surahTitle,
                                                    ayah.arabicText
                                                )
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("quran_bookmark_${ayah.surahNumber}_${ayah.ayahNumber}")
                                        ) {
                                            Icon(
                                                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                                contentDescription = strings.tabBookmarks,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = ayah.arabicText,
                                        fontFamily = AmiriArabicFamily,
                                        fontSize = 25.sp,
                                        lineHeight = 42.sp,
                                        textAlign = TextAlign.Start,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val translation = when (lang) {
                                    AppLanguage.BENGALI -> ayah.bengaliTranslation
                                    AppLanguage.ENGLISH -> ayah.englishTranslation
                                    AppLanguage.ARABIC -> ayah.englishTranslation
                                }
                                if (lang != AppLanguage.ARABIC && translation.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = translation,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val (matchedSurahs, matchedAyahs) = remember(searchQuery, state.surahs) {
        quranRepo.search(searchQuery)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.quranTitle, style = MaterialTheme.typography.headlineMedium)
                    IconButton(
                        onClick = { showOfflineInfoDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = strings.aboutPriyoAzan,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchQuranHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = strings.searchQuranHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_search_input")
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.tabSurahs) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.tabBookmarks} (${AppStrings.localizeDigits(state.bookmarks.size.toString(), lang)})") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    if (matchedAyahs.isNotEmpty() && searchQuery.isNotBlank()) {
                        items(matchedAyahs, key = { "match_${it.surahNumber}:${it.ayahNumber}" }) { ayah ->
                            val surah = state.surahs.firstOrNull { it.number == ayah.surahNumber }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (surah != null) selectedSurah = surah },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${surah?.englishName ?: ""} ${ayah.surahNumber}:${ayah.ayahNumber}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(ayah.arabicText, fontFamily = AmiriArabicFamily, fontSize = 18.sp)
                                    Text(ayah.englishTranslation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    items(matchedSurahs, key = { it.number }) { surah ->
                        val hasContent = quranRepo.hasAyahContent(surah.number)
                        val title = if (lang == AppLanguage.BENGALI) surah.bengaliName else surah.englishName
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSurah = surah }
                                .testTag("quran_surah_card_${surah.number}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasContent) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = AppStrings.localizeDigits(surah.number.toString(), lang),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${surah.revelationType} • ${AppStrings.localizeDigits(surah.ayahCount.toString(), lang)} ${strings.ayahCountSuffix}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = surah.arabicName,
                                    fontFamily = AmiriArabicFamily,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    if (state.bookmarks.isEmpty()) {
                        item {
                            Text(
                                text = strings.noBookmarksYet,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(state.bookmarks, key = { it.id }) { bm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSurah = state.surahs.firstOrNull { it.number == bm.surahNumber }
                                    },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${bm.surahName} • ${bm.surahNumber}:${bm.ayahNumber}", style = MaterialTheme.typography.titleMedium)
                                        IconButton(
                                            onClick = { onToggleBookmark(bm.surahNumber, bm.ayahNumber, bm.surahName, bm.arabicSnippet) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.Bookmark, contentDescription = strings.tabBookmarks, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(bm.arabicSnippet, fontFamily = AmiriArabicFamily, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOfflineInfoDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineInfoDialog = false },
            title = { Text(strings.quranTitle) },
            text = { Text(strings.offlineImportNotice) },
            confirmButton = {
                TextButton(onClick = { showOfflineInfoDialog = false }) { Text(strings.close) }
            }
        )
    }
}
