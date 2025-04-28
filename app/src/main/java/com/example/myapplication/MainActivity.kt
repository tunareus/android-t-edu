package com.example.myapplication

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class MainActivity : AppCompatActivity(), ListFragment.OnItemSelectedListener {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val libraryViewModel: LibraryViewModel by lazy {
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val settingsRepository = SettingsRepository(applicationContext)
        val repository = LibraryRepository(database.libraryItemDao())
        val factory = LibraryViewModel.LibraryViewModelFactory(repository, settingsRepository)
        ViewModelProvider(this, factory)[LibraryViewModel::class.java]
    }

    private var isTwoPaneMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        isTwoPaneMode = binding.detailContainer != null

        if (savedInstanceState != null) {
            binding.root.post {
                restoreStateAfterRotation()
            }
        }

        setupViewModelObservers()
        setupInitialUi(savedInstanceState)
        setupOnBackPressed()
    }

    private fun restoreStateAfterRotation() {
        if (isTwoPaneMode) {
            if (libraryViewModel.isAddingItem.value) {
                libraryViewModel.addItemType.value?.let { replaceDetailWithAddFragment(it) }
            } else libraryViewModel.selectedItem.value?.let {
                replaceDetailFragment(it)
            } ?: replaceWithEmptyFragment()
        } else {
            val navController = try { findNavController(R.id.nav_host_fragment) } catch (e: Exception) { null }
            if (navController == null) return

            if (libraryViewModel.isAddingItem.value) {
                libraryViewModel.addItemType.value?.let { type ->
                    if (navController.currentDestination?.id != R.id.detailFragment) {
                        navigateToDetailFragment(editable = true, itemType = type, item = null)
                    }
                }
            } else if (libraryViewModel.selectedItem.value != null) {
                libraryViewModel.selectedItem.value?.let { item ->
                    if (navController.currentDestination?.id != R.id.detailFragment) {
                        navigateToDetailFragment(editable = false, item = item)
                    }
                }
            } else {
                if (navController.currentDestination?.id != R.id.listFragment) {
                    navController.popBackStack(R.id.listFragment, false)
                }
            }
        }
    }

    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            libraryViewModel.selectedItem.collectLatest { item ->
                if (isTwoPaneMode && !libraryViewModel.isAddingItem.value) {
                    if (item != null) {
                        replaceDetailFragment(item)
                    } else {
                        replaceWithEmptyFragment()
                    }
                }
            }
        }

        lifecycleScope.launch {
            libraryViewModel.isAddingItem.collectLatest { isAdding ->
                if (isTwoPaneMode) {
                    if (isAdding) {
                        libraryViewModel.addItemType.value?.let { type ->
                            replaceDetailWithAddFragment(type)
                        }
                    } else {
                        libraryViewModel.selectedItem.value?.let {
                            replaceDetailFragment(it)
                        } ?: replaceWithEmptyFragment()
                    }
                }
            }
        }
    }

    private fun setupInitialUi(savedInstanceState: Bundle?) {
        if (isTwoPaneMode) {
            if (supportFragmentManager.findFragmentById(R.id.listContainer) == null) {
                supportFragmentManager.commit {
                    replace(R.id.listContainer, ListFragment())
                }
            }
            if (savedInstanceState == null) {
                when {
                    libraryViewModel.isAddingItem.value -> libraryViewModel.addItemType.value?.let { replaceDetailWithAddFragment(it) }
                    libraryViewModel.selectedItem.value != null -> replaceDetailFragment(libraryViewModel.selectedItem.value!!)
                    else -> replaceWithEmptyFragment()
                }
            }
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = try { findNavController(R.id.nav_host_fragment) } catch (e: Exception) { null }

                if (isTwoPaneMode) {
                    when {
                        libraryViewModel.isAddingItem.value -> libraryViewModel.completeAddItem()
                        libraryViewModel.selectedItem.value != null -> libraryViewModel.setSelectedItem(null)
                        else -> finish()
                    }
                } else if (navController != null) {
                    if (navController.currentDestination?.id == R.id.detailFragment) {
                        if (libraryViewModel.isAddingItem.value) {
                            libraryViewModel.completeAddItem()
                        } else if (libraryViewModel.selectedItem.value != null) {
                            libraryViewModel.setSelectedItem(null)
                        }
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    } else {
                        if (isEnabled) {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            }
        })
    }

    override fun onItemSelected(item: LibraryItem) {
        if (!isTwoPaneMode) {
            navigateToDetailFragment(editable = false, item = item)
        }
    }

    override fun onAddItemClicked() {
        val optionTypes = arrayOf(
            DetailFragment.TYPE_BOOK, DetailFragment.TYPE_NEWSPAPER, DetailFragment.TYPE_DISK
        )
        val optionsRu = arrayOf(
            getString(R.string.item_type_book),
            getString(R.string.item_type_newspaper),
            getString(R.string.item_type_disk)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_item_title)
            .setItems(optionsRu) { _, which ->
                val itemType = optionTypes[which]
                libraryViewModel.startAddItem(itemType)
                if (!isTwoPaneMode) {
                    navigateToDetailFragment(editable = true, itemType = itemType, item = null)
                }
            }
            .show()
    }

    private fun navigateToDetailFragment(editable: Boolean, item: LibraryItem?, itemType: String? = null) {
        if (isTwoPaneMode) return

        val type = itemType ?: item?.let { getItemType(it) } ?: return

        val bundle = Bundle().apply {
            putBoolean("editable", editable)
            putString("item_type", type)
            item?.let { putParcelable("item", it) }
        }

        try {
            val navController = findNavController(R.id.nav_host_fragment)
            if (navController.currentDestination?.id == R.id.listFragment) {
                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
            } else {
                //
            }
        } catch (e: Exception) { //
        }
    }

    private fun replaceDetailFragment(item: LibraryItem) {
        if (!isTwoPaneMode) return
        val fragment = DetailFragment.newInstance(editable = false, itemType = getItemType(item), item = item)
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.detailContainer, fragment, "DetailFragmentTag")
        }
    }

    private fun replaceDetailWithAddFragment(itemType: String) {
        if (!isTwoPaneMode) return
        val fragment = DetailFragment.newInstance(editable = true, itemType = itemType, item = null)
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.detailContainer, fragment, "AddFragmentTag")
        }
    }

    private fun replaceWithEmptyFragment() {
        if (!isTwoPaneMode) return
        if (!libraryViewModel.isAddingItem.value) {
            supportFragmentManager.commit(allowStateLoss = true) {
                replace(R.id.detailContainer, EmptyDetailFragment(), "EmptyFragmentTag")
            }
        }
    }

    private fun getItemType(item: LibraryItem): String = when (item) {
        is Book -> DetailFragment.TYPE_BOOK
        is Newspaper -> DetailFragment.TYPE_NEWSPAPER
        is Disk -> DetailFragment.TYPE_DISK
        else -> throw IllegalArgumentException("Unknown LibraryItem type: ${item::class.java.name}")
    }
}