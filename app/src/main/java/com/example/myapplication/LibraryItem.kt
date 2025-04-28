package com.example.myapplication

import android.os.Parcelable

abstract class LibraryItem(
    open val id: Int,
    open var available: Boolean,
    open val name: String
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false
        other as LibraryItem
        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}