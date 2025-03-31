package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val libraryItems = mutableListOf<LibraryItem>()
    private val adapter = LibraryItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        libraryItems.addAll(initializeLibrary())
        adapter.submitList(libraryItems.toList())

        val swipeCallback = SwipeToDeleteCallback { position ->
            removeItem(position)
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initializeLibrary(): List<LibraryItem> {
        return listOf(
            Book(90743, true, "Маугли", 202, "Джозеф Киплинг"),
            Newspaper(17245, true, "Сельская жизнь", 794, Month.MARCH),
            Disk(33456, true, "Дэдпул и Росомаха", "DVD"),
            Book(11223, true, "Война и мир", 1225, "Лев Толстой"),
            Newspaper(55678, true, "Новости мира", 150, Month.JULY),
            Disk(98765, true, "Интерстеллар", "CD"),
            Book(12345, true, "1984", 328, "Джордж Оруэлл"),
            Newspaper(67890, true, "Технологии будущего", 42, Month.APRIL),
            Disk(54321, true, "Звёздные войны", "Blu-ray"),
            Book(11111, true, "Убить пересмешника", 281, "Харпер Ли"),
            Newspaper(22222, true, "Спорт сегодня", 99, Month.MAY),
            Disk(33333, true, "Властелин колец", "4K UHD"),
            Book(44444, true, "Гарри Поттер и философский камень", 223, "Джоан Роулинг"),
            Newspaper(55555, true, "Экономика и финансы", 12, Month.JUNE),
            Disk(66666, true, "Титаник", "DVD"),
            Book(77777, true, "Мастер и Маргарита", 384, "Михаил Булгаков"),
            Newspaper(88888, true, "Культура и искусство", 25, Month.SEPTEMBER),
            Disk(99999, true, "Матрица", "Blu-ray"),
            Book(10101, true, "Преступление и наказание", 430, "Фёдор Достоевский")
        )
    }

    private fun removeItem(position: Int) {
        if (position in libraryItems.indices) {
            val removedItem = libraryItems[position]
            libraryItems.removeAt(position)
            adapter.submitList(libraryItems.toList())

            Snackbar.make(binding.root, "Элемент c id ${removedItem.id} удалён", Snackbar.LENGTH_LONG)
                .setAction("Отменить") {
                    libraryItems.add(position, removedItem)
                    adapter.submitList(libraryItems.toList())
                }
                .show()
        }
    }
}