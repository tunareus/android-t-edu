package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.SortPreference

interface LocalLibraryRepository {
    suspend fun getPagedItems(limit: Int, offset: Int, sortPreference: SortPreference): List<LibraryItem>
    suspend fun addItem(item: LibraryItem, isbnValue: String? = null): Long // Добавил isbnValue, если нужно при создании
    suspend fun removeItem(item: LibraryItem): Boolean
    suspend fun getItemById(id: Int): LibraryItem?
    suspend fun getTotalItemCount(): Int
    suspend fun findByIsbn(isbn: String): LibraryItem?
    suspend fun findByNameAndAuthor(name: String, author: String): List<LibraryItem>
    suspend fun deleteAllItems() // Добавил для populateDatabase
    suspend fun insertAllItems(items: List<LibraryItem>) // Добавил для populateDatabase
}