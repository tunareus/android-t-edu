package com.example.myapplication.data.settings

enum class SortField { NAME, DATE_ADDED }
enum class SortOrder { ASC, DESC }

data class SortPreference(
    val field: SortField = SortField.DATE_ADDED,
    val order: SortOrder = SortOrder.DESC
) {
    companion object {
        const val PREF_KEY_SORT_FIELD = "sort_field"
        const val PREF_KEY_SORT_ORDER = "sort_order"
    }
}