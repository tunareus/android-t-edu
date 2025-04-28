package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.Book
import com.example.myapplication.Disk
import com.example.myapplication.LibraryItem
import com.example.myapplication.Month
import com.example.myapplication.Newspaper
import com.example.myapplication.data.local.dao.LibraryItemDao
import com.example.myapplication.data.local.mapper.toEntity
import com.example.myapplication.data.local.model.LibraryItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private fun initializeLibraryStub(): List<LibraryItem> {
    return mutableListOf(
        Book(0, false, "Гордость и предубеждение", 432, "Джейн Остин"),
        Book(0, true, "1984", 328, "Джордж Оруэлл"), // Дубликат для теста
        Book(0, true, "Великий Гэтсби", 180, "Фрэнсис Скотт Фицджеральд"),
        Book(0, false, "Моби Дик", 635, "Герман Мелвилл"),
        Book(0, true, "Процесс", 250, "Франц Кафка"),
        Book(0, true, "Сто лет одиночества", 417, "Габриэль Гарсиа Маркес"),
        Book(0, false, "Над пропастью во ржи", 214, "Джером Сэлинджер"),
        Book(0, true, "Франкенштейн", 280, "Мэри Шелли"),
        Book(0, true, "Дракула", 418, "Брэм Стокер"),
        Book(0, false, "Джейн Эйр", 500, "Шарлотта Бронте"),
        Book(0, true, "О дивный новый мир", 311, "Олдос Хаксли"),
        Book(0, true, "Собачье сердце", 128, "Михаил Булгаков"),
        Book(0, false, "Идиот", 640, "Фёдор Достоевский"),
        Book(0, true, "Отцы и дети", 300, "Иван Тургенев"),
        Book(0, true, "Мёртвые души", 480, "Николай Гоголь"),
        Book(0, false, "Герой нашего времени", 208, "Михаил Лермонтов"),
        Book(0, true, "Евгений Онегин", 320, "Александр Пушкин"),
        Book(0, true, "Три товарища", 480, "Эрих Мария Ремарк"),
        Book(0, false, "Цветы для Элджернона", 311, "Дэниел Киз"),
        Book(0, true, "Вино из одуванчиков", 384, "Рэй Брэдбери"),
        Book(0, true, "Алиса в Стране чудес", 200, "Льюис Кэрролл"),
        Book(0, false, "Хоббит, или Туда и обратно", 310, "Дж. Р. Р. Толкин"),
        Book(0, true, "Автостопом по галактике", 215, "Дуглас Адамс"),
        Book(0, true, "Дюна", 688, "Фрэнк Герберт"),
        Book(0, false, "Оно", 1138, "Стивен Кинг"),
        Book(0, true, "Сияние", 447, "Стивен Кинг"),
        Book(0, true, "Марсианин", 369, "Энди Вейер"),
        Book(0, false, "Игра Эндера", 324, "Орсон Скотт Кард"),
        Book(0, true, "Задача трёх тел", 400, "Лю Цысинь"),
        Book(0, true, "Гиперион", 482, "Дэн Симмонс"),
        Book(0, false, "Контакт", 432, "Карл Саган"),
        Book(0, true, "Мечтают ли андроиды об электроовцах?", 210, "Филип К. Дик"),
        Book(0, true, "Пикник на обочине", 224, "Аркадий и Борис Стругацкие"),
        Book(0, false, "Трудно быть богом", 256, "Аркадий и Борис Стругацкие"),
        Book(0, true, "Солярис", 204, "Станислав Лем"),
        Newspaper(0, true, "The New York Times", 35120, Month.OCTOBER),
        Newspaper(0, false, "The Guardian", 28541, Month.NOVEMBER),
        Newspaper(0, true, "Le Monde", 31005, Month.DECEMBER),
        Newspaper(0, true, "Коммерсантъ", 15874, Month.MARCH),
        Newspaper(0, false, "Ведомости", 12345, Month.AUGUST),
        Newspaper(0, true, "Российская газета", 21098, Month.SEPTEMBER),
        Newspaper(0, true, "Financial Times", 45123, Month.APRIL),
        Newspaper(0, false, "The Wall Street Journal", 50101, Month.JUNE),
        Newspaper(0, true, "Die Zeit", 29876, Month.JULY),
        Newspaper(0, true, "Новая газета", 9541, Month.MAY),
        Newspaper(0, true, "За рулём", 18765, Month.JANUARY),
        Newspaper(0, false, "Вокруг света", 6543, Month.MARCH),
        Newspaper(0, true, "Популярная механика", 8888, Month.NOVEMBER),
        Newspaper(0, true, "Men's Health", 5432, Month.JUNE),
        Newspaper(0, false, "Vogue", 7654, Month.SEPTEMBER),
        Newspaper(0, true, "Cosmopolitan", 8765, Month.APRIL),
        Newspaper(0, true, "National Geographic", 10001, Month.DECEMBER),
        Newspaper(0, false, "Городские Вести", 3210, Month.AUGUST),
        Newspaper(0, true, "Северный Рабочий", 4321, Month.MAY),
        Newspaper(0, true, "Вечерний Город", 5678, Month.JULY),
        Newspaper(0, false, "ТехноМир", 2468, Month.FEBRUARY),
        Newspaper(0, true, "Финансовый Курьер", 1357, Month.NOVEMBER),
        Newspaper(0, true, "Садовод", 9753, Month.APRIL),
        Newspaper(0, false, "Рыбалка и Охота", 8642, Month.OCTOBER),
        Newspaper(0, true, "Мир Фантастики", 1111, Month.JANUARY),
        Newspaper(0, true, "Игромания", 2222, Month.SEPTEMBER),
        Newspaper(0, false, "Юный техник", 10500, Month.JUNE),
        Newspaper(0, true, "Мурзилка", 15000, Month.MARCH),
        Disk(0, true, "Крёстный отец", "DVD"),
        Disk(0, false, "Побег из Шоушенка", "Blu-ray"),
        Disk(0, true, "Криминальное чтиво", "DVD"),
        Disk(0, true, "Форрест Гамп", "VHS"),
        Disk(0, false, "Бойцовский клуб", "Blu-ray"),
        Disk(0, true, "Начало", "DVD"),
        Disk(0, true, "Тёмный рыцарь", "Blu-ray"),
        Disk(0, false, "Список Шиндлера", "DVD"),
        Disk(0, true, "Молчание ягнят", "CD"),
        Disk(0, true, "Гладиатор", "DVD"),
        Disk(0, false, "Зелёная миля", "Blu-ray"),
        Disk(0, true, "Леон", "DVD"),
        Disk(0, true, "Король Лев (1994)", "VHS"),
        Disk(0, false, "История игрушек", "Blu-ray"),
        Disk(0, true, "Унесённые призраками", "DVD"),
        Disk(0, true, "Назад в будущее", "DVD"),
        Disk(0, false, "Терминатор 2: Судный день", "Blu-ray"),
        Disk(0, true, "Чужой", "VHS"),
        Disk(0, true, "Бегущий по лезвию (Final Cut)", "Blu-ray"),
        Disk(0, false, "The Beatles - Abbey Road", "CD"),
        Disk(0, true, "Pink Floyd - The Dark Side of the Moon", "CD"),
        Disk(0, false, "Queen - A Night at the Opera", "CD"),
        Disk(0, true, "Nirvana - Nevermind", "CD"),
        Disk(0, true, "Michael Jackson - Thriller", "CD"),
        Disk(0, false, "Моцарт - Реквием", "CD"),
        Disk(0, true, "Чайковский - Щелкунчик", "CD"),
        Disk(0, true, "Windows 11 Pro", "DVD"),
        Disk(0, false, "Microsoft Office 2021", "CD"),
        Disk(0, true, "Adobe Photoshop CC", "DVD"),
        Disk(0, true, "The Witcher 3: Wild Hunt", "Blu-ray"),
        Disk(0, false, "Red Dead Redemption 2", "Blu-ray"),
        Disk(0, true, "Grand Theft Auto V", "DVD"),
        Disk(0, true, "Half-Life: Alyx", "HoloDisk"),
        Disk(0, false, "Коллекция документальных фильмов BBC Earth", "Blu-ray"),
        Disk(0, true, "Курс лекций по квантовой физике", "CD")
    )
}

@Database(entities = [LibraryItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun libraryItemDao(): LibraryItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "library_database"
                )
                    .addCallback(LibraryDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LibraryDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.libraryItemDao())
                }
            }
        }

        suspend fun populateDatabase(libraryItemDao: LibraryItemDao) {
            libraryItemDao.deleteAll()
            val initialItems = initializeLibraryStub().map { it.toEntity(id = 0) }
            libraryItemDao.insertAll(initialItems)
        }
    }
}