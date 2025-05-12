package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.repository.LocalLibraryRepository

class FindLocalBookByNameAndAuthorUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(name: String, author: String): Result<List<LibraryItem>> {
        return try {
            if (name.isBlank() && author.isBlank()) {
                return Result.failure(IllegalArgumentException("Name and Author cannot both be blank."))
            }
            Result.success(repository.findByNameAndAuthor(name, author))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}