package com.example.myapplication.data.mapper

import com.example.myapplication.Book
import com.example.myapplication.LibraryItem
import com.example.myapplication.data.remote.model.VolumeItem
import com.example.myapplication.domain.GoogleBookItem

fun VolumeItem.toDomain(): GoogleBookItem? {
    val info = this.volumeInfo ?: return null
    val googleId = this.id ?: return null

    val title = info.title ?: "Без названия"
    val authors = info.authors?.joinToString(", ") ?: "Неизвестный автор"
    val pageCount = info.pageCount ?: 0

    val isbn = info.industryIdentifiers?.firstNotNullOfOrNull {
        if (it.type == "ISBN_13" && !it.identifier.isNullOrBlank()) it.identifier else null
    } ?: info.industryIdentifiers?.firstNotNullOfOrNull {
        if (it.type == "ISBN_10" && !it.identifier.isNullOrBlank()) it.identifier else null
    }
    val cleanIsbn = isbn?.replace("-", "")

    return GoogleBookItem(
        googleVolumeId = googleId,
        isbn = cleanIsbn,
        title = title,
        authorsFormatted = authors,
        pageCount = pageCount
    )
}


fun GoogleBookItem.toLibraryItem(): LibraryItem {

    return Book(
        id = 0,
        name = this.title,
        available = true,
        pages = this.pageCount,
        author = this.authorsFormatted
    )
}