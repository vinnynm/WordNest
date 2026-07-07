package com.enigma.wordnest.games.crossword.data

import android.content.Context
import com.enigma.wordnest.R
import com.enigma.wordnest.games.crossword.data.db.CrosswordDatabase
import com.enigma.wordnest.games.crossword.data.db.CrosswordWordEntity
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import androidx.core.content.edit

/**
 * Loads the WordNet-derived dictionary. First launch (or after a content-version
 * bump — see [DICTIONARY_CONTENT_VERSION]) parses wordnet_library.json /
 * wordnet_definitions.json from res/raw and bulk-inserts every row into Room.
 * Every subsequent launch skips JSON parsing entirely and reads straight from
 * the Room cache, which is dramatically faster for a ~150k+ word dictionary
 * (no JsonReader streaming, no string allocation churn, no cold GC pressure
 * from a huge one-shot object graph).
 *
 * [wordsByLength] and [definitionFor] are served from an in-memory snapshot
 * built once per process from whichever source (JSON or Room) was used —
 * callers don't need to know which happened.
 */
class CrosswordWordRepository(private val context: Context) {

    private val db by lazy { CrosswordDatabase.getInstance(context) }
    private val prefs by lazy {
        context.getSharedPreferences("crossword_dictionary_prefs", Context.MODE_PRIVATE)
    }

    private var byLength: Map<Int, List<String>> = emptyMap()
    private var definitions: Map<String, String> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        val dao = db.wordDao()
        val cacheIsCurrent = prefs.getInt(KEY_DB_VERSION, -1) == DICTIONARY_CONTENT_VERSION
        val cachedCount = if (cacheIsCurrent) dao.count() else 0

        val entities: List<CrosswordWordEntity> = if (cachedCount > 0) {
            dao.getAll()
        } else {
            val fresh = parseFromJson()
            dao.clearAll()
            // Room has a per-statement variable limit; chunk the bulk insert so a
            // 150k+ word dictionary doesn't blow past it in one call.
            fresh.chunked(500).forEach { chunk -> dao.insertAll(chunk) }
            prefs.edit { putInt(KEY_DB_VERSION, DICTIONARY_CONTENT_VERSION) }
            fresh
        }

        byLength = entities.groupBy({ it.length }, { it.word })
        definitions = entities.mapNotNull { e -> e.definition?.let { e.word to it } }.toMap()
    }

    private fun parseFromJson(): List<CrosswordWordEntity> {
        val defs = parseDefinitions()
        val words = mutableSetOf<String>()
        context.resources.openRawResource(R.raw.wordnet_library).use { input ->
            JsonReader(InputStreamReader(input, "UTF-8")).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    reader.nextName() // letter bucket key, e.g. "A"
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val w = reader.nextString().trim().lowercase()
                        if (w.isNotBlank()) words.add(w)
                    }
                    reader.endArray()
                }
                reader.endObject()
            }
        }
        return words.map { w -> CrosswordWordEntity(word = w, length = w.length, definition = defs[w]) }
    }

    private fun parseDefinitions(): Map<String, String> {
        val defs = mutableMapOf<String, String>()
        try {
            context.resources.openRawResource(R.raw.wordnet_definitions).use { input ->
                JsonReader(InputStreamReader(input, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val word = reader.nextName().lowercase()
                        defs[word] = reader.nextString()
                    }
                    reader.endObject()
                }
            }
        } catch (e: Exception) {
            // wordnet_definitions.json not bundled (yet) — definitions stay empty,
            // WordNetJsonClueSource falls back to generic clue text.
        }
        return defs
    }

    fun isLoaded() = byLength.isNotEmpty()

    fun wordsByLength(): Map<Int, List<String>> = byLength

    fun definitionFor(word: String): String? = definitions[word.lowercase()]

    companion object {
        /**
         * Bump this whenever wordnet_library.json / wordnet_definitions.json are
         * regenerated with different content — forces one more JSON parse + Room
         * repopulation on the next app launch instead of silently serving a stale
         * cache from an old APK version.
         */
        private const val DICTIONARY_CONTENT_VERSION = 1
        private const val KEY_DB_VERSION = "crossword_dictionary_db_version"
    }
}