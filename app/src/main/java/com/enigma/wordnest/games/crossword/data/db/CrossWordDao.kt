package com.enigma.wordnest.games.crossword.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CrosswordWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<CrosswordWordEntity>)

    @Query("SELECT COUNT(*) FROM crossword_words")
    suspend fun count(): Int

    @Query("SELECT * FROM crossword_words")
    suspend fun getAll(): List<CrosswordWordEntity>

    @Query("SELECT * FROM crossword_words WHERE length = :length")
    suspend fun getByLength(length: Int): List<CrosswordWordEntity>

    @Query("SELECT definition FROM crossword_words WHERE word = :word LIMIT 1")
    suspend fun definitionFor(word: String): String?

    @Query("DELETE FROM crossword_words")
    suspend fun clearAll()
}