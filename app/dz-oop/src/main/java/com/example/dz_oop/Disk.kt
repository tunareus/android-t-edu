package com.example.dz_oop

class Disk(id: Int, available: Boolean, name: String, private val diskType: String) :
    LibraryItem(id, available, name) {

    override fun getDetailedInfo(): String {
        return "$diskType $name доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun canTakeHome(): Boolean = true
}