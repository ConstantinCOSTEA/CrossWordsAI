package fr.miage.m1.crosswordsai.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data class representing a saved grid
 */
@Serializable
data class SavedGridData(
    val width: Int,
    val height: Int,
    val cells: List<SavedCell>,
    val xAxisType: String,
    val yAxisType: String,
    val solvedCount: Int,
    val totalCount: Int
)

@Serializable
data class SavedCell(
    val x: Int,
    val y: Int,
    val char: Char?,
    val number: Int?,
    val isEmpty: Boolean
)

/**
 * History entry with metadata
 */
@Serializable
data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val gridData: SavedGridData
)

/**
 * Repository for managing crossword history using SharedPreferences
 */
class HistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    companion object {
        private const val PREFS_NAME = "crossword_history"
        private const val KEY_HISTORY = "history_entries"
        private const val MAX_ENTRIES = 20
    }

    /**
     * Get all history entries sorted by timestamp (most recent first)
     */
    fun getHistory(): List<HistoryEntry> {
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<HistoryEntry>>(jsonString)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save a new grid to history
     */
    fun saveGrid(gridData: SavedGridData): String {
        val entries = getHistory().toMutableList()
        
        val newEntry = HistoryEntry(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            gridData = gridData
        )
        
        entries.add(0, newEntry)
        
        // Keep only the most recent entries
        val trimmedEntries = entries.take(MAX_ENTRIES)
        
        val jsonString = json.encodeToString(trimmedEntries)
        prefs.edit { putString(KEY_HISTORY, jsonString) }
        
        return newEntry.id
    }

    /**
     * Delete an entry by ID
     */
    fun deleteEntry(id: String) {
        val entries = getHistory().filter { it.id != id }
        val jsonString = json.encodeToString(entries)
        prefs.edit { putString(KEY_HISTORY, jsonString) }
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        prefs.edit { remove(KEY_HISTORY) }
    }

    /**
     * Get a specific entry by ID
     */
    fun getEntry(id: String): HistoryEntry? {
        return getHistory().find { it.id == id }
    }
}
