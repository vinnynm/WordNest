package com.enigma.wordnest.games.crossword.data

/**
 * Definition source for crossword clues (design doc §4.1 — WordNet, CC0-compatible license).
 *
 * The real integration (per the audit) should:
 *   1. Process the WordNet dataset OFFLINE into a pre-populated SQLite database shipped as
 *      an app asset (avoids parsing WordNet's raw index/data files at runtime).
 *   2. Query it via Room using FTS5 (not FTS4 — FTS5 has better ranking and is the actively
 *      maintained variant; there's no real reason to prefer FTS4 here).
 *
 * That Room/FTS5 layer isn't wired up in this pass (no WordNet asset is bundled with this
 * project), so [ClueSource] is the seam: swap [FallbackClueSource] for a
 * `WordNetFtsClueSource(db: WordNetDatabase)` implementation once the asset + DAO exist,
 * with zero changes needed to CrosswordViewModel.
 */
interface ClueSource {
    /** Returns a short clue string for [word], or null if no definition is available. */
    suspend fun clueFor(word: String): String?
}

/**
 * Sketch of the real Room/FTS5 entity shape, kept here as documentation for the follow-up
 * integration. Not compiled against an actual Room dependency in this pass.
 *
 * @Entity(tableName = "word_definitions")
 * @Fts5
 * data class WordNetEntity(val word: String, val definition: String, val partOfSpeech: String)
 *
 * @Dao
 * interface WordNetDao {
 *     @Query("SELECT * FROM word_definitions WHERE word_definitions MATCH :word LIMIT 1")
 *     suspend fun definitionFor(word: String): WordNetEntity?
 * }
 */
object WordNetSchemaNote

/**
 * Minimal built-in fallback so Crossword is playable without the WordNet asset. Falls back to
 * a generic "N-letter word" clue for anything not in the curated map, so generation never
 * silently excludes a word for lack of a definition (design doc §4.3 step 4 mentions excluding
 * clue-less words as one valid strategy — this fallback is the alternative: keep the word,
 * degrade clue quality instead of shrinking the fill candidate pool).
 */
class FallbackClueSource : ClueSource {

    private val curated: Map<String, String> = mapOf(
        "apple" to "Common orchard fruit",
        "river" to "Flowing body of water",
        "eagle" to "Bird of prey with sharp talons",
        "music" to "Organized sound, art form",
        "ocean" to "Vast body of salt water",
        "cloud" to "Visible mass of water vapor in the sky",
        "table" to "Furniture with a flat top and legs",
        "light" to "Opposite of dark",
        "sound" to "What you hear",
        "green" to "Color of grass"
        // A real build replaces/extends this via the WordNet-backed ClueSource above.
    )

    override suspend fun clueFor(word: String): String? {
        val lower = word.lowercase()
        curated[lower]?.let { return it }
        return "${word.length}-letter word"
    }
}
