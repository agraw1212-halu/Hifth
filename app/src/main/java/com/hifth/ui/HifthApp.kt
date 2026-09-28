package com.hifth.ui

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hifth.HifthState
import com.hifth.HifthViewModel
import com.hifth.data.Chapter
import com.hifth.data.QuranVerse
import com.hifth.data.QuranWord
import com.hifth.data.Reciter
import com.hifth.data.RecitationRecording
import com.hifth.data.SavedVocabulary
import com.hifth.data.StudyPlan
import com.hifth.data.WeakSpot
import kotlinx.coroutines.delay
import java.io.File
import java.text.DateFormat
import java.util.Date

private enum class AppTab(val label: String) {
    Home("Home"), Read("Read"), Hifz("Hifz"), Quiz("Quiz"), Progress("Progress"), Listen("Listen")
}

private fun resolveWordAudio(path: String): String = when {
    path.startsWith("http") -> path
    path.startsWith("wbw/") -> "https://audio.qurancdn.com/$path"
    else -> "https://verses.quran.com/$path"
}

@Composable
fun HifthApp(viewModel: HifthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val memorized by viewModel.memorized.collectAsStateWithLifecycle(initialValue = emptyList())
    val savedWords by viewModel.savedWords.collectAsStateWithLifecycle(initialValue = emptyList())
    val plans by viewModel.plans.collectAsStateWithLifecycle(initialValue = emptyList())
    val recordings by viewModel.recordings.collectAsStateWithLifecycle(initialValue = emptyList())
    val weakSpots by viewModel.weakSpots.collectAsStateWithLifecycle(initialValue = emptyList())
    val streak by viewModel.studyStreak.collectAsStateWithLifecycle(initialValue = 0)
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var quickQuizMode by remember { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.filterNot { it == AppTab.Listen }.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == AppTab.Quiz) quickQuizMode = true
                            selectedTab = tab
                        },
                        icon = {
                            Icon(
                                when (tab) {
                                    AppTab.Home -> Icons.Default.Home
                                    AppTab.Read -> Icons.Default.MenuBook
                                    AppTab.Hifz -> Icons.Default.Book
                                    AppTab.Quiz -> Icons.Default.Checklist
                                    AppTab.Progress -> Icons.Default.Insights
                                    AppTab.Listen -> Icons.Default.Headphones
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.Home -> HomeScreen(
                state = state,
                memorizedCount = memorized.size,
                vocabularyCount = savedWords.size,
                plans = plans,
                streak = streak,
                onTab = {
                    if (it == AppTab.Quiz) quickQuizMode = true
                    selectedTab = it
                },
                onOpenChapter = { chapterId -> viewModel.loadChapter(chapterId); selectedTab = AppTab.Read },
                modifier = Modifier.padding(padding)
            )
            AppTab.Read -> ReadScreen(state, viewModel, Modifier.padding(padding))
            AppTab.Hifz -> PlanScreen(
                state = state,
                plans = plans,
                viewModel = viewModel,
                onStartPractice = { quick ->
                    quickQuizMode = quick
                    selectedTab = AppTab.Quiz
                },
                onOpenProgress = { selectedTab = AppTab.Progress },
                modifier = Modifier.padding(padding)
            )
            AppTab.Quiz -> PracticeScreen(state, memorized, weakSpots, savedWords, viewModel, quickQuizMode, Modifier.padding(padding))
            AppTab.Progress -> ProgressScreen(memorized, savedWords, plans, streak, viewModel, Modifier.padding(padding))
            AppTab.Listen -> RecordScreen(state, recordings, viewModel, onBack = { selectedTab = AppTab.Home }, modifier = Modifier.padding(padding))
        }
    }

    state.selectedWord?.let { (word, verseKey) ->
        WordDetailsDialog(
            word = word,
            verseKey = verseKey,
            onDismiss = viewModel::dismissWord,
            onSave = viewModel::saveSelectedWord
        )
    }

    state.tafsirVerse?.let { verseKey ->
        var showTafsirMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissTafsir,
            title = {
                Column {
                    Text("Tafsir · $verseKey")
                    if (state.tafsirs.isNotEmpty()) {
                        BoxMenuButton(
                            text = state.tafsirs.firstOrNull { it.id == state.selectedTafsir }?.name ?: "Choose tafsir",
                            expanded = showTafsirMenu,
                            onExpandedChange = { showTafsirMenu = it }
                        ) {
                            state.tafsirs.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source.name) },
                                    onClick = { showTafsirMenu = false; viewModel.selectTafsir(source.id) }
                                )
                            }
                        }
                    }
                }
            },
            text = {
                if (state.tafsirLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        state.tafsirText.orEmpty(),
                        modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissTafsir) { Text("Done") } }
        )
    }
}

@Composable
private fun HomeScreen(
    state: HifthState,
    memorizedCount: Int,
    vocabularyCount: Int,
    plans: List<StudyPlan>,
    streak: Int,
    onTab: (AppTab) -> Unit,
    onOpenChapter: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChapter = state.chapters.firstOrNull { it.id == state.selectedChapter }
    val featuredChapters = listOf(1, 36, 55, 67, 112).mapNotNull { id -> state.chapters.firstOrNull { it.id == id } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ه", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
                Text("Hifth", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(18.dp))
            Text("P E A C E F U L   S T U D Y", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("Hifth", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "وَرَتِّلِ ٱلْقُرْءَانَ تَرْتِيلًا",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl, color = MaterialTheme.colorScheme.primary)
            )
            Text("Recite, hide, type, and listen — a calm companion for Hifz and language.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Streak", "${streak}d", "days", Modifier.weight(1f))
                MetricCard("Memorized", "$memorizedCount", "ayahs", Modifier.weight(1f))
                MetricCard("Vocab", "$vocabularyCount", "words", Modifier.weight(1f))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onTab(AppTab.Read) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("CONTINUE READING", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                        Text(currentChapter?.name ?: "Browse the Quran", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                        Text(currentChapter?.translatedName.orEmpty(), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f))
                    }
                    Text(currentChapter?.nameArabic.orEmpty(), fontSize = 27.sp, color = MaterialTheme.colorScheme.onPrimary, textDirection = TextDirection.Rtl)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("Listen", "Read along", Icons.Default.Headphones, Modifier.weight(1f)) { onTab(AppTab.Listen) }
                    QuickActionCard("Memorize", "Build a set", Icons.Default.Book, Modifier.weight(1f)) { onTab(AppTab.Hifz) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionCard("Queezers", "Test recall", Icons.Default.Checklist, Modifier.weight(1f)) { onTab(AppTab.Quiz) }
                    QuickActionCard("Mushaf", "Surahs & juz", Icons.Default.MenuBook, Modifier.weight(1f)) { onTab(AppTab.Read) }
                }
            }
        }
        if (plans.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current Hifz set", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { onTab(AppTab.Hifz) }) { Text("Open tools") }
                    }
                    Card(Modifier.fillMaxWidth().clickable { onTab(AppTab.Hifz) }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(plans.first().title, fontWeight = FontWeight.SemiBold)
                            Text("${plans.first().verseKeys.size} ayahs · ${plans.first().verseKeys.firstOrNull().orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (featuredChapters.isNotEmpty()) {
            item { Text("Start with a short surah", fontWeight = FontWeight.SemiBold) }
            items(featuredChapters, key = { "featured-${it.id}" }) { chapter ->
                Card(Modifier.fillMaxWidth().clickable { onOpenChapter(chapter.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${chapter.id}",
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(horizontal = 11.dp, vertical = 9.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chapter.name, fontWeight = FontWeight.Medium)
                            Text("${chapter.verseCount} ayahs · ${chapter.translatedName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(chapter.nameArabic, fontSize = 23.sp, color = MaterialTheme.colorScheme.primary, textDirection = TextDirection.Rtl)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReadScreen(state: HifthState, viewModel: HifthViewModel, modifier: Modifier = Modifier) {
    var browseMode by remember { mutableStateOf("Surah") }
    var showChapterMenu by remember { mutableStateOf(false) }
    var showReciterMenu by remember { mutableStateOf(false) }
    var arabicOnly by remember { mutableStateOf(false) }
    var fontSize by remember { mutableIntStateOf(30) }
    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("10") }
    var juzNumber by remember { mutableStateOf("1") }
    var miniReaderVerse by remember { mutableStateOf<QuranVerse?>(null) }
    val audio = remember { AudioController() }
    DisposableEffect(audio) { onDispose { audio.release() } }
    LaunchedEffect(state.selectedChapter, state.selectedReciter, state.verses.map(QuranVerse::chapterId).distinct()) {
        (state.verses.map(QuranVerse::chapterId).distinct() + state.selectedChapter)
            .distinct()
            .forEach(viewModel::loadRecitationFiles)
    }
    LaunchedEffect(audio.isPlaying) {
        while (audio.isPlaying) {
            audio.refreshPosition()
            delay(500)
        }
    }

    val visibleVerses = when (browseMode) {
        "Range" -> {
            val start = rangeStart.toIntOrNull() ?: 1
            val end = rangeEnd.toIntOrNull() ?: Int.MAX_VALUE
            state.verses.filter { it.verseNumber in start..end }
        }
        else -> state.verses
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your Quran, your pace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Read, listen, and explore every ayah.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Surah", "Juz", "Range").forEach { mode ->
                    FilterChip(
                        selected = browseMode == mode,
                        onClick = {
                            if (mode != "Juz" && state.juz != null) viewModel.loadChapter(state.selectedChapter)
                            browseMode = mode
                        },
                        label = { Text(mode) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxMenuButton(
                    text = state.chapters.firstOrNull { it.id == state.selectedChapter }?.let { "${it.id}. ${it.name}" } ?: "Choose surah",
                    expanded = showChapterMenu,
                    onExpandedChange = { showChapterMenu = it }
                ) {
                    state.chapters.forEach { chapter ->
                        DropdownMenuItem(
                            text = { Text("${chapter.id}. ${chapter.name} · ${chapter.nameArabic}") },
                            onClick = {
                                showChapterMenu = false
                                browseMode = "Surah"
                                viewModel.loadChapter(chapter.id)
                            }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(state.chapters.firstOrNull { it.id == state.selectedChapter }?.nameArabic.orEmpty(), fontSize = 22.sp)
            }
            when (browseMode) {
                "Juz" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = juzNumber,
                        onValueChange = { juzNumber = it.filter(Char::isDigit).take(2) },
                        label = { Text("Juz (1–30)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default
                    )
                    Button(onClick = { juzNumber.toIntOrNull()?.takeIf { it in 1..30 }?.let(viewModel::loadJuz) }) { Text("Open juz") }
                }
                "Range" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rangeStart, onValueChange = { rangeStart = it.filter(Char::isDigit).take(4) },
                        label = { Text("From ayah") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = rangeEnd, onValueChange = { rangeEnd = it.filter(Char::isDigit).take(4) },
                        label = { Text("To ayah") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Arabic only", modifier = Modifier.weight(1f))
                        Switch(checked = arabicOnly, onCheckedChange = { arabicOnly = it })
                    }
                    Text("Arabic text size · $fontSize sp", style = MaterialTheme.typography.labelLarge)
                    Slider(value = fontSize.toFloat(), onValueChange = { fontSize = it.toInt() }, valueRange = 24f..42f)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoxMenuButton(
                            text = state.reciters.firstOrNull { it.id == state.selectedReciter }?.name ?: "Select reciter",
                            expanded = showReciterMenu,
                            onExpandedChange = { showReciterMenu = it }
                        ) {
                            state.reciters.forEach { reciter ->
                                DropdownMenuItem(
                                    text = { Text(reciter.name) },
                                    onClick = { showReciterMenu = false; viewModel.selectReciter(reciter.id) }
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.loadRecitationFiles() }) { Text("Reload") }
                    }
                }
            }
        }
        items(visibleVerses, key = { it.key }) { verse ->
            VerseCard(
                verse = verse,
                fontSize = fontSize,
                arabicOnly = arabicOnly,
                selected = verse.key in state.selectedVerses,
                onSelectedChange = { viewModel.togglePlanVerse(verse.key) },
                onWord = { viewModel.showWord(it, verse.key) },
                onOpen = { miniReaderVerse = verse },
                onTafsir = { viewModel.loadTafsir(verse.key) },
                onMemorized = { viewModel.markMemorized(verse.key) },
                onPlay = {
                    state.recitationUrls[verse.key]?.let(audio::play)
                        ?: viewModel.loadRecitationFiles(verse.chapterId)
                }
            )
        }
        item {
            if (state.loading) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            } else if (visibleVerses.isEmpty()) {
                Text(
                    if (state.error != null) "Connect to the internet to sync Quran content for offline reading."
                    else "No ayahs in this range yet. Load the next page or adjust the range.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.hasMore) {
                OutlinedButton(onClick = viewModel::loadMore, modifier = Modifier.fillMaxWidth()) { Text("Load next 50 ayahs") }
            }
        }
        miniReaderVerse?.let { verse ->
            AlertDialog(
                onDismissRequest = { miniReaderVerse = null },
                title = { Text("Ayah ${verse.key}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            verse.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl)
                        )
                        Text(verse.translation)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.loadTafsir(verse.key) }) { Text("Tafsir") }
                            TextButton(onClick = {
                                state.recitationUrls[verse.key]?.let(audio::play) ?: viewModel.loadRecitationFiles(verse.chapterId)
                            }) { Text("Play audio") }
                            TextButton(onClick = { viewModel.markMemorized(verse.key); miniReaderVerse = null }) { Text("Memorized") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { miniReaderVerse = null }) { Text("Close") } }
            )
        }
    }
}

@Composable
private fun VerseCard(
    verse: QuranVerse,
    fontSize: Int,
    arabicOnly: Boolean,
    selected: Boolean,
    onSelectedChange: () -> Unit,
    onWord: (QuranWord) -> Unit,
    onOpen: () -> Unit,
    onTafsir: () -> Unit,
    onMemorized: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(28.dp).background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(verse.verseNumber.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(verse.key, modifier = Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                if (verse.memorizedAt != null) {
                    Text("Memorized · ${formatDate(verse.memorizedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play ayah audio", tint = MaterialTheme.colorScheme.primary)
                }
                Checkbox(checked = selected, onCheckedChange = { onSelectedChange() })
            }
            Text(
                verse.arabic,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.8f).sp,
                    textDirection = TextDirection.Rtl,
                    fontFamily = FontFamily.Serif
                )
            )
            if (verse.words.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Start) {
                    verse.words.asReversed().filter { it.text.isNotBlank() && it.charType != "end" }.forEach { word ->
                        TextButton(onClick = { onWord(word) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text(word.text, fontSize = (fontSize * 0.72f).sp, textDirection = TextDirection.Rtl, fontFamily = FontFamily.Serif)
                        }
                    }
                }
            }
            if (!arabicOnly && verse.translation.isNotBlank()) {
                Text(verse.translation, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onTafsir) { Text("Tafsir") }
                TextButton(onClick = onPlay) { Text("Play") }
                TextButton(onClick = onMemorized) { Text("Mark memorized") }
            }
        }
    }
}

@Composable
private fun WordDetailsDialog(
    word: QuranWord,
    verseKey: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val audio = remember { AudioController() }
    DisposableEffect(audio) { onDispose { audio.release() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                word.text,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 30.sp,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Ayah $verseKey", color = MaterialTheme.colorScheme.onSurfaceVariant)
                DetailLine("Meaning", word.translation.ifBlank { "Not provided by the content source" })
                DetailLine("Transliteration", word.transliteration.ifBlank { "Not provided" })
                DetailLine("Root", word.root.ifBlank { "Not supplied in this verse response" })
                DetailLine("Lemma", word.lemma.ifBlank { "Not supplied in this verse response" })
                DetailLine("Grammar", word.grammar.ifBlank { "Not supplied in this verse response" })
                Text("Morphology fields depend on Quran.com content availability.", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = !word.audioUrl.isNullOrBlank(),
                    onClick = { word.audioUrl?.let(::resolveWordAudio)?.let(audio::play) }
                ) { Text("Listen") }
                TextButton(onClick = onSave) { Text("Save word") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun BoxMenuButton(
    text: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box {
        OutlinedButton(onClick = { onExpandedChange(true) }) { Text(text, maxLines = 1) }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) { content() }
    }
}

@Composable
private fun PracticeScreen(
    state: HifthState,
    memorized: List<QuranVerse>,
    weakSpots: List<WeakSpot>,
    savedWords: List<SavedVocabulary>,
    viewModel: HifthViewModel,
    initialQuickQuiz: Boolean,
    modifier: Modifier = Modifier
) {
    val source = if (state.verses.isNotEmpty()) state.verses else memorized
    var quickQuiz by remember(initialQuickQuiz) { mutableStateOf(initialQuickQuiz) }
    var verseIndex by remember { mutableIntStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    var translationQuiz by remember { mutableStateOf(false) }
    LaunchedEffect(source) {
        verseIndex = 0
        typedAnswer = ""
        checked = false
    }
    val verse = source.getOrNull(verseIndex)
    val prompt = if (translationQuiz) verse?.arabic.orEmpty() else "Recall ayah ${verse?.key.orEmpty()}"
    val expected = if (translationQuiz) verse?.translation.orEmpty() else verse?.arabic.orEmpty()
    val targetWords = expected.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val enteredWords = typedAnswer.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val comparison = compareWords(targetWords, enteredWords)
    val errorPercent = if (targetWords.isEmpty()) 0 else comparison.count { !it } * 100 / maxOf(targetWords.size, enteredWords.size)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Practice & review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Recall, check each word, and revisit the ayahs that need another look.")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !quickQuiz, onClick = { quickQuiz = false }, label = { Text("Recall") })
                    FilterChip(selected = quickQuiz, onClick = { quickQuiz = true }, label = { Text("Quick quiz") })
                }
                if (!quickQuiz) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !translationQuiz, onClick = { translationQuiz = false; checked = false; typedAnswer = "" }, label = { Text("Hifz") })
                        FilterChip(selected = translationQuiz, onClick = { translationQuiz = true; checked = false; typedAnswer = "" }, label = { Text("Translation") })
                    }
                }
            }
        }
        if (quickQuiz) {
            item { PracticeQuizScreen(source, savedWords, viewModel::recordPracticeAttempt) }
        } else if (verse == null) {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Load Quran ayahs to begin practicing.", fontWeight = FontWeight.SemiBold)
                        Text("Your fetched ayahs are available offline; open a surah from Read to sync more.")
                    }
                }
            }
        } else {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(prompt, style = MaterialTheme.typography.titleMedium)
                        if (reveal || translationQuiz) {
                            Text(
                                if (translationQuiz) verse.arabic else verse.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl)
                            )
                        }
                        if (reveal) Text(verse.translation)
                        if (!checked) {
                            OutlinedTextField(
                                value = typedAnswer,
                                onValueChange = { typedAnswer = it },
                                label = { Text(if (translationQuiz) "Type the meaning" else "Type the ayah") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (translationQuiz) TextDirection.Ltr else TextDirection.Rtl
                                )
                            )
                        } else {
                            Text(buildAnnotatedString {
                                targetWords.forEachIndexed { index, target ->
                                    val correct = comparison.getOrElse(index) { false }
                                    pushStyle(SpanStyle(color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error))
                                    append(target)
                                    pop()
                                    append(" ")
                                }
                                enteredWords.drop(targetWords.size).forEach { extra ->
                                    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.error))
                                    append("+$extra ")
                                    pop()
                                }
                            }, style = MaterialTheme.typography.bodyLarge)
                            Text("Errors: $errorPercent% · ${comparison.count { it }} of ${targetWords.size} words correct")
                            if (errorPercent > 0) {
                                val corrections = targetWords.indices.filterNot { comparison.getOrElse(it) { false } }
                                    .mapNotNull { index -> targetWords.getOrNull(index)?.let { expectedWord -> "${enteredWords.getOrNull(index) ?: "—"} → $expectedWord" } }
                                Text("Corrections: ${corrections.joinToString("  ")}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!checked) {
                                Button(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide ayah" else "Reveal ayah") }
                                Button(
                                    enabled = typedAnswer.isNotBlank() && targetWords.isNotEmpty(),
                                    onClick = {
                                        checked = true
                                        viewModel.recordPracticeAttempt(verse.key, errorPercent)
                                        if (errorPercent == 0 && !translationQuiz) viewModel.markMemorized(verse.key)
                                    }
                                ) { Text("Check") }
                            } else {
                                Button(onClick = {
                                    verseIndex = (verseIndex + 1) % source.size
                                    typedAnswer = ""
                                    checked = false
                                    reveal = false
                                }) { Text("Next ayah") }
                                TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide" else "Show ayah") }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Weak spots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val weakest = weakSpots.filter { it.errorPoints > 0 }.sortedByDescending(WeakSpot::errorPoints).take(5)
            if (weakest.isEmpty()) Text("Missed ayahs will appear here as you practice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            weakest.forEach { item ->
                Text("Ayah ${item.verseKey} · ${item.errorPoints} accumulated error points in ${item.attempts} attempts", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PlanScreen(
    state: HifthState,
    plans: List<StudyPlan>,
    viewModel: HifthViewModel,
    onStartPractice: (quickQuiz: Boolean) -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectionText by remember { mutableStateOf("") }
    var selectedChapterId by remember { mutableIntStateOf(state.selectedChapter) }
    var showChapterMenu by remember { mutableStateOf(false) }
    val selectedChapter = state.chapters.firstOrNull { it.id == selectedChapterId }
    val parsedSelection = remember(selectionText, state.chapters) { parseVerseSelection(selectionText, state.chapters) }
    val selectedKeys = remember(parsedSelection.verseKeys, state.selectedVerses) {
        (parsedSelection.verseKeys + state.selectedVerses).distinct().sortedWith(
            compareBy(
                { it.substringBefore(":").toIntOrNull() ?: 0 },
                { it.substringAfter(":").toIntOrNull() ?: 0 }
            )
        )
    }
    val selectedNumbers = selectedKeys.asSequence()
        .filter { it.substringBefore(":").toIntOrNull() == selectedChapterId }
        .mapNotNull { it.substringAfter(":").toIntOrNull() }
        .toSet()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hifz", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Select ayahs by number or tap them, then begin a study session.")
                }
                TextButton(onClick = onOpenProgress) { Text("Completed") }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Type numbers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = selectionText,
                        onValueChange = { selectionText = it },
                        placeholder = { Text("1, 36, 67:1-10") },
                        supportingText = { Text("Examples: 1, 18, 2:255, 67:1-30, 78-114") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1
                    )
                    if (selectedKeys.isEmpty() && selectionText.isNotBlank()) {
                        parsedSelection.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text("Tap ayahs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BoxMenuButton(
                            text = selectedChapter?.let { "${it.id}. ${it.name} (${it.verseCount})" } ?: "Choose surah",
                            expanded = showChapterMenu,
                            onExpandedChange = { showChapterMenu = it }
                        ) {
                            state.chapters.forEach { chapter ->
                                DropdownMenuItem(
                                    text = { Text("${chapter.id}. ${chapter.name} (${chapter.verseCount})") },
                                    onClick = {
                                        selectedChapterId = chapter.id
                                        showChapterMenu = false
                                        viewModel.loadChapter(chapter.id)
                                    }
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                selectedChapter?.let { chapter ->
                                    selectionText = listOf(selectionText.trim().trimEnd(','), chapter.id.toString())
                                        .filter(String::isNotBlank).joinToString(", ")
                                }
                            }
                        ) { Text("Whole surah") }
                    }
                    selectedChapter?.let { chapter ->
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
                            modifier = Modifier.fillMaxWidth().height(208.dp),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            gridItems(chapter.verseCount, key = { it + 1 }) { index ->
                                val verseNumber = index + 1
                                val isSelected = verseNumber in selectedNumbers
                                androidx.compose.material3.Surface(
                                    onClick = { viewModel.togglePlanVerse("${chapter.id}:$verseNumber") },
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "$verseNumber",
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Text("Selected set", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (selectedKeys.isEmpty()) "No ayahs yet. Add a surah or a range to begin."
                        else "${selectedKeys.size} ayahs · ${selectedKeys.take(3).joinToString(", ")}${if (selectedKeys.size > 3) " …" else ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectionText.isNotBlank() || state.selectedVerses.isNotEmpty()) {
                        TextButton(onClick = { selectionText = ""; viewModel.clearPlanSelection() }) { Text("Clear selection") }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Name this study set") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.savePlan(title, selectedKeys); selectionText = ""; title = "" },
                        enabled = title.isNotBlank() && selectedKeys.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save study set") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("Hide & reveal", "Recall from memory", Icons.Default.School, Modifier.weight(1f)) { onStartPractice(false) }
                QuickActionCard("Queezers", "Test your recall", Icons.Default.Checklist, Modifier.weight(1f)) { onStartPractice(true) }
            }
        }
        item { Text("Saved plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (plans.isEmpty()) item { Text("Your saved plans will be listed here.") }
        items(plans, key = { it.id }) { plan ->
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text(plan.title, fontWeight = FontWeight.SemiBold)
                    Text("${plan.verseKeys.size} ayahs · ${plan.verseKeys.firstOrNull().orEmpty()}${if (plan.verseKeys.size > 1) " – ${plan.verseKeys.last()}" else ""}")
                    Text("Created ${formatDate(plan.createdAt)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(
    memorized: List<QuranVerse>,
    savedWords: List<SavedVocabulary>,
    plans: List<StudyPlan>,
    streak: Int,
    viewModel: HifthViewModel,
    modifier: Modifier = Modifier
) {
    var showWords by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("A steady rhythm, one ayah at a time.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Study streak", "$streak", "days active", Modifier.weight(1f))
                MetricCard("Memorized", "${memorized.size}", "ayahs", Modifier.weight(1f))
                MetricCard("Plans", "${plans.size}", "saved", Modifier.weight(1f))
            }
        }
        item { Text("Completed verse sets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (memorized.isEmpty()) item { Text("Ayahs you mark memorized will appear here with their dates.") }
        val grouped = memorized.groupBy(QuranVerse::chapterId).toSortedMap()
        grouped.forEach { (chapterId, verses) ->
            item(key = "chapter-$chapterId") {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Surah $chapterId · ${verses.size} memorized ayahs", fontWeight = FontWeight.SemiBold)
                        Text(verses.sortedBy(QuranVerse::verseNumber).joinToString { it.verseNumber.toString() })
                        Text("Latest · ${verses.maxOfOrNull { it.memorizedAt ?: 0L }?.let(::formatDate).orEmpty()}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Saved vocabulary (${savedWords.size})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showWords = !showWords }) { Text(if (showWords) "Hide" else "View") }
            }
        }
        if (showWords) {
            if (savedWords.isEmpty()) item { Text("Tap a Quran word and save it to start your vocabulary list.") }
            items(savedWords, key = { it.id }) { word ->
                Card {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(word.arabic, fontSize = 23.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                            Text("${word.meaning} · ayah ${word.verseKey}")
                            if (word.root.isNotBlank()) Text("Root · ${word.root}", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.deleteVocabulary(word.id) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            Text(caption, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RecordScreen(
    state: HifthState,
    recordings: List<RecitationRecording>,
    viewModel: HifthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audio = remember { AudioController() }
    var recording by remember { mutableStateOf(false) }
    var recordingPath by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var selectedVerseKey by remember { mutableStateOf<String?>(null) }
    var recordingVerseKey by remember { mutableStateOf<String?>(null) }
    var showVerseMenu by remember { mutableStateOf(false) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    val selectedVerse = state.verses.firstOrNull { it.key == selectedVerseKey } ?: state.verses.firstOrNull()
    val recitationUrl = selectedVerse?.let { state.recitationUrls[it.key] }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recordingVerseKey = selectedVerse?.key
            recordingPath = startLocalRecording(context, recorderHolder = { recorder = it })
            recording = recordingPath != null
            permissionError = if (recording) null else "Recording could not be started."
        } else {
            permissionError = "Microphone permission is needed to record your recitation."
        }
    }
    LaunchedEffect(state.selectedChapter, state.selectedReciter, state.verses.map(QuranVerse::chapterId).distinct()) {
        (state.verses.map(QuranVerse::chapterId).distinct() + state.selectedChapter)
            .distinct()
            .forEach(viewModel::loadRecitationFiles)
    }
    LaunchedEffect(audio.isPlaying) {
        while (audio.isPlaying) {
            audio.refreshPosition()
            delay(500)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val currentRecorder = recorder
            if (currentRecorder != null) {
                runCatching { currentRecorder.stop() }.onSuccess {
                    val path = recordingPath
                    val verseKey = recordingVerseKey
                    if (path != null && verseKey != null) viewModel.saveRecording(verseKey, path)
                }
            }
            recorder?.release()
            audio.release()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Home") }
            Text("Listen & recite", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Listen to a reciter, record your own reading, and review it with playback controls.")
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoxMenuButton(
                        text = selectedVerse?.let { "Ayah ${it.key}" } ?: "Choose a loaded ayah",
                        expanded = showVerseMenu,
                        onExpandedChange = { showVerseMenu = it }
                    ) {
                        state.verses.forEach { verse ->
                            DropdownMenuItem(text = { Text(verse.key) }, onClick = {
                                selectedVerseKey = verse.key
                                showVerseMenu = false
                            })
                        }
                    }
                    selectedVerse?.let {
                        Text(
                            it.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl)
                        )
                        Text(it.translation)
                    } ?: Text("Load a surah from Read to choose an ayah.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !recording && selectedVerse != null,
                            onClick = {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                    android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    recordingVerseKey = selectedVerse?.key
                                    recordingPath = startLocalRecording(context, recorderHolder = { recorder = it })
                                    recording = recordingPath != null
                                    if (!recording) permissionError = "Recording could not be started."
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) { Text("Record") }
                        Button(
                            enabled = recording,
                            onClick = {
                                runCatching { recorder?.stop() }
                                    .onSuccess {
                                        val path = recordingPath
                                        val verseKey = recordingVerseKey
                                        if (path != null && verseKey != null) viewModel.saveRecording(verseKey, path)
                                    }
                                    .onFailure { permissionError = "The recording was too short. Try recording again." }
                                recorder?.release()
                                recorder = null
                                recording = false
                            }
                        ) { Text("Stop") }
                    }
                    if (recording) Text("Recording…", color = MaterialTheme.colorScheme.error)
                    permissionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Playback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = recordingPath != null && !recording,
                            onClick = { recordingPath?.let(audio::playFile) }
                        ) { Text("My recording") }
                        OutlinedButton(
                            enabled = recitationUrl != null,
                            onClick = { if (audio.isPlaying) audio.toggle() else recitationUrl?.let(audio::play) }
                        ) { Text(if (audio.isPlaying) "Pause" else "Reciter") }
                    }
                    Slider(
                        value = audio.position.toFloat().coerceIn(0f, audio.duration.coerceAtLeast(1).toFloat()),
                        onValueChange = { audio.seekTo(it.toInt()) },
                        valueRange = 0f..audio.duration.coerceAtLeast(1).toFloat(),
                        enabled = audio.duration > 0
                    )
                    Text("${formatTime(audio.position)} / ${formatTime(audio.duration)}", style = MaterialTheme.typography.labelMedium)
                    audio.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (recitationUrl == null) {
                        Text("Reciter audio loads from Quran.com when online.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (recordings.isNotEmpty()) {
            item { Text("My recordings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(recordings, key = { it.id }) { item ->
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ayah ${item.verseKey}", fontWeight = FontWeight.SemiBold)
                            Text(formatDate(item.createdAt), style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { audio.playFile(item.path) }) { Text("Play") }
                    }
                }
            }
        }
    }
}

private fun startLocalRecording(context: Context, recorderHolder: (MediaRecorder?) -> Unit): String? {
    val file = File(context.filesDir, "hifth-${System.currentTimeMillis()}.m4a")
    return runCatching {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()
        recorderHolder(mediaRecorder)
        file.absolutePath
    }.getOrElse {
        recorderHolder(null)
        null
    }
}

internal fun compareWords(expected: List<String>, actual: List<String>): List<Boolean> {
    val expectedNormalized = expected.map(::normalizeWord)
    val actualNormalized = actual.map(::normalizeWord)
    return (0 until maxOf(expected.size, actual.size)).map { index ->
        expectedNormalized.getOrNull(index) == actualNormalized.getOrNull(index)
    }
}

private fun normalizeWord(value: String): String =
    value.lowercase().replace(Regex("[\\p{M}\\u0640]"), "")
        .replace(Regex("[\\p{P}\\p{S}]"), "")

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))

private fun formatTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
