package com.example.myapplication.data.local.mapper

import com.example.myapplication.Book
import com.example.myapplication.Disk
import com.example.myapplication.LibraryItem
import com.example.myapplication.Month
import com.example.myapplication.Newspaper
import com.example.myapplication.data.local.model.ItemType
import com.example.myapplication.data.local.model.LibraryItemEntity

fun LibraryItem.toEntity(id: Int? = null): LibraryItemEntity {
    val entityId = id ?: this.id.takeIf { it != 0 } ?: 0
    val currentTime = System.currentTimeMillis()

    return when (this) {
        is Book -> LibraryItemEntity(
            id = entityId, available = this.available, name = this.name, type = ItemType.BOOK, dateAdded = currentTime,
            pages = this.pages, author = this.author, issueNumber = null, monthDisplayName = null, diskType = null
        )
        is Newspaper -> LibraryItemEntity(
            id = entityId, available = this.available, name = this.name, type = ItemType.NEWSPAPER, dateAdded = currentTime,
            pages = null, author = null, issueNumber = this.issueNumber, monthDisplayName = this.month.displayName, diskType = null
        )
        is Disk -> LibraryItemEntity(
            id = entityId, available = this.available, name = this.name, type = ItemType.DISK, dateAdded = currentTime,
            pages = null, author = null, issueNumber = null, monthDisplayName = null, diskType = this.getDiskType()
        )
    }
}

fun LibraryItemEntity.toDomain(): LibraryItem {
    return when (this.type) {
        ItemType.BOOK -> Book(
            id = this.id, available = this.available, name = this.name,
            pages = this.pages ?: 0,
            author = this.author ?: ""
        )
        ItemType.NEWSPAPER -> Newspaper(
            id = this.id, available = this.available, name = this.name,
            issueNumber = this.issueNumber ?: 0,
            month = Month.entries.firstOrNull { it.displayName == this.monthDisplayName } ?: Month.JANUARY
        )
        ItemType.DISK -> Disk(
            id = this.id, available = this.available, name = this.name,
            diskType = this.diskType ?: ""
        )
    }
}