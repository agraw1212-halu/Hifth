package com.hifth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hifth.data.Chapter
import com.hifth.data.QuranRepository
import com.hifth.data.QuranVerse
import com.hifth.data.QuranWord
import com.hifth.data.Reciter
import com.hifth.data.TafsirOption
import com.hifth.data.WeakSpot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HifthState(
    val chapters: List<Chapter> = emptyList(),
    val verses: List<QuranVerse> = emptyList(),
    val selectedChapter: Int = 1,
    val juz: Int? = null,
    val page: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val tafsirVerse: String? = null,
    val tafsirText: String? = null,
    val tafsirLoading: Boolean = false,
    val tafsirs: List<TafsirOption> = emptyList(),
    val selectedTafsir: Int? = null,
    val reciters: List<Reciter> = emptyList(),
    val selectedReciter: Int = 7,
    val recitationUrls: Map<String, String> = emptyMap(),
    val selectedWord: Pair<QuranWord, String>? = null,
    val selectedVerses: Set<String> = emptySet()
)

class HifthViewModel(private val repository: QuranRepository) : ViewModel() {
    private val _state = MutableStateFlow(HifthState())
    private val loadedAudioChapters = mutableSetOf<Pair<Int, Int>>()
    val state = _state.asStateFlow()

    val memorized = repository.memorized
    val savedWords = repository.savedWords
    val plans = repository.plans
    val recordings = repository.recordings
    val weakSpots = repository.weakSpots
    val studyStreak = repository.studyStreak

    init {
        viewModelScope.launch {
            repository.chapters.collect { chapters ->
                _state.update { it.copy(chapters = chapters) }
                if (chapters.isNotEmpty() && _state.value.verses.isEmpty()) loadChapter(1)
            }
        }
        viewModelScope.launch {
            runSafely { repository.refreshChapters() }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not load surahs. Check your connection.") } }
        }
        loadReciters()
        loadTafsirs()
    }

    fun loadChapter(chapterId: Int) {
        viewModelScope.launch {
            _state.update {
                it.copy(selectedChapter = chapterId, juz = null, verses = emptyList(), page = 0, hasMore = true, loading = true, error = null)
            }
            loadVerses(page = 1, chapterId = chapterId, juz = null, append = false)
        }
    }

    fun loadJuz(juz: Int) {
        viewModelScope.launch {
            _state.update {
                it.copy(juz = juz, verses = emptyList(), page = 0, hasMore = true, loading = true, error = null)
            }
            loadVerses(page = 1, chapterId = _state.value.selectedChapter, juz = juz, append = false)
        }
    }

    fun loadMore() {
        val snapshot = _state.value
        if (!snapshot.loading && snapshot.hasMore) {
            viewModelScope.launch {
                _state.update { it.copy(loading = true, error = null) }
                loadVerses(snapshot.page + 1, snapshot.selectedChapter, snapshot.juz, append = true)
            }
        }
    }

    private suspend fun loadVerses(page: Int, chapterId: Int, juz: Int?, append: Boolean) {
        runSafely { repository.verses(chapterId, page, juz) }
            .onSuccess { result ->
                _state.update { current ->
                    val all = if (append) current.verses + result.verses else result.verses
                    current.copy(
                        verses = all.distinctBy(QuranVerse::key),
                        page = page,
                        hasMore = result.hasMore,
                        loading = false,
                        error = null
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "Unable to load Quran content.") }
            }
    }

    private fun loadReciters() {
        viewModelScope.launch {
            runSafely { repository.reciters() }
                .onSuccess { list ->
                    _state.update { current ->
                        current.copy(
                            reciters = list,
                            selectedReciter = list.firstOrNull { it.id == current.selectedReciter }?.id ?: list.firstOrNull()?.id ?: current.selectedReciter
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not load reciters.") } }
        }
    }

    private fun loadTafsirs() {
        viewModelScope.launch {
            runSafely { repository.tafsirs() }
                .onSuccess { list ->
                    _state.update { current ->
                        current.copy(tafsirs = list, selectedTafsir = current.selectedTafsir ?: list.firstOrNull()?.id)
                    }
                }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not load tafsir sources.") } }
        }
    }

    fun selectTafsir(id: Int) {
        _state.update { it.copy(selectedTafsir = id) }
        _state.value.tafsirVerse?.let(::loadTafsir)
    }

    fun selectReciter(id: Int) {
        loadedAudioChapters.clear()
        _state.update { it.copy(selectedReciter = id, recitationUrls = emptyMap()) }
        loadRecitationFiles()
    }

    fun loadRecitationFiles(chapterId: Int = _state.value.selectedChapter) {
        val current = _state.value
        val key = current.selectedReciter to chapterId
        if (!loadedAudioChapters.add(key)) return
        viewModelScope.launch {
            runSafely { repository.recitationFiles(current.selectedReciter, chapterId) }
                .onSuccess { urls ->
                    if (_state.value.selectedReciter == current.selectedReciter) {
                        _state.update { it.copy(recitationUrls = it.recitationUrls + urls) }
                    }
                }
                .onFailure { error ->
                    loadedAudioChapters.remove(key)
                    _state.update { it.copy(error = error.message ?: "Could not load recitation audio.") }
                }
        }
    }

    fun showWord(word: QuranWord, verseKey: String) {
        _state.update { it.copy(selectedWord = word to verseKey) }
    }

    fun dismissWord() {
        _state.update { it.copy(selectedWord = null) }
    }

    fun saveSelectedWord() {
        val selection = _state.value.selectedWord ?: return
        viewModelScope.launch {
            runSafely { repository.saveVocabulary(selection.first, selection.second) }
                .onSuccess { dismissWord() }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not save this word.") } }
        }
    }

    fun togglePlanVerse(verseKey: String) {
        _state.update { current ->
            val selection = current.selectedVerses.toMutableSet()
            if (!selection.add(verseKey)) selection.remove(verseKey)
            current.copy(selectedVerses = selection)
        }
    }

    fun clearPlanSelection() {
        _state.update { it.copy(selectedVerses = emptySet()) }
    }

    fun savePlan(title: String, verses: List<String>) {
        if (title.isBlank() || verses.isEmpty()) return
        viewModelScope.launch {
            runSafely {
                repository.savePlan(title.trim(), verses.distinct())
                repository.recordStudySession()
            }.onSuccess { clearPlanSelection() }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not save this plan.") } }
        }
    }

    fun markMemorized(verseKey: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            runSafely {
                repository.markMemorized(verseKey)
                repository.recordStudySession()
            }.onSuccess {
                _state.update { current ->
                    current.copy(
                        selectedVerses = current.selectedVerses - verseKey,
                        verses = current.verses.map { verse ->
                            if (verse.key == verseKey) verse.copy(memorizedAt = now) else verse
                        }
                    )
                }
            }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not update memorization progress.") } }
        }
    }

    fun recordStudySession() {
        viewModelScope.launch { repository.recordStudySession() }
    }

    fun recordPracticeAttempt(verseKey: String, errorPercent: Int) {
        viewModelScope.launch {
            runSafely {
                repository.recordPracticeAttempt(verseKey, errorPercent)
                repository.recordStudySession()
            }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not save practice progress.") } }
        }
    }

    fun deleteVocabulary(id: Long) {
        viewModelScope.launch {
            runSafely { repository.deleteVocabulary(id) }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not remove this word.") } }
        }
    }

    fun saveRecording(verseKey: String, path: String) {
        viewModelScope.launch {
            runSafely { repository.saveRecording(verseKey, path) }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Could not save this recording.") } }
        }
    }

    fun loadTafsir(verseKey: String) {
        val tafsirId = _state.value.selectedTafsir
        if (tafsirId == null) {
            _state.update { it.copy(tafsirVerse = verseKey, tafsirText = "Connect to the internet to load tafsir sources.", tafsirLoading = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(tafsirVerse = verseKey, tafsirText = null, tafsirLoading = true) }
            runSafely { repository.tafsir(tafsirId, verseKey) }
                .onSuccess { text -> _state.update { it.copy(tafsirText = text, tafsirLoading = false) } }
                .onFailure { error -> _state.update { it.copy(tafsirText = error.message ?: "Tafsir is unavailable offline.", tafsirLoading = false) } }
        }
    }

    private suspend fun <T> runSafely(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    fun dismissTafsir() {
        _state.update { it.copy(tafsirVerse = null, tafsirText = null) }
    }

    class Factory(private val repository: QuranRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HifthViewModel::class.java))
            return HifthViewModel(repository) as T
        }
    }
}
