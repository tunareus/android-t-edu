package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.repository.LocalLibraryRepository

class GetLocalItemByIdUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(id: Int): Result<LibraryItem?> {
        return try {
            Result.success(repository.getItemById(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}