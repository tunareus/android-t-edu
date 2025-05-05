package com.example.myapplication.domain

data class GoogleBookItem(
    val googleVolumeId: String,
    val isbn: String?,
    val title: String,
    val authorsFormatted: String,
    val pageCount: Int
)