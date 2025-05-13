package com.example.myapplication.data.remote.mapper

import com.example.myapplication.data.remote.model.VolumeItem
import com.example.myapplication.domain.model.GoogleBookDetails

fun VolumeItem.toDomain(): GoogleBookDetails? {
    val info = this.volumeInfo ?: return null
    val googleId = this.id ?: return null

    val title = info.title ?: "No Title"
    val authors = info.authors?.joinToString(", ") ?: "Unknown Author"
    val pageCount = info.pageCount ?: 0

    val isbn13 = info.industryIdentifiers?.firstOrNull { it.type == "ISBN_13" }?.identifier
    val isbn10 = info.industryIdentifiers?.firstOrNull { it.type == "ISBN_10" }?.identifier

    val finalIsbn = isbn13 ?: isbn10
    val cleanIsbn = finalIsbn?.replace("-", "")?.trim()?.takeIf { it.isNotBlank() }


    return GoogleBookDetails(
        googleVolumeId = googleId,
        isbn = cleanIsbn,
        title = title,
        authorsFormatted = authors,
        pageCount = pageCount
    )
}

fun List<VolumeItem>.toDomain(): List<GoogleBookDetails> = this.mapNotNull { it.toDomain() }