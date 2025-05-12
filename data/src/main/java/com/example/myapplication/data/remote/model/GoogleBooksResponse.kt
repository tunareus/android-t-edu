package com.example.myapplication.data.remote.model

data class GoogleBooksResponse(
    val items: List<VolumeItem>?
)

data class VolumeItem(
    val id: String?,
    val volumeInfo: VolumeInfo?
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val pageCount: Int?,
    val industryIdentifiers: List<IndustryIdentifier>?
)

data class IndustryIdentifier(
    val type: String?,
    val identifier: String?
)