package com.example.myapplication

import kotlinx.parcelize.Parcelize

@Parcelize
class Book(
    override val id: Int,
    override var available: Boolean,
    override val name: String,
    val pages: Int,
    val author: String
) : LibraryItem(id, available, name) { //
}