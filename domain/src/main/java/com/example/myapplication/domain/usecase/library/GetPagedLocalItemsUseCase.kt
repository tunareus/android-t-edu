package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.domain.repository.LocalLibraryRepository

class GetPagedLocalItemsUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(
        limit: Int,
        offset: Int,
        sortPreference: SortPreference
    ): Result<List<LibraryItem>> {
        return try {
            Result.success(repository.getPagedItems(limit, offset, sortPreference))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}