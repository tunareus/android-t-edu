package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.myapplication.data.local.model.LibraryItemEntity

@Dao
interface LibraryItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LibraryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LibraryItemEntity>)

    @Query("DELETE FROM library_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Int): Int

    @Query("SELECT COUNT(id) FROM library_items")
    suspend fun getCount(): Int

    @Query("SELECT * FROM library_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): LibraryItemEntity?

    @RawQuery
    suspend fun getPagedItemsRaw(query: SupportSQLiteQuery): List<LibraryItemEntity>

    suspend fun getPagedItems(limit: Int, offset: Int, sortBy: String, sortOrder: String): List<LibraryItemEntity> {
        val validSortBy = when (sortBy.lowercase()) {
            "name" -> "name"
            "dateadded" -> "dateAdded"
            else -> "dateAdded"
        }
        val validSortOrder = if (sortOrder.equals("DESC", ignoreCase = true)) "DESC" else "ASC"

        val query = "SELECT * FROM library_items ORDER BY $validSortBy $validSortOrder LIMIT $limit OFFSET $offset"
        return getPagedItemsRaw(SimpleSQLiteQuery(query))
    }

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}