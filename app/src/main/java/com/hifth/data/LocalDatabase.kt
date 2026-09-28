package com.hifth.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: Int,
    val nameArabic: String,
    val name: String,
    val translatedName: String,
    val verseCount: Int
)

@Entity(tableName = "verses")
data class VerseEntity(
    @PrimaryKey val key: String,
    val chapterId: Int,
    val verseNumber: Int,
    val arabic: String,
    val translation: String,
    val wordsJson: String,
    val memorizedAt: Long? = null,
    val juzNumber: Int = 0
)

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseKey: String,
    val arabic: String,
    val meaning: String,
    val root: String,
    val savedAt: Long
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val verseKeysJson: String,
    val createdAt: Long
)

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseKey: String,
    val path: String,
    val createdAt: Long
)

@Entity(tableName = "weak_spots")
data class WeakSpotEntity(
    @PrimaryKey val verseKey: String,
    val attempts: Int,
    val errorPoints: Int,
    val lastPracticedAt: Long
)

@Dao
interface QuranDao {
    @Query("SELECT * FROM chapters ORDER BY id")
    fun observeChapters(): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM verses WHERE chapterId = :chapterId ORDER BY verseNumber")
    suspend fun versesForChapter(chapterId: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE juzNumber = :juzNumber ORDER BY chapterId, verseNumber")
    suspend fun versesForJuz(juzNumber: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE key IN (:keys)")
    suspend fun versesForKeys(keys: List<String>): List<VerseEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVerses(verses: List<VerseEntity>)

    @Query("UPDATE verses SET chapterId = :chapterId, verseNumber = :verseNumber, arabic = :arabic, translation = :translation, wordsJson = :wordsJson, juzNumber = :juzNumber WHERE `key` = :key")
    suspend fun updateVerseContent(key: String, chapterId: Int, verseNumber: Int, arabic: String, translation: String, wordsJson: String, juzNumber: Int)

    @Transaction
    suspend fun saveVerses(verses: List<VerseEntity>) {
        insertVerses(verses)
        verses.forEach { verse ->
            updateVerseContent(verse.key, verse.chapterId, verse.verseNumber, verse.arabic, verse.translation, verse.wordsJson, verse.juzNumber)
        }
    }

    @Query("UPDATE verses SET memorizedAt = :time WHERE `key` = :verseKey")
    suspend fun markMemorized(verseKey: String, time: Long)

    @Query("SELECT * FROM verses WHERE memorizedAt IS NOT NULL ORDER BY memorizedAt DESC")
    fun observeMemorizedVerses(): Flow<List<VerseEntity>>

    @Insert
    suspend fun saveWord(word: SavedWordEntity)

    @Query("SELECT * FROM saved_words ORDER BY savedAt DESC")
    fun observeSavedWords(): Flow<List<SavedWordEntity>>

    @Query("DELETE FROM saved_words WHERE id = :id")
    suspend fun deleteSavedWord(id: Long)

    @Insert
    suspend fun savePlan(plan: StudyPlanEntity)

    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun observePlans(): Flow<List<StudyPlanEntity>>

    @Insert
    suspend fun saveRecording(recording: RecordingEntity)

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM weak_spots ORDER BY errorPoints DESC")
    fun observeWeakSpots(): Flow<List<WeakSpotEntity>>

    @Query("SELECT * FROM weak_spots WHERE verseKey = :verseKey")
    suspend fun weakSpot(verseKey: String): WeakSpotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeakSpot(weakSpot: WeakSpotEntity)

    @Transaction
    suspend fun recordPracticeAttempt(verseKey: String, errorPercent: Int, time: Long) {
        val previous = weakSpot(verseKey)
        saveWeakSpot(
            WeakSpotEntity(
                verseKey = verseKey,
                attempts = (previous?.attempts ?: 0) + 1,
                errorPoints = (previous?.errorPoints ?: 0) + errorPercent,
                lastPracticedAt = time
            )
        )
    }
}

@Database(
    entities = [ChapterEntity::class, VerseEntity::class, SavedWordEntity::class, StudyPlanEntity::class, RecordingEntity::class, WeakSpotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HifthDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        fun create(context: Context): HifthDatabase =
            Room.databaseBuilder(context, HifthDatabase::class.java, "hifth.db").build()
    }
}
