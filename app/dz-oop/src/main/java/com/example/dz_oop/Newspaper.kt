package com.example.dz_oop

class Newspaper(
    id: Int,
    available: Boolean,
    name: String,
    private val issueNumber: Int,
    private val month: Month
) : LibraryItem(id, available, name), InLibraryUse, Digitizable {

    override fun getDetailedInfo(): String {
        return "выпуск: $issueNumber газеты $name, месяц: ${month.displayName} с id: $id доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun readInLibraryAction() {
        available = false
    }

    override fun getDigitizableName(): String = name
}