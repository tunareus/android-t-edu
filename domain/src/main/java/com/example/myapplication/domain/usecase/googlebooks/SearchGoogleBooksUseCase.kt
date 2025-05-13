package com.example.myapplication.domain.usecase.googlebooks

import com.example.myapplication.domain.model.GoogleBookDetails
import com.example.myapplication.domain.repository.GoogleBooksRepository

class SearchGoogleBooksUseCase(private val repository: GoogleBooksRepository) {
    suspend operator fun invoke(
        authorQuery: String?,
        titleQuery: String?,
        maxResults: Int
    ): Result<List<GoogleBookDetails>> {
        return try {
            if ((authorQuery.isNullOrBlank() || authorQuery.length < 3) &&
                (titleQuery.isNullOrBlank() || titleQuery.length < 3)) {
                return Result.failure(IllegalArgumentException("Author or title query must be at least 3 characters long."))
            }
            Result.success(repository.searchBooks(authorQuery, titleQuery, maxResults))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}