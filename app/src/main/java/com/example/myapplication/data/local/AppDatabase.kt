package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.LibraryItem
import com.example.myapplication.data.local.dao.LibraryItemDao
import com.example.myapplication.data.local.mapper.toEntity
import com.example.myapplication.data.local.model.LibraryItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun initializeLibraryStub(): List<LibraryItem> {
    return emptyList()
}

@Database(entities = [LibraryItemEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun libraryItemDao(): LibraryItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "library_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(LibraryDatabaseCallback(scope))
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class LibraryDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.libraryItemDao())
                }
            }
        }

        suspend fun populateDatabase(libraryItemDao: LibraryItemDao) {
            libraryItemDao.deleteAll()
            val initialItems = initializeLibraryStub().map {
                it.toEntity(id = 0, isbnValue = null)
            }
            libraryItemDao.insertAll(initialItems)
        }
    }
}