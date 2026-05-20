package com.busraknya.moodjournal.data.repository

import com.busraknya.moodjournal.data.database.EntryDAO
import com.busraknya.moodjournal.data.database.EntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Repository for handling data operations for journal entries.
 * It abstracts the data source (DAO) from the ViewModels.
 */
class EntryRepository @Inject constructor(
    private val entryDAO: EntryDAO
) {
    // Exposes a flow of all entries, ensuring data is fetched on an IO thread.
    fun getAllEntries(): Flow<List<EntryEntity>> = entryDAO.getAllEntries()
        .flowOn(Dispatchers.IO)
        .conflate()

    suspend fun addEntry(entry: EntryEntity) = entryDAO.insertEntry(entry)

    suspend fun updateEntry(entry: EntryEntity) = entryDAO.updateEntry(entry)

    suspend fun deleteEntry(entry: EntryEntity) = entryDAO.deleteEntry(entry)

    suspend fun deleteAllEntries() = entryDAO.deleteAllEntries()

    suspend fun getEntryById(entryId: String): EntryEntity? = entryDAO.getEntryById(entryId)

    fun getEntriesBetween(startDate: String, endDate: String): Flow<List<EntryEntity>> =
        entryDAO.getEntriesBetween(startDate, endDate).flowOn(Dispatchers.IO).conflate()

    suspend fun getEntryCount(): Int = entryDAO.getEntryCount()
}