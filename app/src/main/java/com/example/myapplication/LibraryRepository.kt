package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class RepositoryLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class LibraryRepository {

    private val mutex = Mutex()
    private var internalLibraryItems: MutableList<LibraryItem> = mutableListOf()
    private var loadCounter = 0

    init {
        UniqueIdGenerator.reset()
        internalLibraryItems = initializeLibrary()
    }

    private fun shouldSimulateError(): Boolean {
        loadCounter++
        return loadCounter % 5 == 0
    }

    suspend fun getItems(): List<LibraryItem> = withContext(Dispatchers.IO) {
        val loadingDelay = Random.nextLong(100, 2001)
        delay(loadingDelay)

        if (shouldSimulateError()) {
            throw RepositoryLoadException("Ошибка симуляции: Не удалось загрузить данные из репозитория.")
        }

        val items = mutex.withLock {
            internalLibraryItems.toList()
        }

        delay(1000)

        return@withContext items
    }

    suspend fun addItem(item: LibraryItem) = withContext(Dispatchers.IO) {
        delay(Random.nextLong(50, 501))
        mutex.withLock {
            internalLibraryItems.add(item)
            internalLibraryItems.sortBy { it.id }
        }
    }

    suspend fun removeItem(item: LibraryItem) = withContext(Dispatchers.IO) {
        delay(Random.nextLong(50, 501))
        mutex.withLock {
            internalLibraryItems.remove(item)
            UniqueIdGenerator.releaseId(item.id)
        }
    }

    private fun initializeLibrary(): MutableList<LibraryItem> {
        return mutableListOf(
            Book(UniqueIdGenerator.getUniqueId(), true, "Маугли", 202, "Джозеф Киплинг"),
            Newspaper(UniqueIdGenerator.getUniqueId(), false, "Сельская жизнь", 794, Month.MARCH),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Дэдпул и Росомаха", "DVD"),
            Book(UniqueIdGenerator.getUniqueId(), true, "Война и мир", 1225, "Лев Толстой"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Новости мира", 150, Month.JULY),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Интерстеллар", "CD"),
            Book(UniqueIdGenerator.getUniqueId(), false, "1984", 328, "Джордж Оруэлл"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Спорт сегодня", 45, Month.APRIL),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Звездные войны", "Blu-ray"),
            Book(UniqueIdGenerator.getUniqueId(), false, "Убить пересмешника", 281, "Харпер Ли"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Экономика и жизнь", 120, Month.FEBRUARY),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Титаник", "DVD"),
            Book(UniqueIdGenerator.getUniqueId(), false, "Гарри Поттер и философский камень", 223, "Джоан Роулинг"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Наука и жизнь", 300, Month.JANUARY),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Властелин колец", "Blu-ray"),
            Book(UniqueIdGenerator.getUniqueId(), true, "Мастер и Маргарита", 406, "Михаил Булгаков"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Культура и искусство", 200, Month.MAY),
            Disk(UniqueIdGenerator.getUniqueId(), false, "Аватар", "DVD"),
            Book(UniqueIdGenerator.getUniqueId(), true, "Преступление и наказание", 430, "Федор Достоевский"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Технологии будущего", 75, Month.AUGUST),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Матрица", "Blu-ray"),
            Book(UniqueIdGenerator.getUniqueId(), false, "451 градус по Фаренгейту", 256, "Рэй Брэдбери"),
            Newspaper(UniqueIdGenerator.getUniqueId(), true, "Политика и общество", 90, Month.NOVEMBER),
            Disk(UniqueIdGenerator.getUniqueId(), true, "Начало", "DVD")
        )
    }
}