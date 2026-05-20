package com.busraknya.moodjournal.ui.screen.editor

import com.busraknya.moodjournal.data.database.EntryEntity

/**
 * Represents the complete UI state for the entry editor screen.
 *
 * This data class acts as a single source of truth for the editor's UI. It holds the current
 * values of the input fields and also contains derived state logic to determine UI behavior,
 * such as whether the "Save" button should be enabled or if there are unsaved changes.
 *
 * @param id The unique ID of the entry being edited, or null if creating a new entry.
 * @param title The current text in the title field.
 * @param subtitle The current text in the subtitle field.
 * @param description The current text in the description field.
 * @param imageUri The URI of the attached image, as a String.
 * @param originalEntry The initial state of the entry when the screen was opened. This is used
 *                      to detect if any changes have been made. It's private to prevent
 *                      accidental modification from outside the class.
 */
data class EntryEditorUiState(
    val id: String? = null,
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val imageUri: String? = null,
    private val originalEntry: EntryEntity? = null
) {
    /**
     * A derived property that computes whether there are any unsaved changes.
     * This is used to warn the user before they navigate away with unsaved work.
     * The logic handles both new entries and existing ones.
     */
    val hasUnsavedChanges: Boolean
        get() {
            return if (originalEntry == null) {
                // For a new entry, any text input or an attached image counts as a change.
                isDirty()
            } else {
                // For an existing entry, compare current fields with the original state.
                title != originalEntry.noteTitle ||
                        subtitle != originalEntry.noteSubtitle ||
                        description != originalEntry.noteDescription ||
                        imageUri != originalEntry.noteImagePath
            }
        }

    /**
     * A derived property that determines if the "Save" button should be enabled.
     * The business rule is that an entry must have at least a title and a description to be saved.
     */
    val isSaveButtonEnabled: Boolean
        get() = title.isNotBlank() && description.isNotBlank()

    /**
     * Helper function to check if a new, unsaved entry contains any data.
     */
    private fun isDirty(): Boolean {
        return title.isNotBlank() ||
                subtitle.isNotBlank() ||
                description.isNotBlank() ||
                imageUri != null
    }
}