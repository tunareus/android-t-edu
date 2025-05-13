package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.GoogleBookDetails

interface GoogleBooksRepository {
    suspend fun searchBooks(
        authorQuery: String?,
        titleQuery: String?,
        maxResults: Int
    ): List<GoogleBookDetails>
}