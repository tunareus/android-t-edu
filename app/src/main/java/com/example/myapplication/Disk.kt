package com.example.myapplication

import kotlinx.parcelize.Parcelize

@Parcelize
class Disk(
    override val id: Int,
    override var available: Boolean,
    override val name: String,
    private val diskType: String
) : LibraryItem(id, available, name), HomeLendable {

    override fun getDetailedInfo(): String {
        return "$diskType $name доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun takeHomeAction() {
        available = false
    }

    fun getDiskType(): String = diskType
}