package com.enigma.wordnest.games.crossword.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CrosswordWordEntity::class], version = 1, exportSchema = false)
abstract class CrosswordDatabase : RoomDatabase() {
    abstract fun wordDao(): CrosswordWordDao

    companion object {
        @Volatile private var instance: CrosswordDatabase? = null

        fun getInstance(context: Context): CrosswordDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrosswordDatabase::class.java,
                    "crossword_words.db"
                ).build().also { instance = it }
            }
    }
}