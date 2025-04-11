package com.example.myapplication

import android.os.Parcelable

abstract class LibraryItem(
    open val id: Int,
    open var available: Boolean,
    open val name: String
) : Parcelable {
    abstract fun getDetailedInfo(): String

    open fun getShortInfo(): String {
        return "$name доступна: ${if (available) "Да" else "Нет"}"
    }

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