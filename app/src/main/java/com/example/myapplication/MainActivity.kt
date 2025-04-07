package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
                result.data?.getSerializableExtra(DetailActivity.EXTRA_ITEM)?.let { serializable ->
                    val newItem = serializable as? LibraryItem
                    newItem?.let {
                        viewModel.libraryItems.add(it)
                        viewModel.libraryItems.sortBy { item -> item.id }
                        val updatedList = viewModel.libraryItems.toList()
                        val newIndex = updatedList.indexOf(it)
                        adapter.submitList(updatedList) {
                            binding.recyclerView.smoothScrollToPosition(newIndex)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        adapter.submitList(viewModel.libraryItems.toList())
        adapter.itemClickListener = { item ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_ITEM, item as java.io.Serializable)
                when (item) {
                    is Book -> putExtra(DetailActivity.EXTRA_ITEM_TYPE, DetailActivity.TYPE_BOOK)
                    is Disk -> putExtra(DetailActivity.EXTRA_ITEM_TYPE, DetailActivity.TYPE_DISK)
                    is Newspaper -> putExtra(DetailActivity.EXTRA_ITEM_TYPE, DetailActivity.TYPE_NEWSPAPER)
                }
                putExtra(DetailActivity.EXTRA_EDITABLE, false)
            }
            detailActivityLauncher.launch(intent)
        }
        binding.addButton.setOnClickListener {
            val types = arrayOf("Книга", "Диск", "Газета")
            AlertDialog.Builder(this)
                .setTitle("Выберите тип элемента")
                .setItems(types) { _, which ->
                    val selectedType = when (which) {
                        0 -> DetailActivity.TYPE_BOOK
                        1 -> DetailActivity.TYPE_DISK
                        2 -> DetailActivity.TYPE_NEWSPAPER
                        else -> DetailActivity.TYPE_BOOK
                    }
                    val intent = Intent(this, DetailActivity::class.java).apply {
                        putExtra(DetailActivity.EXTRA_EDITABLE, true)
                        putExtra(DetailActivity.EXTRA_ITEM_TYPE, selectedType)
                    }
                    detailActivityLauncher.launch(intent)
                }
                .show()
        }
        val swipeCallback = SwipeToDeleteCallback { position ->
            removeItem(position)
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun removeItem(position: Int) {
        if (position in viewModel.libraryItems.indices) {
            val removedItem = viewModel.libraryItems.removeAt(position)
            UniqueIdGenerator.releaseId(removedItem.id)
            adapter.submitList(viewModel.libraryItems.toList())
        }
    }
}