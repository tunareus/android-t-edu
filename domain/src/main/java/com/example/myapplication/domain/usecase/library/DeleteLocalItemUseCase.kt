package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.repository.LocalLibraryRepository

class DeleteLocalItemUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(item: LibraryItem): Result<Boolean> {
        return try {
            Result.success(repository.removeItem(item))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}