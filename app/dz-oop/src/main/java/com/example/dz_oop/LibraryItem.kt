package com.example.dz_oop

abstract class LibraryItem(val id: Int, var available: Boolean, val name: String) {
    open fun getShortInfo(): String {
        return "$name доступна: ${if (available) "Да" else "Нет"}"
    }

    abstract fun getDetailedInfo(): String
}