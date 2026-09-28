package com.hifth.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.settingsDataStore by preferencesDataStore(name = "hifth_settings")

class QuranRepository(
    private val context: Context,
    private val dao: QuranDao,
    private val api: QuranApi = QuranApi()
) {
    private val gson = Gson()
    private val streakKey = intPreferencesKey("study_streak")
    private val lastStudyDayKey = stringPreferencesKey("last_study_day")

    val chapters: Flow<List<Chapter>> = dao.observeChapters().map { entities -> entities.map(ChapterEntity::toModel) }
    val memorized: Flow<List<QuranVerse>> = dao.observeMemorizedVerses().map { entities -> entities.map(VerseEntity::toModel) }
    val savedWords: Flow<List<SavedVocabulary>> = dao.observeSavedWords().map { entities -> entities.map(SavedWordEntity::toModel) }
    val plans: Flow<List<StudyPlan>> = dao.observePlans().map { entities -> entities.map(StudyPlanEntity::toModel(gson)) }
    val recordings: Flow<List<RecitationRecording>> = dao.observeRecordings().map { entities -> entities.map(RecordingEntity::toModel) }
    val weakSpots: Flow<List<WeakSpot>> = dao.observeWeakSpots().map { entities -> entities.map(WeakSpotEntity::toModel) }
    val studyStreak: Flow<Int> = context.settingsDataStore.data.map { it[streakKey] ?: 0 }

    suspend fun refreshChapters(): List<Chapter> {
        val fetched = api.chapters()
        dao.saveChapters(fetched.map(Chapter::toEntity))
        return fetched
    }

    suspend fun verses(chapterId: Int, page: Int, juz: Int? = null): QuranVersePage {
        val cached = if (juz == null) {
            dao.versesForChapter(chapterId).map(VerseEntity::toModel)
        } else {
            dao.versesForJuz(juz).map(VerseEntity::toModel)
        }
        return try {
            val result = if (juz == null) api.versesByChapter(chapterId, page) else api.versesByJuz(juz, page)
            dao.saveVerses(result.map(QuranVerse::toEntity))
            QuranVersePage(result, result.size >= 50)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (page == 1 && cached.isNotEmpty()) QuranVersePage(cached, hasMore = false) else throw error
        }
    }

    suspend fun tafsirs(): List<TafsirOption> = api.tafsirs()
    suspend fun tafsir(tafsirId: Int, verseKey: String): String = api.tafsir(tafsirId, verseKey)
    suspend fun reciters(): List<Reciter> = api.reciters()
    suspend fun recitationFiles(reciterId: Int, chapterId: Int): Map<String, String> =
        api.recitationFiles(reciterId, chapterId)

    suspend fun saveVocabulary(word: QuranWord, verseKey: String) {
        dao.saveWord(SavedWordEntity(
            verseKey = verseKey,
            arabic = word.text,
            meaning = word.translation,
            root = word.root,
            savedAt = System.currentTimeMillis()
        ))
    }

    suspend fun markMemorized(verseKey: String) {
        dao.markMemorized(verseKey, System.currentTimeMillis())
    }

    suspend fun savePlan(title: String, verseKeys: List<String>) {
        dao.savePlan(StudyPlanEntity(title = title, verseKeysJson = gson.toJson(verseKeys), createdAt = System.currentTimeMillis()))
    }

    suspend fun saveRecording(verseKey: String, path: String) {
        dao.saveRecording(RecordingEntity(verseKey = verseKey, path = path, createdAt = System.currentTimeMillis()))
    }

    suspend fun recordPracticeAttempt(verseKey: String, errorPercent: Int) {
        dao.recordPracticeAttempt(verseKey, errorPercent, System.currentTimeMillis())
    }

    suspend fun recordStudySession() {
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val today = dayFormat.format(Date())
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = dayFormat.format(yesterdayCalendar.time)
        context.settingsDataStore.edit { preferences ->
            val lastStudyDay = preferences[lastStudyDayKey]
            if (lastStudyDay != today) {
                preferences[streakKey] = if (lastStudyDay == yesterday) {
                    (preferences[streakKey] ?: 0) + 1
                } else {
                    1
                }
                preferences[lastStudyDayKey] = today
            }
        }
    }

    suspend fun deleteVocabulary(id: Long) = dao.deleteSavedWord(id)

    private fun Chapter.toEntity() = ChapterEntity(id, nameArabic, name, translatedName, verseCount)
    private fun ChapterEntity.toModel() = Chapter(id, nameArabic, name, translatedName, verseCount)
    private fun QuranVerse.toEntity() = VerseEntity(
        key, chapterId, verseNumber, arabic, translation, gson.toJson(words), memorizedAt, juzNumber
    )

    private fun VerseEntity.toModel(): QuranVerse {
        val type = object : TypeToken<List<QuranWord>>() {}.type
        return QuranVerse(
            key, chapterId, verseNumber, arabic, translation,
            gson.fromJson(wordsJson, type) ?: emptyList(), memorizedAt, juzNumber
        )
    }

    private fun SavedWordEntity.toModel() = SavedVocabulary(id, verseKey, arabic, meaning, root, savedAt)
    private fun RecordingEntity.toModel() = RecitationRecording(id, verseKey, path, createdAt)
    private fun WeakSpotEntity.toModel() = WeakSpot(verseKey, attempts, errorPoints, lastPracticedAt)
    private fun StudyPlanEntity.toModel(gson: Gson): StudyPlan {
        val type = object : TypeToken<List<String>>() {}.type
        return StudyPlan(id, title, gson.fromJson(verseKeysJson, type) ?: emptyList(), createdAt)
    }
}
