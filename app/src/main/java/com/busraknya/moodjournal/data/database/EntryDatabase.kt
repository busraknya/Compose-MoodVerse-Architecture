package com.busraknya.moodjournal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room Database class for the application.
 * Connects the EntryEntity (table) with the EntryDAO (queries).
 */
@Database(entities = [EntryEntity::class], version = 1, exportSchema = false)
abstract class EntryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDAO
}