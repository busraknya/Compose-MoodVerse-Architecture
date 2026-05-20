@file:OptIn(ExperimentalCoroutinesApi::class)

package com.busraknya.moodjournal.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.busraknya.moodjournal.data.database.EntryEntity
import com.busraknya.moodjournal.data.repository.EntryRepository
import com.busraknya.moodjournal.ml.SentimentAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the [DashboardScreen].
 *
 * It combines user inputs (selected date and view mode) to reactively fetch and process
 * entry data from the [EntryRepository], transforming it into a [DashboardUiState] for the UI.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val noteRepository: EntryRepository
) : ViewModel() {

    // Internal state for the currently displayed date, controlled by the user.
    private val _displayedDate = MutableStateFlow(LocalDate.now())
    val displayedDate: StateFlow<LocalDate> = _displayedDate.asStateFlow()

    // Internal state for the current view mode (Weekly/Monthly), controlled by the user.
    private val _viewMode = MutableStateFlow(ViewMode.WEEKLY)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    /**
     * The main UI state flow for the Dashboard screen.
     * It reactively updates whenever the displayed date or view mode changes.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        _displayedDate,
        _viewMode
    ) { date, mode ->
        Pair(date, mode)
    }.flatMapLatest { (date, mode) ->
        // Calculate the start and end dates for the selected period.
        val (startDate, endDate) = when (mode) {
            ViewMode.WEEKLY -> {
                val startOfWeek = date.with(WeekFields.ISO.firstDayOfWeek)
                startOfWeek to startOfWeek.plusDays(6)
            }

            ViewMode.MONTHLY -> {
                val startOfMonth = date.withDayOfMonth(1)
                startOfMonth to startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth())
            }
        }

        noteRepository.getNotesBetween(
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).map { notesForPeriod ->
            if (notesForPeriod.isEmpty()) {
                val totalNoteCount = noteRepository.getNotesCount()
                if (totalNoteCount > 0) {
                    DashboardUiState.EmptyWeekWithHistory
                } else {
                    DashboardUiState.EmptyInitial
                }
            } else {
                val positiveCount =
                    notesForPeriod.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_POSITIVE }
                val negativeCount =
                    notesForPeriod.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_NEGATIVE }
                val neutralCount =
                    notesForPeriod.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_NEUTRAL }
                val dominantMood = findDominantMood(positiveCount, negativeCount, neutralCount)

                val dominantInsight = generateDominantMoodInsight(dominantMood)
                val detailedInsight = generateDetailedDayInsight(notesForPeriod)

                val dateLabels = mutableListOf<String>()
                val chartEntries = List(3) { mutableListOf<Float>() }

                val (periodDays, labelPattern) = when (mode) {
                    ViewMode.WEEKLY -> 7 to "dd/MM"
                    ViewMode.MONTHLY -> startDate.lengthOfMonth() to "dd"
                }

                val labelFormatter = DateTimeFormatter.ofPattern(labelPattern)

                for (i in 0 until periodDays) {
                    val currentDay = startDate.plusDays(i.toLong())
                    val notesForDay = notesForPeriod.filter {
                        it.noteEntryDate == currentDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    dateLabels.add(currentDay.format(labelFormatter))
                    chartEntries[0].add(notesForDay.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_POSITIVE }
                        .toFloat())
                    chartEntries[1].add(notesForDay.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_NEGATIVE }
                        .toFloat())
                    chartEntries[2].add(notesForDay.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_NEUTRAL }
                        .toFloat())
                }

                val maxDailyTotal =
                    notesForPeriod.groupBy { it.noteEntryDate }.map { it.value.size }.maxOrNull()
                        ?: 0
                val yAxisMaxItemCount = max(maxDailyTotal, 4) + 1

                DashboardUiState.Success(
                    DashboardData(
                        chartEntries = chartEntries,
                        dateLabels = dateLabels,
                        dominantMood = dominantMood,
                        totalNotes = notesForPeriod.size,
                        positiveCount = positiveCount,
                        negativeCount = negativeCount,
                        neutralCount = neutralCount,
                        yAxisMaxItemCount = yAxisMaxItemCount,
                        smartInsight = detailedInsight,
                        dominantMoodInsight = dominantInsight,
                        viewMode = mode
                    )
                )
            }
        }
    }.catch { e ->
        emit(DashboardUiState.Error("Failed to load mood data: ${e.message}"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    // --- Public functions to be called from the UI ---

    fun onDateSelected(newDate: LocalDate) {
        _displayedDate.value = newDate
    }

    fun setViewMode(newMode: ViewMode) {
        _viewMode.value = newMode
    }

    fun showPrevious() {
        _displayedDate.value = when (_viewMode.value) {
            ViewMode.WEEKLY -> _displayedDate.value.minusWeeks(1)
            ViewMode.MONTHLY -> _displayedDate.value.minusMonths(1)
        }
    }

    fun showNext() {
        val currentDate = _displayedDate.value
        val viewMode = _viewMode.value
        val today = LocalDate.now()

        // What is the START date of the period we are currently viewing?
        val startOfCurrentPeriod = when (viewMode) {
            ViewMode.WEEKLY -> currentDate.with(WeekFields.ISO.firstDayOfWeek)
            ViewMode.MONTHLY -> currentDate.withDayOfMonth(1)
        }

        // What is the START date of the period that contains TODAY?
        val startOfTodayPeriod = when (viewMode) {
            ViewMode.WEEKLY -> today.with(WeekFields.ISO.firstDayOfWeek)
            ViewMode.MONTHLY -> today.withDayOfMonth(1)
        }

        // THE CORRECT RULE: We can navigate forward ONLY IF the start of our
        // current period is strictly BEFORE the start of today's period.
        if (startOfCurrentPeriod.isBefore(startOfTodayPeriod)) {
            _displayedDate.value = when (viewMode) {
                ViewMode.WEEKLY -> currentDate.plusWeeks(1)
                ViewMode.MONTHLY -> currentDate.plusMonths(1)
            }
        }
    }

    // --- Private helper functions for data processing ---

    private fun generateDominantMoodInsight(dominantMood: String): Insight {
        val textRes = when (dominantMood) {
            SentimentAnalyzer.SENTIMENT_POSITIVE -> R.string.dashboard_summary_positive
            SentimentAnalyzer.SENTIMENT_NEGATIVE -> R.string.dashboard_summary_negative
            else -> R.string.dashboard_summary_neutral
        }
        return Insight(textRes)
    }

    private fun generateDetailedDayInsight(notesForPeriod: List<EntryEntity>): Insight {
        if (notesForPeriod.isEmpty()) return Insight(R.string.smart_insight_no_entries)

        val notesByDay = notesForPeriod.groupBy { LocalDate.parse(it.noteEntryDate) }
        val mostPositiveDay =
            notesByDay.maxByOrNull { (_, notes) -> notes.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_POSITIVE } }
        val maxPositiveCount =
            mostPositiveDay?.value?.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_POSITIVE }
                ?: 0
        val mostNegativeDay =
            notesByDay.maxByOrNull { (_, notes) -> notes.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_NEGATIVE } }
        val maxNegativeCount =
            mostNegativeDay?.value?.count { it.noteSentiment == SentimentAnalyzer.SENTIMENT_NEGATIVE }
                ?: 0
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

        return when {
            maxPositiveCount > 0 && maxPositiveCount >= maxNegativeCount -> {
                val dayName = mostPositiveDay?.key?.format(dayFormatter)
                Insight(R.string.smart_insight_most_positive, listOf(dayName.orEmpty()))
            }

            maxNegativeCount > 0 -> {
                val dayName = mostNegativeDay?.key?.format(dayFormatter)
                Insight(R.string.smart_insight_most_negative, listOf(dayName.orEmpty()))
            }

            else -> Insight(R.string.smart_insight_balanced)
        }
    }

    private fun findDominantMood(positive: Int, negative: Int, neutral: Int): String {
        val counts = mapOf("Positive" to positive, "Negative" to negative, "Neutral" to neutral)
        val maxCount = counts.values.maxOrNull() ?: 0
        if (maxCount == 0) return "Neutral"
        val topMoods = counts.filter { it.value == maxCount }.keys
        return if (topMoods.size > 1) "Neutral" else topMoods.first()
    }
}