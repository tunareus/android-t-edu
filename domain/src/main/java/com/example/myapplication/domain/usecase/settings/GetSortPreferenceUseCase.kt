package com.example.myapplication.domain.usecase.settings

import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.domain.repository.SettingsRepository

class GetSortPreferenceUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(): Result<SortPreference> {
        return try {
            Result.success(repository.getSortPreference())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}