package com.enigma.wordnest.games.crossword.data

/**
 * ClueSource backed by wordnet_definitions.json (via CrosswordWordRepository),
 * replacing the small curated map in FallbackClueSource with real WordNet
 * definitions. Falls back to FallbackClueSource's generic "${length}-letter
 * word" text for any word WordNet has no definition for (proper nouns, gaps
 * in the generator's POS coverage, etc.) — mirrors the "keep the word, degrade
 * clue quality instead of shrinking the fill pool" strategy already described
 * in ClueRepository.kt's docstring.
 *
 * This is the seam ClueRepository.kt calls out: swap this in for
 * FallbackClueSource with zero changes to CrosswordViewModel beyond the
 * constructor argument.
 */
class WordNetJsonClueSource(
    private val repo: CrosswordWordRepository
) : ClueSource {

    private val fallback = FallbackClueSource()

    override suspend fun clueFor(word: String): String? {
        val definition = repo.definitionFor(word)
        if (!definition.isNullOrBlank()) return definition
        return fallback.clueFor(word)
    }
}