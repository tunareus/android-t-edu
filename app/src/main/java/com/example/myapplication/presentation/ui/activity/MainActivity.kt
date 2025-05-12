package com.example.myapplication.presentation.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.domain.model.Book
import com.example.myapplication.domain.model.Disk
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.Newspaper
import com.example.myapplication.presentation.ui.fragment.DetailFragment
import com.example.myapplication.presentation.ui.fragment.EmptyDetailFragment
import com.example.myapplication.presentation.ui.fragment.ListFragment
import com.example.myapplication.presentation.viewmodel.LibraryViewModel
import com.example.myapplication.presentation.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ListFragment.OnItemSelectedListener {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var isTwoPaneMode = false
    private var navController: NavController? = null

    private val libraryViewModel: LibraryViewModel by lazy {
        val myApplication = application as MyApplication
        val factory = LibraryViewModelFactory(
            myApplication,
            myApplication.getPagedLocalItemsUseCase,
            myApplication.getTotalLocalItemCountUseCase,
            myApplication.addLocalItemUseCase,
            myApplication.deleteLocalItemUseCase,
            myApplication.getLocalItemByIdUseCase,
            myApplication.findLocalBookByIsbnUseCase,
            myApplication.findLocalBookByNameAndAuthorUseCase,
            myApplication.searchGoogleBooksUseCase,
            myApplication.saveGoogleBookToLocalLibraryUseCase,
            myApplication.getSortPreferenceUseCase,
            myApplication.setSortPreferenceUseCase
        )
        ViewModelProvider(this, factory)[LibraryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        isTwoPaneMode = binding.detailContainer != null

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navController = navHostFragment?.navController

        if (isTwoPaneMode) {
            if (supportFragmentManager.findFragmentById(R.id.listContainer) == null) {
                supportFragmentManager.commit {
                    replace(R.id.listContainer, ListFragment::class.java, null)
                }
            }
        }

        setupViewModelObservers()
        setupOnBackPressed()

        if (savedInstanceState == null && isTwoPaneMode) {
            synchronizeDetailPaneWithViewModelState()
        }
    }

    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            libraryViewModel.selectedItem.collectLatest { item ->
                if (isTwoPaneMode && !libraryViewModel.isAddingItem.value) {
                    synchronizeDetailPaneWithViewModelState()
                }
            }
        }

        lifecycleScope.launch {
            libraryViewModel.isAddingItem.collectLatest { isAdding ->
                if (isTwoPaneMode) {
                    synchronizeDetailPaneWithViewModelState()
                }
            }
        }
    }

    private fun synchronizeDetailPaneWithViewModelState() {
        if (!isTwoPaneMode) return

        val currentFragmentInDetailView = supportFragmentManager.findFragmentById(R.id.detailContainer)

        if (libraryViewModel.isAddingItem.value) {
            libraryViewModel.addItemType.value?.let { type ->
                val existingTag = "AddFragmentTag_${type}"
                if (currentFragmentInDetailView?.tag != existingTag) {
                    replaceDetailFragmentContainer(null, true, type, existingTag)
                }
            } ?: replaceWithEmptyFragment()
        } else if (libraryViewModel.selectedItem.value != null) {
            libraryViewModel.selectedItem.value?.let { item ->
                val existingTag = "DetailFragmentTag_${item.id}"
                if (currentFragmentInDetailView?.tag != existingTag) {
                    replaceDetailFragmentContainer(item, false, getItemTypeString(item), existingTag)
                }
            }
        } else {
            if (currentFragmentInDetailView !is EmptyDetailFragment) {
                replaceWithEmptyFragment()
            }
        }
    }


    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTwoPaneMode) {
                    when {
                        libraryViewModel.isAddingItem.value -> libraryViewModel.completeAddItem()
                        libraryViewModel.selectedItem.value != null -> libraryViewModel.setSelectedLocalItem(null)
                        else -> finish()
                    }
                } else {
                    val currentDestId = navController?.currentDestination?.id
                    if (currentDestId == R.id.detailFragment) {
                        if (libraryViewModel.isAddingItem.value) libraryViewModel.completeAddItem()
                        if (libraryViewModel.selectedItem.value != null) libraryViewModel.setSelectedLocalItem(null)

                        if (navController?.popBackStack() != true) finish()
                    } else {
                        if (navController?.popBackStack() != true) finish()
                    }
                }
            }
        })
    }

    override fun onItemSelected(item: LibraryItem) {
        libraryViewModel.setSelectedLocalItem(item)
        if (!isTwoPaneMode) {
            navigateToDetailFragment(false, item)
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
                    navigateToDetailFragment(true, null, itemType)
                }
            }
            .show()
    }

    private fun navigateToDetailFragment(editable: Boolean, item: LibraryItem?, itemType: String? = null) {
        if (isTwoPaneMode) return

        val typeForNav = itemType ?: item?.let { getItemTypeString(it) }
        if (typeForNav == null && editable) {
            Toast.makeText(this, "Item type for new item is unknown.", Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putBoolean("editable", editable)
            putString("itemType", typeForNav ?: DetailFragment.TYPE_BOOK)
            item?.let { putParcelable("item", it) }
        }
        navController?.navigate(R.id.action_listFragment_to_detailFragment, bundle)
    }

    private fun replaceDetailFragmentContainer(item: LibraryItem?, editable: Boolean, itemType: String?, tag: String) {
        if (!isTwoPaneMode) return

        val typeForFragment = itemType ?: item?.let { getItemTypeString(it) } ?: DetailFragment.TYPE_BOOK

        val currentFragment = supportFragmentManager.findFragmentById(R.id.detailContainer)
        if (currentFragment?.tag == tag && currentFragment is DetailFragment) {
        }

        val fragment = DetailFragment.newInstance(
            editable = editable,
            itemType = typeForFragment,
            item = item
        )
        supportFragmentManager.commit { //
            replace(R.id.detailContainer, fragment, tag)
        }
    }

    private fun replaceWithEmptyFragment() {
        if (!isTwoPaneMode) return
        val currentFragment = supportFragmentManager.findFragmentById(R.id.detailContainer)
        if (!libraryViewModel.isAddingItem.value && currentFragment !is EmptyDetailFragment) {
            supportFragmentManager.commit {
                replace(R.id.detailContainer, EmptyDetailFragment::class.java, null, "EmptyFragmentTag")
            }
        }
    }

    private fun getItemTypeString(item: LibraryItem): String = when (item) {
        is Book -> DetailFragment.TYPE_BOOK
        is Newspaper -> DetailFragment.TYPE_NEWSPAPER
        is Disk -> DetailFragment.TYPE_DISK
    }
}