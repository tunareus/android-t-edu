package com.example.dz_oop

import java.util.Scanner

abstract class LibraryItem(val id: Int, var available: Boolean, val name: String) {
    abstract fun getShortInfo(): String
    abstract fun getDetailedInfo(): String
}

class Book(id: Int, available: Boolean, name: String, val pages: Int, val author: String) :
    LibraryItem(id, available, name) {

    override fun getShortInfo(): String {
        return "$name доступна: ${if (available) "Да" else "Нет"}"
    }

    override fun getDetailedInfo(): String {
        return "книга: $name ($pages стр.) автора: $author с id: $id доступна: ${if (available) "Да" else "Нет"}"
    }
}

class Newspaper(id: Int, available: Boolean, name: String, val issueNumber: Int) :
    LibraryItem(id, available, name) {

    override fun getShortInfo(): String {
        return "$name доступна: ${if (available) "Да" else "Нет"}"
    }

    override fun getDetailedInfo(): String {
        return "выпуск: $issueNumber газеты $name с id: $id доступен: ${if (available) "Да" else "Нет"}"
    }
}

class Disk(id: Int, available: Boolean, name: String, val diskType: String) :
    LibraryItem(id, available, name) {

    override fun getShortInfo(): String {
        return "$name доступна: ${if (available) "Да" else "Нет"}"
    }

    override fun getDetailedInfo(): String {
        return "$diskType $name доступен: ${if (available) "Да" else "Нет"}"
    }
}

fun main() {
    val scanner = Scanner(System.`in`)

    val libraryItems = mutableListOf<LibraryItem>(
        Book(90743, true, "Маугли", 202, "Джозеф Киплинг"),
        Newspaper(17245, true, "Сельская жизнь", 794),
        Disk(33456, true, "Дэдпул и Росомаха", "DVD"),
        Book(11223, true, "Война и мир", 1225, "Лев Толстой"),
        Newspaper(55678, true, "Новости мира", 150),
        Disk(98765, true, "Интерстеллар", "CD")
    )

    mainLoop@ while (true) {
        println("""
            Главное меню:
            1. Показать книги
            2. Показать газеты
            3. Показать диски
            4. Выход (закрыть программу)
        """.trimIndent())
        print("Введите ваш выбор: ")
        when (scanner.nextLine().trim()) {
            "1" -> {
                val books = libraryItems.filter { it is Book }
                if (books.isEmpty()) {
                    println("Книг не найдено.")
                    continue@mainLoop
                }
                processSubtype(books, scanner, "Книга")
            }
            "2" -> {
                val newspapers = libraryItems.filter { it is Newspaper }
                if (newspapers.isEmpty()) {
                    println("Газет не найдено.")
                    continue@mainLoop
                }
                processSubtype(newspapers, scanner, "Газета")
            }
            "3" -> {
                val disks = libraryItems.filter { it is Disk }
                if (disks.isEmpty()) {
                    println("Дисков не найдено.")
                    continue@mainLoop
                }
                processSubtype(disks, scanner, "Диск")
            }
            "4" -> break@mainLoop
            else -> println("Неверный выбор, повторите ввод.")
        }
    }
    println("Программа завершена.")
}

fun processSubtype(items: List<LibraryItem>, scanner: Scanner, itemType: String) {
    subtypeLoop@ while (true) {
        println("$itemType список:")
        for ((index, item) in items.withIndex()) {
            println("${index + 1}. ${item.getShortInfo()}")
        }
        println("0. Вернуться в главное меню")
        print("Выберите объект: ")
        val choiceStr = scanner.nextLine().trim()
        if (choiceStr == "0") break@subtypeLoop
        val choice = choiceStr.toIntOrNull()
        if (choice == null || choice !in 1..items.size) {
            println("Неверный выбор, повторите ввод.")
            continue@subtypeLoop
        }
        val selectedItem = items[choice - 1]
        objectActionLoop@ while (true) {
            println("""
                Выбранный объект: ${selectedItem.getShortInfo()}
                1. Взять домой
                2. Читать в читальном зале
                3. Показать подробную информацию
                4. Вернуть
                0. Вернуться к выбору объекта
            """.trimIndent())
            print("Ваш выбор: ")
            when (scanner.nextLine().trim()) {
                "1" -> {
                    if (selectedItem !is Book && selectedItem !is Disk) {
                        println("Невозможно взять домой данный тип объекта.")
                    } else if (!selectedItem.available) {
                        println("Объект уже занят.")
                    } else {
                        selectedItem.available = false
                        println("$itemType ${selectedItem.id} взяли домой")
                    }
                }
                "2" -> {
                    if (selectedItem !is Book && selectedItem !is Newspaper) {
                        println("Невозможно читать в читальном зале данный тип объекта.")
                    } else if (!selectedItem.available) {
                        println("Объект уже занят.")
                    } else {
                        selectedItem.available = false
                        println("$itemType ${selectedItem.id} взяли в читальный зал")
                    }
                }
                "3" -> {
                    println(selectedItem.getDetailedInfo())
                }
                "4" -> {
                    if (selectedItem.available) {
                        println("Невозможно вернуть, объект уже доступен.")
                    } else {
                        selectedItem.available = true
                        println("$itemType ${selectedItem.id} возвращен")
                    }
                }
                "0" -> break@objectActionLoop
                else -> println("Неверный выбор, повторите ввод.")
            }
        }
    }
}