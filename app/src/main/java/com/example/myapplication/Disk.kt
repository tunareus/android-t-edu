package com.example.myapplication

class Disk(id: Int, available: Boolean, name: String, private val diskType: String) :
    LibraryItem(id, available, name), HomeLendable {

    override fun getDetailedInfo(): String {
        return "$diskType $name доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun takeHomeAction() {
        available = false
    }
}