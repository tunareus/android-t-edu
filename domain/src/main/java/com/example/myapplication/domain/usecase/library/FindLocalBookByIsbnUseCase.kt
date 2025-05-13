package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.repository.LocalLibraryRepository

class FindLocalBookByIsbnUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(isbn: String): Result<LibraryItem?> {
        return try {
            if (isbn.isBlank()) {
                return Result.failure(IllegalArgumentException("ISBN cannot be blank."))
            }
            Result.success(repository.findByIsbn(isbn))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}