package com.example.myapplication

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
                libraryViewModel.addItemType.value?.let { type ->
                    replaceDetailWithAddFragment(type)
                }
            } else if (libraryViewModel.selectedItem.value != null) {
                libraryViewModel.selectedItem.value?.let { item ->
                    replaceDetailFragment(item)
                }
            } else {
                replaceWithEmptyFragment()
            }
        } else {
            if (libraryViewModel.isAddingItem.value) {
                libraryViewModel.addItemType.value?.let { type ->
                    navigateToDetailFragment(editable = true, itemType = type, item = null, forceReset = true)
                }
            } else if (libraryViewModel.selectedItem.value != null) {
                libraryViewModel.selectedItem.value?.let { item ->
                    navigateToDetailFragment(editable = false, item = item, forceReset = true)
                }
            } else {
                try {
                    val navController = findNavController(R.id.nav_host_fragment)
                    if (navController.currentDestination?.id != R.id.listFragment) {
                        navController.popBackStack(R.id.listFragment, false)
                    }
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    private fun setupViewModelObservers() {
        lifecycleScope.launch {
            libraryViewModel.selectedItem.collectLatest { item ->
                if (isTwoPaneMode) {
                    if (!libraryViewModel.isAddingItem.value) {
                        if (item != null) {
                            replaceDetailFragment(item)
                        } else {
                            replaceWithEmptyFragment()
                        }
                    } else {
                        //
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
                        libraryViewModel.selectedItem.value?.let { item ->
                            replaceDetailFragment(item)
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
                if (libraryViewModel.isAddingItem.value) {
                    libraryViewModel.addItemType.value?.let { replaceDetailWithAddFragment(it) }
                } else if (libraryViewModel.selectedItem.value != null) {
                    replaceDetailFragment(libraryViewModel.selectedItem.value!!)
                } else {
                    replaceWithEmptyFragment()
                }
            }
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTwoPaneMode) {
                    if (libraryViewModel.isAddingItem.value) {
                        libraryViewModel.completeAddItem()
                    } else if (libraryViewModel.selectedItem.value != null) {
                        libraryViewModel.setSelectedItem(null)
                    } else {
                        finish()
                    }
                } else {
                    val navController = findNavController(R.id.nav_host_fragment)
                    if (navController.currentDestination?.id == R.id.detailFragment) {
                        if (libraryViewModel.isAddingItem.value) {
                            libraryViewModel.completeAddItem()
                        } else {
                            libraryViewModel.setSelectedItem(null)
                        }
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    } else if (navController.currentDestination?.id == R.id.listFragment) {
                        finish()
                    } else {
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    }
                }
            }
        })
    }

    override fun onItemSelected(item: LibraryItem) {
        libraryViewModel.setSelectedItem(item)
        if (!isTwoPaneMode) {
            navigateToDetailFragment(editable = false, item = item, forceReset = false)
        }
    }

    override fun onAddItemClicked() {
        val optionTypes = arrayOf(
            DetailFragment.TYPE_BOOK,
            DetailFragment.TYPE_NEWSPAPER,
            DetailFragment.TYPE_DISK
        )
        val optionsRu = arrayOf("Книга", "Газета", "Диск")

        AlertDialog.Builder(this)
            .setTitle("Выберите тип элемента для добавления")
            .setItems(optionsRu) { _: DialogInterface, which: Int ->
                val itemType = optionTypes[which]
                libraryViewModel.startAddItem(itemType)
                if (!isTwoPaneMode) {
                    navigateToDetailFragment(editable = true, itemType = itemType, item = null, forceReset = false)
                }
            }
            .show()
    }

    private fun navigateToDetailFragment(editable: Boolean, item: LibraryItem?, itemType: String? = null, forceReset: Boolean) {
        if (isTwoPaneMode) return

        val type = itemType ?: item?.let { getItemType(it) }
        if (type.isNullOrEmpty()) {
            return
        }

        val bundle = Bundle().apply {
            putBoolean("editable", editable)
            putString("item_type", type)
            item?.let { putParcelable("item", it) }
        }

        try {
            val navController = findNavController(R.id.nav_host_fragment)

            if (forceReset || navController.currentDestination?.id == R.id.detailFragment) {
                val listFragmentInBackStack = navController.previousBackStackEntry?.destination?.id == R.id.listFragment
                if (listFragmentInBackStack) {
                    navController.popBackStack(R.id.listFragment, false)
                    navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) {
                            owner.lifecycle.removeObserver(this)
                            if (navController.currentDestination?.id == R.id.listFragment) {
                                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                            } else {
                                //
                            }
                        }
                    })
                } else {
                    try {
                        navController.popBackStack(R.id.listFragment, true)
                        navController.navigate(R.id.listFragment)
                        navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                            override fun onResume(owner: LifecycleOwner) {
                                owner.lifecycle.removeObserver(this)
                                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                            }
                        })

                    } catch (navEx: Exception) {
                        //
                    }
                }
            } else if (navController.currentDestination?.id == R.id.listFragment) {
                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
            } else {
                //
            }
        } catch (e: Exception) {
            try {
                findNavController(R.id.nav_host_fragment).popBackStack(R.id.listFragment, false)
            } catch (_: Exception) {}
        }
    }

    private fun replaceDetailFragment(item: LibraryItem) {
        if (!isTwoPaneMode) return
        val fragment = DetailFragment.newInstance(
            editable = false,
            itemType = getItemType(item),
            item = item
        )
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.detailContainer, fragment, "DetailFragmentTag")
        }
    }

    private fun replaceDetailWithAddFragment(itemType: String) {
        if (!isTwoPaneMode) return
        val fragment = DetailFragment.newInstance(
            editable = true,
            itemType = itemType,
            item = null
        )
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
        } else {
            //
        }
    }

    private fun getItemType(item: LibraryItem): String = when (item) {
        is Book -> DetailFragment.TYPE_BOOK
        is Newspaper -> DetailFragment.TYPE_NEWSPAPER
        is Disk -> DetailFragment.TYPE_DISK
        else -> {
            ""
        }
    }
}