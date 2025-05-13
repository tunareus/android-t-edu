package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.SortPreference

interface SettingsRepository {
    suspend fun saveSortPreference(preference: SortPreference)
    suspend fun getSortPreference(): SortPreference
}