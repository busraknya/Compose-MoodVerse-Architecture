package com.busraknya.moodjournal.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a single journal entry in the database.
 */
@Entity(tableName = "entries_table")
data class EntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "note_id")
    val noteID: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "note_title")
    val noteTitle: String,

    @ColumnInfo(name = "note_subtitle")
    val noteSubtitle: String,

    @ColumnInfo(name = "note_description")
    val noteDescription: String,

    @ColumnInfo(name = "note_image")
    val noteImagePath: String?,

    @ColumnInfo(name = "note_sentiment")
    val noteSentiment: String?,

    @ColumnInfo(name = "note_date")
    val noteEntryDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
)