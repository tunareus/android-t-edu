package com.example.myapplication

import java.io.Serializable

abstract class LibraryItem(val id: Int, var available: Boolean, val name: String) : Serializable {
    open fun getShortInfo(): String {
        return "$name доступна: ${if (available) "Да" else "Нет"}"
    }
    abstract fun getDetailedInfo(): String
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LibraryItem) return false
        return id == other.id && available == other.available && name == other.name
    }
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + available.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}