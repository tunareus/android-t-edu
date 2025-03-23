package com.example.dz_oop

import java.util.Scanner
import java.io.PrintStream

fun main() {
    System.setOut(PrintStream(System.out, true, "UTF-8"))
    val scanner = Scanner(System.`in`, "UTF-8")
    val libraryItems = initializeLibrary()
    runLibrarySystem(scanner, libraryItems)
}

fun initializeLibrary(): MutableList<LibraryItem> {
    return mutableListOf(
        Book(90743, true, "Маугли", 202, "Джозеф Киплинг"),
        Newspaper(17245, true, "Сельская жизнь", 794),
        Disk(33456, true, "Дэдпул и Росомаха", "DVD"),
        Book(11223, true, "Война и мир", 1225, "Лев Толстой"),
        Newspaper(55678, true, "Новости мира", 150),
        Disk(98765, true, "Интерстеллар", "CD")
    )
}

fun runLibrarySystem(scanner: Scanner, libraryItems: List<LibraryItem>) {
    mainLoop@ while (true) {
        displayMainMenu()
        print("Введите ваш выбор: ")

        when (scanner.nextLine().trim()) {
            ACTION_BOOKS -> handleBookSection(libraryItems, scanner)
            ACTION_NEWSPAPERS -> handleNewspaperSection(libraryItems, scanner)
            ACTION_DISKS -> handleDiskSection(libraryItems, scanner)
            ACTION_EXIT -> break@mainLoop
            else -> println("Неверный выбор, повторите ввод.")
        }
    }
    println("Программа завершена.")
}

fun displayMainMenu() {
    println("""
        Главное меню:
        $ACTION_BOOKS. Показать книги
        $ACTION_NEWSPAPERS. Показать газеты
        $ACTION_DISKS. Показать диски
        $ACTION_EXIT. Выход (закрыть программу)
    """.trimIndent())
}

fun handleBookSection(libraryItems: List<LibraryItem>, scanner: Scanner) {
    val books = libraryItems.filterIsInstance<Book>()
    if (books.isEmpty()) {
        println("Книг не найдено.")
        return
    }
    processSubtype(books, scanner, "Книга")
}

fun handleNewspaperSection(libraryItems: List<LibraryItem>, scanner: Scanner) {
    val newspapers = libraryItems.filterIsInstance<Newspaper>()
    if (newspapers.isEmpty()) {
        println("Газет не найдено.")
        return
    }
    processSubtype(newspapers, scanner, "Газета")
}

fun handleDiskSection(libraryItems: List<LibraryItem>, scanner: Scanner) {
    val disks = libraryItems.filterIsInstance<Disk>()
    if (disks.isEmpty()) {
        println("Дисков не найдено.")
        return
    }
    processSubtype(disks, scanner, "Диск")
}

fun processSubtype(items: List<LibraryItem>, scanner: Scanner, itemType: String) {
    subtypeLoop@ while (true) {
        displayItemsList(items, itemType)
        print("Выберите объект: ")
        val choiceStr = scanner.nextLine().trim()

        if (choiceStr == ACTION_GO_BACK) break@subtypeLoop

        val choice = choiceStr.toIntOrNull()
        if (choice == null || choice !in 1..items.size) {
            println("Неверный выбор, повторите ввод.")
            continue@subtypeLoop
        }

        processItemActions(items[choice - 1], scanner, itemType)
    }
}

fun displayItemsList(items: List<LibraryItem>, itemType: String) {
    println("$itemType список:")
    items.forEachIndexed { index, item ->
        println("${index + 1}. ${item.getShortInfo()}")
    }
    println("$ACTION_GO_BACK. Вернуться в главное меню")
}

fun processItemActions(selectedItem: LibraryItem, scanner: Scanner, itemType: String) {
    objectActionLoop@ while (true) {
        displayItemMenu(selectedItem)
        print("Ваш выбор: ")

        when (scanner.nextLine().trim()) {
            ACTION_TAKE_HOME -> handleTakeHome(selectedItem, itemType)
            ACTION_READ_IN_LIBRARY -> handleReadInLibrary(selectedItem, itemType)
            ACTION_SHOW_DETAILS -> println(selectedItem.getDetailedInfo())
            ACTION_RETURN -> handleReturn(selectedItem, itemType)
            ACTION_GO_BACK -> break@objectActionLoop
            else -> println("Неверный выбор, повторите ввод.")
        }
    }
}

fun displayItemMenu(item: LibraryItem) {
    println("""
        Выбранный объект: ${item.getShortInfo()}
        $ACTION_TAKE_HOME. Взять домой
        $ACTION_READ_IN_LIBRARY. Читать в читальном зале
        $ACTION_SHOW_DETAILS. Показать подробную информацию
        $ACTION_RETURN. Вернуть
        $ACTION_GO_BACK. Вернуться к выбору объекта
    """.trimIndent())
}

fun handleTakeHome(item: LibraryItem, itemType: String) {
    if (item !is HomeLendable) {
        println("Невозможно взять домой данный тип объекта.")
    } else if (!item.available) {
        println("Объект уже занят.")
    } else {
        item.takeHomeAction()
        println("$itemType ${item.id} взяли домой")
    }
}

fun handleReadInLibrary(item: LibraryItem, itemType: String) {
    if (item !is InLibraryUse) {
        println("Невозможно читать в читальном зале данный тип объекта.")
    } else if (!item.available) {
        println("Объект уже занят.")
    } else {
        item.readInLibraryAction()
        println("$itemType ${item.id} взяли в читальный зал")
    }
}

fun handleReturn(item: LibraryItem, itemType: String) {
    if (item.available) {
        println("Невозможно вернуть, объект уже доступен.")
    } else {
        item.available = true
        println("$itemType ${item.id} возвращен")
    }
}