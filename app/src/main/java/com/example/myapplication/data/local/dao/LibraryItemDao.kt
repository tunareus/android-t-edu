package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
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

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}