package com.hifth.data

import android.text.Html
import android.os.Build
import com.hifth.BuildConfig
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class QuranApi {
    private val baseUrl = BuildConfig.QURAN_API_BASE_URL.trimEnd('/') + "/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun chapters(): List<Chapter> = request("chapters?language=en")
        .getAsJsonArray("chapters").map { item ->
            val chapter = item.asJsonObject
            Chapter(
                id = chapter.int("id"),
                nameArabic = chapter.string("name_arabic"),
                name = chapter.string("name_simple"),
                translatedName = chapter.getAsJsonObject("translated_name")?.string("name").orEmpty(),
                verseCount = chapter.int("verses_count")
            )
        }

    suspend fun versesByChapter(chapterId: Int, page: Int): List<QuranVerse> {
        val query = "verses/by_chapter/$chapterId?language=en&words=true&word_fields=text_uthmani,translation,transliteration&fields=text_uthmani&translations=20&per_page=50&page=$page"
        return parseVerses(request(query).getAsJsonArray("verses"))
    }

    suspend fun versesByJuz(juz: Int, page: Int): List<QuranVerse> {
        val query = "verses/by_juz/$juz?language=en&words=true&word_fields=text_uthmani,translation,transliteration&fields=text_uthmani&translations=20&per_page=50&page=$page"
        return parseVerses(request(query).getAsJsonArray("verses"))
    }

    suspend fun tafsirs(): List<TafsirOption> =
        request("resources/tafsirs?language=en").getAsJsonArray("tafsirs").map { item ->
            val tafsir = item.asJsonObject
            TafsirOption(tafsir.int("id"), tafsir.string("name"))
        }

    suspend fun tafsir(tafsirId: Int, verseKey: String): String {
        val encoded = verseKey.replace(":", "%3A")
        val result = request("tafsirs/$tafsirId/by_ayah/$encoded")
        return htmlToText(result.getAsJsonObject("tafsir")?.string("text").orEmpty())
    }

    suspend fun reciters(): List<Reciter> =
        request("resources/recitations?language=en").getAsJsonArray("recitations").map { item ->
            val reciter = item.asJsonObject
            Reciter(reciter.int("id"), reciter.string("reciter_name"))
        }

    suspend fun recitationFiles(reciterId: Int, chapterId: Int): Map<String, String> {
        val result = request("recitations/$reciterId/by_chapter/$chapterId")
        val files = result.getAsJsonArray("audio_files") ?: JsonArray()
        return files.associate { item ->
            val file = item.asJsonObject
            file.string("verse_key") to file.string("url").let { url ->
                if (url.startsWith("http")) url else "https://verses.quran.com/$url"
            }
        }
    }

    private suspend fun request(path: String): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Quran.com returned HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Quran.com returned an empty response")
            JsonParser.parseString(body).asJsonObject
        }
    }

    private fun parseVerses(array: JsonArray): List<QuranVerse> = array.map { item ->
        val verse = item.asJsonObject
        val key = verse.string("verse_key")
        val parts = key.split(":")
        val words = verse.getAsJsonArray("words")?.map { wordItem ->
            val word = wordItem.asJsonObject
            QuranWord(
                id = word.get("id")?.takeUnless(JsonElement::isJsonNull)?.asInt,
                text = word.string("text_uthmani"),
                translation = word.getAsJsonObject("translation")?.string("text").orEmpty(),
                transliteration = word.getAsJsonObject("transliteration")?.string("text").orEmpty(),
                root = word.string("root"),
                lemma = word.string("lemma"),
                grammar = word.string("grammar")
            )
        }.orEmpty()
        QuranVerse(
            key = key,
            chapterId = parts.getOrNull(0)?.toIntOrNull() ?: 1,
            verseNumber = parts.getOrNull(1)?.toIntOrNull() ?: 1,
            arabic = verse.string("text_uthmani"),
            translation = verse.getAsJsonArray("translations")?.firstOrNull()
                ?.asJsonObject?.string("text")?.let(::htmlToText).orEmpty(),
            words = words,
            juzNumber = verse.int("juz_number")
        )
    }

    private fun JsonObject.string(key: String): String =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asString.orEmpty()

    private fun JsonObject.int(key: String, fallback: Int = 0): Int =
        get(key)?.takeUnless(JsonElement::isJsonNull)?.asInt ?: fallback

    @Suppress("DEPRECATION")
    private fun htmlToText(html: String): String {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
        return result.toString().trim()
    }
}
