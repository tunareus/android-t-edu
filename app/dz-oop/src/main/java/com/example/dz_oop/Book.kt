package com.example.dz_oop

class Book(id: Int, available: Boolean, name: String, val pages: Int, val author: String) :
    LibraryItem(id, available, name) {

    override fun getDetailedInfo(): String {
        return "книга: $name ($pages стр.) автора: $author с id: $id доступна: ${if (available) "Да" else "Нет"}"
    }

    override fun canTakeHome(): Boolean = true
    override fun canReadInLibrary(): Boolean = true
}