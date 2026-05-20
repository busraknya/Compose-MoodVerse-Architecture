package com.busraknya.moodjournal.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for journal entries.
 * Defines the database interactions, abstracting SQL queries from the repository.
 */
@Dao
interface EntryDAO {

    @Query("SELECT * FROM entries_table ORDER BY note_date DESC")
    fun getAllEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries_table WHERE note_id = :entryId")
    suspend fun getEntryById(entryId: String): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity)

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)

    @Query("DELETE FROM entries_table")
    suspend fun deleteAllEntries()

    @Query("SELECT * FROM entries_table WHERE note_date BETWEEN :startDate AND :endDate")
    fun getEntriesBetween(startDate: String, endDate: String): Flow<List<EntryEntity>>

    @Query("SELECT COUNT(note_id) FROM entries_table")
    suspend fun getEntryCount(): Int
}