package com.example.myapplication

import kotlinx.parcelize.Parcelize

@Parcelize
class Newspaper(
    override val id: Int,
    override var available: Boolean,
    override val name: String,
    val issueNumber: Int,
    val month: Month
) : LibraryItem(id, available, name), InLibraryUse, Digitizable {

    override fun getDetailedInfo(): String {
        return "выпуск: $issueNumber газеты $name, месяц: ${month.displayName} с id: $id доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun readInLibraryAction() {
        available = false
    }

    override fun getDigitizableName(): String = name
}