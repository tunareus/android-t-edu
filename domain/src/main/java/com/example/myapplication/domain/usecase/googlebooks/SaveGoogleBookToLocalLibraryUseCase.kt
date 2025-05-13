package com.example.myapplication.domain.usecase.googlebooks

import com.example.myapplication.domain.model.Book
import com.example.myapplication.domain.model.GoogleBookDetails
import com.example.myapplication.domain.repository.LocalLibraryRepository

class SaveGoogleBookToLocalLibraryUseCase(
    private val localLibraryRepository: LocalLibraryRepository
) {
    sealed class SaveResult {
        data class Success(val itemId: Long, val bookTitle: String) : SaveResult()
        data class AlreadyExists(val bookTitle: String, val reason: String) : SaveResult()
        data class Failure(val error: Throwable) : SaveResult()
    }

    suspend operator fun invoke(googleBook: GoogleBookDetails): SaveResult {
        return try {
            if (!googleBook.isbn.isNullOrBlank()) {
                localLibraryRepository.findByIsbn(googleBook.isbn)?.let {
                    return SaveResult.AlreadyExists(googleBook.title, "ISBN ${googleBook.isbn} already exists (ID: ${it.id})")
                }
            }

            val existingByNameAuthor = localLibraryRepository.findByNameAndAuthor(googleBook.title, googleBook.authorsFormatted)
            if (existingByNameAuthor.isNotEmpty()) {
                return SaveResult.AlreadyExists(googleBook.title, "Title and author match existing item (ID: ${existingByNameAuthor.first().id})")
            }

            val bookToSave = Book(
                id = 0,
                name = googleBook.title,
                available = true,
                pages = googleBook.pageCount,
                author = googleBook.authorsFormatted
            )
            val newId = localLibraryRepository.addItem(bookToSave, googleBook.isbn)
            SaveResult.Success(newId, googleBook.title)
        } catch (e: Exception) {
            SaveResult.Failure(e)
        }
    }
}