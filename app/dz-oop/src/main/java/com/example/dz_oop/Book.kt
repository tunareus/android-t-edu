package com.example.dz_oop

class Book(id: Int, available: Boolean, name: String, private val pages: Int, private val author: String) :
    LibraryItem(id, available, name), HomeLendable, InLibraryUse {

    override fun getDetailedInfo(): String {
        return "книга: $name ($pages стр.) автора: $author с id: $id доступна: ${if (available) "Да" else "Нет"}"
    }

    override fun takeHomeAction() {
        available = false
    }

    override fun readInLibraryAction() {
        available = false
    }
}