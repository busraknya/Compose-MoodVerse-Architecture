package com.busraknya.moodjournal.ui.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.busraknya.moodjournal.data.database.EntryDAO
import com.busraknya.moodjournal.data.database.EntryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for providing database-related dependencies.
 * This object tells Hilt how to create instances of the database and its DAOs,
 * so they can be injected anywhere in the application.
 */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    private const val DATABASE_NAME = "mood_journal.db"

    /*
    // ============================ EXAMPLE OF A DATABASE MIGRATION ============================
    //
    // If you add a new column to the EntryEntity (e.g., 'val category: String'), you must:
    // 1.  Increase the database version in EntryDatabase.kt (e.g., from 1 to 2).
    // 2.  Create a Migration object like this to tell Room how to handle the update without losing data.
    //
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // This SQL command adds a new 'category' column to the existing table.
            db.execSQL("ALTER TABLE entries_table ADD COLUMN category TEXT DEFAULT 'General' NOT NULL")
        }
    }
    */

    @Singleton
    @Provides
    fun provideRoomDatabase(@ApplicationContext context: Context): EntryDatabase {
        return Room.databaseBuilder(context, EntryDatabase::class.java, DATABASE_NAME)
            // ============================ ⚠️ IMPORTANT NOTE FOR PRODUCTION ⚠️ ============================
            // `fallbackToDestructiveMigration()` is used here for development convenience.
            // For a live app, you MUST remove this and implement a proper Migration strategy
            // to avoid deleting user data on schema updates. A working example is provided above.
            // See the README.md file for more information.
            //
            // TO PREPARE FOR PRODUCTION:
            // 1. Comment out or DELETE the line below:
            .fallbackToDestructiveMigration()
            // 2. Uncomment the MIGRATION_1_2 object above.
            // 3. Add your migrations to the builder like this:
            // .addMigrations(MIGRATION_1_2)
            // =========================================================================================
            .build()
    }

    @Singleton
    @Provides
    fun provideEntryDao(database: EntryDatabase): EntryDAO {
        return database.entryDao()
    }
}