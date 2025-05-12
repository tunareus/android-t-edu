package com.example.myapplication.domain.model

enum class SortField { NAME, DATE_ADDED }
enum class SortOrder { ASC, DESC }

data class SortPreference(
    val field: SortField = SortField.DATE_ADDED,
    val order: SortOrder = SortOrder.DESC
) {
    //
}