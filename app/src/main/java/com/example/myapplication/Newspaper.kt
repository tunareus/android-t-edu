package com.example.myapplication

class Newspaper(
    id: Int,
    available: Boolean,
    name: String,
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