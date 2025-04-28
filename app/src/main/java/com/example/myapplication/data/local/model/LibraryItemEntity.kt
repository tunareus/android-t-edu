package com.example.myapplication.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemType { BOOK, NEWSPAPER, DISK }

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(defaultValue = "true")
    var available: Boolean,

    val name: String,

    @ColumnInfo(index = true)
    val type: ItemType,

    @ColumnInfo(index = true, defaultValue = "0")
    val dateAdded: Long,

    val pages: Int?,
    val author: String?,

    val issueNumber: Int?,
    val monthDisplayName: String?,

    val diskType: String?
)