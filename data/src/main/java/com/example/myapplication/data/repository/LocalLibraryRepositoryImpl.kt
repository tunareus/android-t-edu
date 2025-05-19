package com.example.myapplication.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.myapplication.data.local.db.LibraryItemDao
import com.example.myapplication.data.local.mapper.toDomain
import com.example.myapplication.data.local.mapper.toEntity
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.SortField
import com.example.myapplication.domain.model.SortOrder
import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.domain.repository.LocalLibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalLibraryRepositoryImpl(
    private val libraryItemDao: LibraryItemDao
) : LocalLibraryRepository {

    override suspend fun getPagedItems(
        limit: Int,
        offset: Int,
        sortPreference: SortPreference
    ): List<LibraryItem> = withContext(Dispatchers.IO) {
        val field = when (sortPreference.field) {
            SortField.NAME -> "name"
            SortField.DATE_ADDED -> "dateAdded"
        }
        val order = when (sortPreference.order) {
            SortOrder.ASC -> "ASC"
            SortOrder.DESC -> "DESC"
        }
        val orderByClause = if (sortPreference.field == SortField.NAME) "$field COLLATE NOCASE $order" else "$field $order"
        val query = "SELECT * FROM library_items ORDER BY $orderByClause LIMIT $limit OFFSET $offset"

        try {
            libraryItemDao.getPagedItemsRaw(SimpleSQLiteQuery(query)).map { it.toDomain() }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun addItem(item: LibraryItem, isbnValue: String?): Long = withContext(Dispatchers.IO) {
        try {
            val entity = item.toEntity(isbnValue = isbnValue)
            libraryItemDao.insert(entity)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun removeItem(item: LibraryItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val affectedRows = libraryItemDao.deleteById(item.id)
            affectedRows > 0
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getItemById(id: Int): LibraryItem? = withContext(Dispatchers.IO) {
        try {
            libraryItemDao.getItemById(id)?.toDomain()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getTotalItemCount(): Int = withContext(Dispatchers.IO) {
        try {
            libraryItemDao.getCount()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun findByIsbn(isbn: String): LibraryItem? = withContext(Dispatchers.IO) {
        try {
            libraryItemDao.findByIsbn(isbn)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByNameAndAuthor(name: String, author: String): List<LibraryItem> = withContext(Dispatchers.IO) {
        try {
            libraryItemDao.findByNameAndAuthor(name, author).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteAllItems(): Unit = withContext(Dispatchers.IO) {
        libraryItemDao.deleteAll()
    }

    override suspend fun insertAllItems(items: List<LibraryItem>): Unit = withContext(Dispatchers.IO) {
        libraryItemDao.insertAll(items.map { it.toEntity(isbnValue = null) })
    }
}