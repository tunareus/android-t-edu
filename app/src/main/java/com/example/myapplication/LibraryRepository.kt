package com.example.myapplication

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.myapplication.data.local.dao.LibraryItemDao
import com.example.myapplication.data.local.mapper.toDomain
import com.example.myapplication.data.local.mapper.toEntity
import com.example.myapplication.data.settings.SortField
import com.example.myapplication.data.settings.SortOrder
import com.example.myapplication.data.settings.SortPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepositoryLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class LibraryRepository(private val libraryItemDao: LibraryItemDao) {

    suspend fun getPagedItems(
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
        val query = "SELECT * FROM library_items ORDER BY $field $order LIMIT $limit OFFSET $offset"
        try {
            libraryItemDao.getPagedItemsRaw(SimpleSQLiteQuery(query)).map { it.toDomain() }
        } catch (e: Exception) {
            throw RepositoryLoadException("Failed to load items from database", e)
        }
    }

    suspend fun addItem(item: LibraryItem): Long = withContext(Dispatchers.IO) {
        try {
            val entity = item.toEntity(id = 0)
            libraryItemDao.insert(entity)
        } catch (e: Exception) {
            throw RepositoryLoadException("Failed to add item to database", e)
        }
    }

    suspend fun removeItem(item: LibraryItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val affectedRows = libraryItemDao.deleteById(item.id)
            return@withContext affectedRows > 0
        } catch (e: Exception) {
            throw RepositoryLoadException("Failed to remove item from database", e)
        }
    }

    suspend fun getItemById(id: Int): LibraryItem? = withContext(Dispatchers.IO) {
        try {
            libraryItemDao.getItemById(id)?.toDomain()
        } catch (e: Exception) {
            throw RepositoryLoadException("Failed to get item by ID from database", e)
        }
    }

    suspend fun getTotalItemCount(): Int = withContext(Dispatchers.IO) {
        try {
            libraryItemDao.getCount()
        } catch (e: Exception) {
            throw RepositoryLoadException("Failed to get item count from database", e)
        }
    }
}