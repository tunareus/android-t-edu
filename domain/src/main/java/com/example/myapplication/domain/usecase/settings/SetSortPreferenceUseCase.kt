package com.example.myapplication.domain.usecase.settings

import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.domain.repository.SettingsRepository

class SetSortPreferenceUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(preference: SortPreference): Result<Unit> {
        return try {
            repository.saveSortPreference(preference)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}