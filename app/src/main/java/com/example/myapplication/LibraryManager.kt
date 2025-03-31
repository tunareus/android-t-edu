package com.example.myapplication

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
        Book(UniqueIdGenerator.getUniqueId(), true, "Маугли", 202, "Джозеф Киплинг"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Сельская жизнь", 794, Month.MARCH),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Дэдпул и Росомаха", "DVD"),
        Book(UniqueIdGenerator.getUniqueId(), true, "Война и мир", 1225, "Лев Толстой"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Новости мира", 150, Month.JULY),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Интерстеллар", "CD"),
        Book(UniqueIdGenerator.getUniqueId(), true, "1984", 328, "Джордж Оруэлл"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Спорт сегодня", 45, Month.APRIL),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Звездные войны", "Blu-ray"),
        Book(UniqueIdGenerator.getUniqueId(), true, "Убить пересмешника", 281, "Харпер Ли"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Экономика и жизнь", 120, Month.FEBRUARY),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Титаник", "DVD"),
        Book(UniqueIdGenerator.getUniqueId(), true, "Гарри Поттер и философский камень", 223, "Джоан Роулинг"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Наука и жизнь", 300, Month.JANUARY),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Властелин колец", "Blu-ray"),
        Book(UniqueIdGenerator.getUniqueId(), true, "Мастер и Маргарита", 406, "Михаил Булгаков"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Культура и искусство", 200, Month.MAY),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Аватар", "DVD"),
        Book(UniqueIdGenerator.getUniqueId(), true, "Преступление и наказание", 430, "Федор Достоевский"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Технологии будущего", 75, Month.AUGUST),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Матрица", "Blu-ray"),
        Book(UniqueIdGenerator.getUniqueId(), true, "451 градус по Фаренгейту", 256, "Рэй Брэдбери"),
        Newspaper(UniqueIdGenerator.getUniqueId(), true, "Политика и общество", 90, Month.NOVEMBER),
        Disk(UniqueIdGenerator.getUniqueId(), true, "Начало", "DVD")
    )
}

fun runLibrarySystem(scanner: Scanner, libraryItems: MutableList<LibraryItem>) {
    val manager = Manager()
    val digitizationCabinet: DigitizationCabinet<Digitizable> = DigitizationCabinet()

    mainLoop@ while (true) {
        displayMainMenu()
        print("Введите ваш выбор: ")

        when (scanner.nextLine().trim()) {
            ACTION_BOOKS -> handleBookSection(libraryItems, scanner)
            ACTION_NEWSPAPERS -> handleNewspaperSection(libraryItems, scanner)
            ACTION_DISKS -> handleDiskSection(libraryItems, scanner)
            ACTION_MANAGER -> handleManagerSection(scanner, manager, libraryItems)
            ACTION_DIGITIZE -> handleDigitizationSection(scanner, digitizationCabinet, libraryItems)
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
        $ACTION_MANAGER. Менеджер по закупкам
        $ACTION_DIGITIZE. Кабинет оцифровки
        $ACTION_EXIT. Выход (закрыть программу)
    """.trimIndent())
}

fun handleBookSection(libraryItems: List<LibraryItem>, scanner: Scanner) {
    val books = libraryItems.filterByType<Book>()
    if (books.isEmpty()) {
        println("Книг не найдено.")
        return
    }
    processSubtype(books, scanner, "Книга")
}

fun handleNewspaperSection(libraryItems: List<LibraryItem>, scanner: Scanner) {
    val newspapers = libraryItems.filterByType<Newspaper>()
    if (newspapers.isEmpty()) {
        println("Газет не найдено.")
        return
    }
    processSubtype(newspapers, scanner, "Газета")
}

fun handleDiskSection(libraryItems: List<LibraryItem>, scanner: Scanner) {
    val disks = libraryItems.filterByType<Disk>()
    if (disks.isEmpty()) {
        println("Дисков не найдено.")
        return
    }
    processSubtype(disks, scanner, "Диск")
}

fun handleManagerSection(scanner: Scanner, manager: Manager, items: MutableList<LibraryItem>) {
    println("""
        Менеджер по закупкам:
        $ACTION_BUY_BOOK. Купить книгу
        $ACTION_BUY_NEWSPAPER. Купить газету
        $ACTION_BUY_DISK. Купить диск
        $ACTION_GO_BACK. Вернуться в главное меню
    """.trimIndent())

    print("Введите ваш выбор: ")

    when (scanner.nextLine().trim()) {
        ACTION_BUY_BOOK -> {
            val bookShop = BookShop()
            val book = manager.buy(bookShop)
            items.add(book)
            println("Книга '${book.name}' с ID ${book.id} успешно куплена и добавлена в библиотеку")
        }
        ACTION_BUY_NEWSPAPER -> {
            val newspaperKiosk = NewspaperKiosk()
            val newspaper = manager.buy(newspaperKiosk)
            items.add(newspaper)
            println("Газета '${newspaper.name}' с ID ${newspaper.id} успешно куплена и добавлена в библиотеку")
        }
        ACTION_BUY_DISK -> {
            val diskShop = DiskShop()
            val disk = manager.buy(diskShop)
            items.add(disk)
            println("Диск '${disk.name}' с ID ${disk.id} успешно куплен и добавлен в библиотеку")
        }
        ACTION_GO_BACK -> return
        else -> println("Неверный выбор, повторите ввод.")
    }
}

fun handleDigitizationSection(scanner: Scanner, cabinet: DigitizationCabinet<Digitizable>, items: MutableList<LibraryItem>) {
    println("""
        Кабинет оцифровки:
        $ACTION_DIGITIZE_BOOK. Оцифровать книгу
        $ACTION_DIGITIZE_NEWSPAPER. Оцифровать газету
        $ACTION_GO_BACK. Вернуться в главное меню
    """.trimIndent())

    print("Введите ваш выбор: ")

    when (scanner.nextLine().trim()) {
        ACTION_DIGITIZE_BOOK -> {
            val books = items.filterByType<Book>()
            if (books.isEmpty()) {
                println("Книг не найдено.")
                return
            }

            println("Выберите книгу для оцифровки:")
            books.forEachIndexed { index, book ->
                println("${index + 1}. ${book.getShortInfo()}")
            }

            print("Введите номер: ")
            val choice = scanner.nextLine().toIntOrNull()

            if (choice == null || choice !in 1..books.size) {
                println("Неверный выбор.")
                return
            }

            val selectedBook = books[choice - 1]
            val disk = cabinet.digitize(selectedBook)
            items.add(disk)
            println("Книга '${selectedBook.name}' успешно оцифрована. Создан диск '${disk.name}' с ID ${disk.id}")
        }
        ACTION_DIGITIZE_NEWSPAPER -> {
            val newspapers = items.filterByType<Newspaper>()
            if (newspapers.isEmpty()) {
                println("Газет не найдено.")
                return
            }

            println("Выберите газету для оцифровки:")
            newspapers.forEachIndexed { index, newspaper ->
                println("${index + 1}. ${newspaper.getShortInfo()}")
            }

            print("Введите номер: ")
            val choice = scanner.nextLine().toIntOrNull()

            if (choice == null || choice !in 1..newspapers.size) {
                println("Неверный выбор.")
                return
            }

            val selectedNewspaper = newspapers[choice - 1]
            val disk = cabinet.digitize(selectedNewspaper)
            items.add(disk)
            println("Газета '${selectedNewspaper.name}' успешно оцифрована. Создан диск '${disk.name}' с ID ${disk.id}")
        }
        ACTION_GO_BACK -> return
        else -> println("Неверный выбор, повторите ввод.")
    }
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