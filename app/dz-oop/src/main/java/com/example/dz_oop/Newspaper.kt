package com.example.dz_oop

class Newspaper(id: Int, available: Boolean, name: String, private val issueNumber: Int) :
    LibraryItem(id, available, name) {

    override fun getDetailedInfo(): String {
        return "выпуск: $issueNumber газеты $name с id: $id доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun canReadInLibrary(): Boolean = true
}