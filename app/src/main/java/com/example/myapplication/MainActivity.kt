package com.example.myapplication

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
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
        Log.d("MainActivity", "Attempting to restore state after rotation...")
        if (isTwoPaneMode) {
            if (libraryViewModel.isAddingItem.value == true) {
                libraryViewModel.addItemType.value?.let { type ->
                    Log.d("MainActivity", "Restoring AddItem state (two-pane)")
                    replaceDetailWithAddFragment(type)
                } ?: Log.w("MainActivity", "isAddingItem is true but addItemType is null during restore (two-pane)")
            } else if (libraryViewModel.selectedItem.value != null) {
                libraryViewModel.selectedItem.value?.let { item ->
                    Log.d("MainActivity", "Restoring SelectedItem state (two-pane)")
                    replaceDetailFragment(item)
                }
            } else {
                Log.d("MainActivity", "Restoring Empty state (two-pane)")
                replaceWithEmptyFragment()
            }
        } else {
            if (libraryViewModel.isAddingItem.value == true) {
                libraryViewModel.addItemType.value?.let { type ->
                    Log.d("MainActivity", "Restoring AddItem state (single-pane)")
                    navigateToDetailFragment(editable = true, itemType = type, item = null, forceReset = true)
                } ?: Log.w("MainActivity", "isAddingItem is true but addItemType is null during restore (single-pane)")
            } else if (libraryViewModel.selectedItem.value != null) {
                libraryViewModel.selectedItem.value?.let { item ->
                    Log.d("MainActivity", "Restoring SelectedItem state (single-pane)")
                    navigateToDetailFragment(editable = false, item = item, forceReset = true)
                }
            } else {
                Log.d("MainActivity", "No item selected or adding in ViewModel (single-pane restore). Ensuring ListFragment is shown.")
                try {
                    val navController = findNavController(R.id.nav_host_fragment)
                    if (navController.currentDestination?.id != R.id.listFragment) {
                        navController.popBackStack(R.id.listFragment, false)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error ensuring ListFragment during restore", e)
                }
            }
        }
    }

    private fun setupViewModelObservers() {
        libraryViewModel.selectedItem.observe(this) { item ->
            Log.d("MainActivity", "Observer: selectedItem changed to ${item?.name ?: "null"}")
            if (isTwoPaneMode) {
                if (libraryViewModel.isAddingItem.value == false) {
                    if (item != null) {
                        replaceDetailFragment(item)
                    } else {
                        replaceWithEmptyFragment()
                    }
                } else {
                    Log.d("MainActivity", "Observer: selectedItem changed, but in adding mode. Ignoring for two-pane update.")
                }
            }
        }

        libraryViewModel.isAddingItem.observe(this) { isAdding ->
            Log.d("MainActivity", "Observer: isAddingItem changed to $isAdding")
            if (isTwoPaneMode) {
                if (isAdding) {
                    libraryViewModel.addItemType.value?.let { type ->
                        replaceDetailWithAddFragment(type)
                    } ?: Log.w("MainActivity", "Observer: isAddingItem became true, but addItemType is null.")
                } else {
                    libraryViewModel.selectedItem.value?.let { item ->
                        replaceDetailFragment(item)
                    } ?: replaceWithEmptyFragment()
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
                if (libraryViewModel.isAddingItem.value == true) {
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
                Log.d("MainActivity", "Back button pressed.")
                if (isTwoPaneMode) {
                    if (libraryViewModel.isAddingItem.value == true) {
                        Log.d("MainActivity", "Back pressed during AddItem (two-pane). Completing add.")
                        libraryViewModel.completeAddItem()
                    } else if (libraryViewModel.selectedItem.value != null) {
                        Log.d("MainActivity", "Back pressed with selected item (two-pane). Clearing selection.")
                        libraryViewModel.setSelectedItem(null)
                    } else {
                        Log.d("MainActivity", "Back pressed with no selection/add (two-pane). Finishing.")
                        finish()
                    }
                } else {
                    val navController = findNavController(R.id.nav_host_fragment)
                    if (navController.currentDestination?.id == R.id.detailFragment) {
                        if (libraryViewModel.isAddingItem.value == true) {
                            Log.d("MainActivity", "Back pressed during AddItem (single-pane). Completing add state.")
                            libraryViewModel.completeAddItem()
                        } else {
                            Log.d("MainActivity", "Back pressed on Detail view (single-pane). Clearing selection state.")
                            libraryViewModel.setSelectedItem(null)
                        }
                        Log.d("MainActivity", "Popping back stack (single-pane).")
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    } else if (navController.currentDestination?.id == R.id.listFragment) {
                        Log.d("MainActivity", "Back pressed on ListFragment (single-pane). Finishing.")
                        finish()
                    } else {
                        Log.w("MainActivity", "Back pressed on unexpected fragment (${navController.currentDestination?.label}). Popping stack.")
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
            Log.e("MainActivity", "Cannot navigate to detail: item type is unknown or empty.")
            return
        }

        val bundle = Bundle().apply {
            putBoolean("editable", editable)
            putString("item_type", type)
            item?.let { putParcelable("item", it) }
        }

        try {
            val navController = findNavController(R.id.nav_host_fragment)
            Log.d("MainActivity", "Navigating (single-pane): editable=$editable, type=$type, item=${item?.name ?: "null"}, forceReset=$forceReset")

            if (forceReset || navController.currentDestination?.id == R.id.detailFragment) {
                Log.d("MainActivity", "Popping back to ListFragment before navigation.")
                if (navController.previousBackStackEntry?.destination?.id == R.id.listFragment || navController.graph.startDestinationId == R.id.listFragment) {
                    navController.popBackStack(R.id.listFragment, false)
                    navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) {
                            owner.lifecycle.removeObserver(this)
                            if (navController.currentDestination?.id == R.id.listFragment) {
                                Log.d("MainActivity", "Resumed ListFragment after pop, navigating to Detail.")
                                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                            } else {
                                Log.w("MainActivity", "Resumed after pop, but not on ListFragment? Current: ${navController.currentDestination?.label}. Skipping detail navigation.")
                            }
                        }
                    })
                } else {
                    Log.w("MainActivity", "ListFragment not found in back stack during forceReset/detail-to-detail. Navigating directly.")
                    if (navController.currentDestination?.id == R.id.listFragment) {
                        navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                    } else {
                        Log.e("MainActivity", "Cannot navigate to detail from current destination: ${navController.currentDestination?.label}")
                    }
                }
            } else if (navController.currentDestination?.id == R.id.listFragment) {
                Log.d("MainActivity", "Currently on ListFragment, navigating to Detail.")
                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
            } else {
                Log.w("MainActivity", "Navigation requested from unexpected state: ${navController.currentDestination?.label}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during single-pane navigation", e)
            try {
                findNavController(R.id.nav_host_fragment).popBackStack(R.id.listFragment, false)
            } catch (_: Exception) {}
        }
    }

    private fun replaceDetailFragment(item: LibraryItem) {
        if (!isTwoPaneMode) return
        Log.d("MainActivity", "Replacing detail container with item: ${item.name}")
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
        Log.d("MainActivity", "Replacing detail container with AddFragment for type: $itemType")
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
        Log.d("MainActivity", "Replacing detail container with EmptyDetailFragment.")
        if (libraryViewModel.isAddingItem.value == false) {
            supportFragmentManager.commit(allowStateLoss = true) {
                replace(R.id.detailContainer, EmptyDetailFragment(), "EmptyFragmentTag")
            }
        }
    }

    private fun getItemType(item: LibraryItem): String = when (item) {
        is Book -> DetailFragment.TYPE_BOOK
        is Newspaper -> DetailFragment.TYPE_NEWSPAPER
        is Disk -> DetailFragment.TYPE_DISK
        else -> {
            Log.w("MainActivity", "Unknown item type: ${item::class.java.simpleName}")
            ""
        }
    }
}