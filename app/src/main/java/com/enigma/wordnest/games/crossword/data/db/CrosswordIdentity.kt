package com.enigma.wordnest.games.crossword.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per dictionary word, cached from wordnet_library.json /
 * wordnet_definitions.json after the first parse. [length] is stored
 * denormalized so `wordsByLength()` can be a single indexed query instead
 * of a full-table scan + group-by in Kotlin on every cold start.
 */
@Entity(tableName = "crossword_words")
data class CrosswordWordEntity(
    @PrimaryKey val word: String,
    val length: Int,
    val definition: String?
)