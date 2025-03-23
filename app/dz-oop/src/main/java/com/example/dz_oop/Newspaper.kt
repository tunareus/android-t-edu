package com.example.dz_oop

class Newspaper(
    id: Int,
    available: Boolean,
    name: String,
    private val issueNumber: Int,
    private val month: Int
) : LibraryItem(id, available, name), InLibraryUse {

    private fun getMonthName(): String {
        return when (month) {
            1 -> "Январь"
            2 -> "Февраль"
            3 -> "Март"
            4 -> "Апрель"
            5 -> "Май"
            6 -> "Июнь"
            7 -> "Июль"
            8 -> "Август"
            9 -> "Сентябрь"
            10 -> "Октябрь"
            11 -> "Ноябрь"
            12 -> "Декабрь"
            else -> "Неизвестный месяц"
        }
    }

    override fun getDetailedInfo(): String {
        return "выпуск: $issueNumber газеты $name, месяц: ${getMonthName()} с id: $id доступен: ${if (available) "Да" else "Нет"}"
    }

    override fun readInLibraryAction() {
        available = false
    }
}