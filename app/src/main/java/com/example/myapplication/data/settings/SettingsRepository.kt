package com.example.myapplication.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    suspend fun saveSortPreference(preference: SortPreference) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString(SortPreference.PREF_KEY_SORT_FIELD, preference.field.name)
            putString(SortPreference.PREF_KEY_SORT_ORDER, preference.order.name)
        }
    }

    suspend fun getSortPreference(): SortPreference = withContext(Dispatchers.IO) {
        val fieldStr = prefs.getString(SortPreference.PREF_KEY_SORT_FIELD, SortField.DATE_ADDED.name)
        val orderStr = prefs.getString(SortPreference.PREF_KEY_SORT_ORDER, SortOrder.DESC.name)

        SortPreference(
            field = SortField.entries.firstOrNull { it.name == fieldStr } ?: SortField.DATE_ADDED,
            order = SortOrder.entries.firstOrNull { it.name == orderStr } ?: SortOrder.DESC
        )
    }
}