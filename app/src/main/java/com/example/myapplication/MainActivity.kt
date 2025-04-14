package com.example.myapplication

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ListFragment.OnItemSelectedListener {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val libraryViewModel: LibraryViewModel by lazy {
        ViewModelProvider(this)[LibraryViewModel::class.java]
    }
    private var isTwoPaneMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        isTwoPaneMode = binding.detailContainer != null

        if (supportFragmentManager.findFragmentById(R.id.listContainer) !is ListFragment) {
            supportFragmentManager.commit {
                replace(R.id.listContainer, ListFragment())
            }
        }

        if (isTwoPaneMode) {
            val fragment = if (libraryViewModel.selectedItem != null) {
                DetailFragment.newInstance(
                    editable = false,
                    itemType = getItemType(libraryViewModel.selectedItem!!),
                    item = libraryViewModel.selectedItem
                )
            } else {
                EmptyDetailFragment()
            }
            supportFragmentManager.commit {
                replace(R.id.detailContainer, fragment)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isTwoPaneMode) {
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    } else {
                        finish()
                    }
                } else {
                    val detailFragment = supportFragmentManager.findFragmentById(R.id.detailContainer)
                    if (detailFragment !is EmptyDetailFragment) {
                        libraryViewModel.selectedItem = null
                        supportFragmentManager.commit {
                            replace(R.id.detailContainer, EmptyDetailFragment())
                        }
                    } else {
                        finish()
                    }
                }
            }
        })
    }

    override fun onItemSelected(item: LibraryItem) {
        libraryViewModel.selectedItem = item
        val detailFragment = DetailFragment.newInstance(
            editable = false,
            itemType = getItemType(item),
            item = item
        )
        if (isTwoPaneMode) {
            supportFragmentManager.commit {
                replace(R.id.detailContainer, detailFragment)
            }
        } else {
            supportFragmentManager.commit {
                replace(R.id.listContainer, detailFragment)
                addToBackStack(null)
            }
        }
    }

    override fun onAddItemClicked() {
        val options = arrayOf("Книга", "Газета", "Диск")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                val itemType = when (which) {
                    0 -> DetailFragment.TYPE_BOOK
                    1 -> DetailFragment.TYPE_NEWSPAPER
                    2 -> DetailFragment.TYPE_DISK
                    else -> ""
                }
                val fragment = DetailFragment.newInstance(editable = true, itemType = itemType, item = null)
                if (isTwoPaneMode) {
                    supportFragmentManager.commit {
                        replace(R.id.detailContainer, fragment)
                    }
                } else {
                    supportFragmentManager.commit {
                        replace(R.id.listContainer, fragment)
                        addToBackStack(null)
                    }
                }
            }
            .show()
    }

    private fun getItemType(item: LibraryItem): String = when (item) {
        is Book -> DetailFragment.TYPE_BOOK
        is Newspaper -> DetailFragment.TYPE_NEWSPAPER
        is Disk -> DetailFragment.TYPE_DISK
        else -> ""
    }
}