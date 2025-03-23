package com.example.dz_oop

class DigitizationCabinet {
    fun <T> digitize(item: T): Disk where T : LibraryItem {
        if (item !is Book && item !is Newspaper) {
            throw IllegalArgumentException("Можно оцифровать только книги и газеты")
        }

        return Disk(
            id = (10000..99999).random(),
            available = true,
            name = "Цифровая копия: ${item.name}",
            diskType = "CD"
        )
    }
}