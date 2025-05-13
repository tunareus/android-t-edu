package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.repository.LocalLibraryRepository

class GetTotalLocalItemCountUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(): Result<Int> {
        return try {
            Result.success(repository.getTotalItemCount())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}