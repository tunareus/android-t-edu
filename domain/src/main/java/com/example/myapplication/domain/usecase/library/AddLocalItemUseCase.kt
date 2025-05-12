package com.example.myapplication.domain.usecase.library

import com.example.myapplication.domain.model.Book
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.repository.LocalLibraryRepository

class AddLocalItemUseCase(private val repository: LocalLibraryRepository) {
    suspend operator fun invoke(item: LibraryItem): Result<Long> {
        return try {
            val isbn = if (item is Book) {
                null
            } else null
            Result.success(repository.addItem(item, isbn))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}