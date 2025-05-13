package com.example.myapplication.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.myapplication.domain.model.SortField
import com.example.myapplication.domain.model.SortOrder
import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val PREF_KEY_SORT_FIELD = "sort_field_v2"
internal const val PREF_KEY_SORT_ORDER = "sort_order_v2"
private const val APP_SETTINGS_PREFS_NAME = "app_settings_prefs"


class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun saveSortPreference(preference: SortPreference) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString(PREF_KEY_SORT_FIELD, preference.field.name)
            putString(PREF_KEY_SORT_ORDER, preference.order.name)
        }
    }

    override suspend fun getSortPreference(): SortPreference = withContext(Dispatchers.IO) {
        val fieldStr = prefs.getString(PREF_KEY_SORT_FIELD, SortField.DATE_ADDED.name)
        val orderStr = prefs.getString(PREF_KEY_SORT_ORDER, SortOrder.DESC.name)

        SortPreference(
            field = SortField.entries.firstOrNull { it.name == fieldStr } ?: SortField.DATE_ADDED,
            order = SortOrder.entries.firstOrNull { it.name == orderStr } ?: SortOrder.DESC
        )
    }
}