package com.example.myapplication.data.local.mapper

import com.example.myapplication.data.local.model.ItemType
import com.example.myapplication.data.local.model.LibraryItemEntity
import com.example.myapplication.domain.model.Book
import com.example.myapplication.domain.model.Disk
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.Month
import com.example.myapplication.domain.model.Newspaper

fun LibraryItem.toEntity(specificId: Int? = null, isbnValue: String? = null, dateAddedTimestamp: Long? = null): LibraryItemEntity {
    val entityId = specificId ?: this.id.takeIf { it != 0 } ?: 0
    val currentTime = dateAddedTimestamp ?: System.currentTimeMillis()

    return when (this) {
        is Book -> LibraryItemEntity(
            id = entityId, available = this.available, name = this.name, type = ItemType.BOOK, dateAdded = currentTime,
            pages = this.pages, author = this.author, issueNumber = null, monthDisplayName = null, diskType = null,
            isbn = isbnValue
        )
        is Newspaper -> LibraryItemEntity(
            id = entityId, available = this.available, name = this.name, type = ItemType.NEWSPAPER, dateAdded = currentTime,
            pages = null, author = null, issueNumber = this.issueNumber, monthDisplayName = this.month.displayName, diskType = null,
            isbn = null
        )
        is Disk -> LibraryItemEntity(
            id = entityId, available = this.available, name = this.name, type = ItemType.DISK, dateAdded = currentTime,
            pages = null, author = null, issueNumber = null, monthDisplayName = null, diskType = this.getDiskType(),
            isbn = null
        )
    }
}

fun LibraryItemEntity.toDomain(): LibraryItem {
    return when (this.type) {
        ItemType.BOOK -> Book(
            id = this.id, available = this.available, name = this.name,
            pages = this.pages ?: 0,
            author = this.author ?: "Unknown Author"
        )
        ItemType.NEWSPAPER -> Newspaper(
            id = this.id, available = this.available, name = this.name,
            issueNumber = this.issueNumber ?: 0,
            month = Month.entries.firstOrNull { it.displayName == this.monthDisplayName } ?: Month.JANUARY
        )
        ItemType.DISK -> Disk(
            id = this.id, available = this.available, name = this.name,
            diskType = this.diskType ?: "Unknown Type"
        )
    }
}

fun List<LibraryItemEntity>.toDomain(): List<LibraryItem> = this.map { it.toDomain() }
fun List<LibraryItem>.toEntity(dateAddedTimestamp: Long? = null): List<LibraryItemEntity> = this.map { it.toEntity(dateAddedTimestamp = dateAddedTimestamp) }