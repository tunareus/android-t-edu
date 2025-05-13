package com.example.myapplication.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GoogleBookDetails(
    val googleVolumeId: String,
    val isbn: String?,
    val title: String,
    val authorsFormatted: String,
    val pageCount: Int
) : Parcelable