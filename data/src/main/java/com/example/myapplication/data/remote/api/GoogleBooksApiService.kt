package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.model.GoogleBooksResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {

    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("fields") fields: String = "items(id,volumeInfo(title,authors,pageCount,industryIdentifiers))"
    ): GoogleBooksResponse
}