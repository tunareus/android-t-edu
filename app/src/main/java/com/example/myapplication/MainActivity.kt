package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = LibraryItemAdapter()
    private val viewModel: LibraryViewModel by viewModels()

    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val newItem = result.data?.getParcelableExtraCompat<LibraryItem>(DetailActivity.EXTRA_ITEM)
                newItem?.let {
                    viewModel.onItemCreated(it)
                    binding.recyclerView.post {
                        val pos =
                            viewModel.libraryItems.value?.indexOfFirst { item -> item.id == it.id }
                                ?: -1
                        if (pos >= 0) {
                            binding.recyclerView.scrollToPosition(pos)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.libraryItems.observe(this) { list ->
            adapter.submitList(list.toList())
        }

        adapter.itemClickListener = { item ->
            val itemType = when (item) {
                is Book -> DetailActivity.TYPE_BOOK
                is Disk -> DetailActivity.TYPE_DISK
                is Newspaper -> DetailActivity.TYPE_NEWSPAPER
                else -> DetailActivity.TYPE_BOOK
            }
            val intent = DetailActivity.newIntent(this, false, itemType, item)
            detailActivityLauncher.launch(intent)
        }

        binding.addButton.setOnClickListener {
            val types = arrayOf("Книга", "Диск", "Газета")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите тип элемента")
                .setItems(types) { _, which ->
                    val selectedType = when (which) {
                        0 -> DetailActivity.TYPE_BOOK
                        1 -> DetailActivity.TYPE_DISK
                        2 -> DetailActivity.TYPE_NEWSPAPER
                        else -> DetailActivity.TYPE_BOOK
                    }
                    val intent = DetailActivity.newIntent(this, true, selectedType)
                    detailActivityLauncher.launch(intent)
                }
                .show()
        }

        val swipeCallback = SwipeToDeleteCallback { position ->
            viewModel.removeItem(position)
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }
}