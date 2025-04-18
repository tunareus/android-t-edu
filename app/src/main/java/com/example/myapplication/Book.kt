package com.example.myapplication

import kotlinx.parcelize.Parcelize

@Parcelize
class Book(
    override val id: Int,
    override var available: Boolean,
    override val name: String,
    val pages: Int,
    val author: String
) : LibraryItem(id, available, name), HomeLendable, InLibraryUse, Digitizable {

    override fun getDetailedInfo(): String {
        return "книга: $name ($pages стр.) автора: $author с id: $id доступна: ${if (available) "Да" else "Нет"}"
    }

    override fun takeHomeAction() {
        available = false
    }

    override fun readInLibraryAction() {
        available = false
    }

    override fun getDigitizableName(): String = name
}