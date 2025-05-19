package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.GoogleBooksApiService
import com.example.myapplication.data.remote.mapper.toDomain
import com.example.myapplication.domain.model.GoogleBookDetails
import com.example.myapplication.domain.repository.GoogleBooksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class GoogleBooksRepositoryImpl(
    private val googleBooksApiService: GoogleBooksApiService
) : GoogleBooksRepository {

    override suspend fun searchBooks(
        authorQuery: String?,
        titleQuery: String?,
        maxResults: Int
    ): List<GoogleBookDetails> = withContext(Dispatchers.IO) {
        val queryParts = mutableListOf<String>()
        if (!authorQuery.isNullOrBlank()) queryParts.add("inauthor:\"$authorQuery\"")
        if (!titleQuery.isNullOrBlank()) queryParts.add("intitle:\"$titleQuery\"")
        val combinedQuery = queryParts.joinToString("+")

        if (combinedQuery.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val response = googleBooksApiService.searchBooks(
                query = combinedQuery,
                maxResults = maxResults
            )
            response.items?.toDomain() ?: emptyList()
        } catch (e: HttpException) {
            throw IOException("HTTP error ${e.code()}: ${e.message()}", e)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Error fetching Google Books: ${e.localizedMessage}", e)
        }
    }
}