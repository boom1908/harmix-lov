package com.boom.harmix.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Recent search terms, kept locally so the search bar can suggest them again. */
@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("harmix_search_history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(read())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun add(term: String) {
        val clean = term.trim()
        if (clean.isEmpty()) return
        val updated = (listOf(clean) + _history.value.filterNot { it.equals(clean, ignoreCase = true) })
            .take(MAX_ENTRIES)
        persist(updated)
    }

    fun remove(term: String) {
        persist(_history.value.filterNot { it.equals(term, ignoreCase = true) })
    }

    fun clear() = persist(emptyList())

    private fun persist(list: List<String>) {
        _history.value = list
        prefs.edit().putString(KEY, list.joinToString(SEPARATOR)).apply()
    }

    private fun read(): List<String> =
        prefs.getString(KEY, "")
            ?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.take(MAX_ENTRIES)
            .orEmpty()

    private companion object {
        const val KEY = "terms"
        const val SEPARATOR = "\u001F"
        const val MAX_ENTRIES = 12
    }
}
