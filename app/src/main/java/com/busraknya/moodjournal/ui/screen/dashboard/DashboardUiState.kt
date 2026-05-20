package com.busraknya.moodjournal.ui.screen.dashboard

import androidx.annotation.StringRes

// It's a good practice to group related, small data models and states in a single file
// for better organization, especially if they are only used by one screen.

/**
 * Defines the time periods available for viewing data on the Dashboard.
 */
enum class ViewMode {
    WEEKLY,
    MONTHLY
}

/**
 * Represents a piece of contextual, "smart" text displayed to the user.
 * This structure allows for dynamic string formatting.
 *
 * @param textResId The string resource ID for the insight text (e.g., R.string.smart_insight_positive).
 * @param args A list of arguments to be formatted into the string resource (e.g., a day name like "Monday").
 */
data class Insight(
    @StringRes val textResId: Int,
    val args: List<Any> = emptyList()
)

/**
 * A data-holding class that contains all the processed information required by the
 * Dashboard UI when it is in a `Success` state.
 *
 * This class is an immutable representation of the data to be displayed.
 */
data class DashboardData(
    val chartEntries: List<List<Float>>,
    val dateLabels: List<String>,
    val dominantMood: String,
    val totalEntries: Int, // Renamed from totalNotes for consistency
    val positiveCount: Int,
    val negativeCount: Int,
    val neutralCount: Int,
    val smartInsight: Insight,
    val dominantMoodInsight: Insight,
    val viewMode: ViewMode
)

/**
 * Represents all possible states for the Dashboard UI.
 *
 * Using a sealed class for UI state is a modern Android best practice. It ensures that the
 * UI must exhaustively handle all possible outcomes (Loading, Success, various Empty states, Error),
 * which makes the code more robust and prevents state-related bugs.
 */
sealed class DashboardUiState {
    /**
     * Represents the state where data is being fetched for the first time or after a refresh.
     * The UI should typically display a full-screen loading indicator.
     */
    object Loading : DashboardUiState()

    /**
     * Represents the state where data has been successfully loaded and is ready to be displayed.
     * @param data The [DashboardData] payload containing everything the UI needs to render.
     */
    data class Success(val data: DashboardData) : DashboardUiState()

    /**
     * Represents the state where there are no journal entries at all in the database.
     * The UI should show a welcome message encouraging the user to create their first entry.
     */
    object EmptyInitial : DashboardUiState()

    /**
     * Represents the state where the database has entries, but there are none for the currently
     * selected time period (e.g., an empty week).
     * The UI should show a message like "No entries for this period."
     */
    object EmptyPeriod : DashboardUiState() // Renamed for clarity

    /**
     * Represents a state where an unexpected error occurred during data fetching.
     * @param message A user-friendly error message to be displayed.
     */
    data class Error(val message: String) : DashboardUiState()
}