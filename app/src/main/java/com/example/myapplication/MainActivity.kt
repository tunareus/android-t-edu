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
                if (isTwoPaneMode) {
                    libraryViewModel.selectedItem.value?.let { item ->
                        replaceDetailFragment(item)
                    } ?: replaceWithEmptyFragment()
                }
            }
        }

        libraryViewModel.selectedItem.observe(this) { item ->
            if (isTwoPaneMode) {
                if (item != null) {
                    replaceDetailFragment(item)
                } else {
                    replaceWithEmptyFragment()
                }
            }
        }

        if (isTwoPaneMode) {
            if (supportFragmentManager.findFragmentById(R.id.listContainer) == null) {
                supportFragmentManager.commit {
                    replace(R.id.listContainer, ListFragment())
                }
            }
            if (libraryViewModel.selectedItem.value == null && savedInstanceState == null) {
                replaceWithEmptyFragment()
            } else if (libraryViewModel.selectedItem.value != null) {
                replaceDetailFragment(libraryViewModel.selectedItem.value!!)
            }
        } else {
            //
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTwoPaneMode) {
                    if (libraryViewModel.selectedItem.value != null) {
                        libraryViewModel.setSelectedItem(null)
                    } else {
                        finish()
                    }
                } else {
                    val navController = findNavController(R.id.nav_host_fragment)
                    if (navController.currentDestination?.id == R.id.listFragment) {
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
            val navController = findNavController(R.id.nav_host_fragment)
            val bundle = Bundle().apply {
                putBoolean("editable", false)
                putString("item_type", getItemType(item))
                putParcelable("item", item)
            }

            when (navController.currentDestination?.id) {
                R.id.listFragment -> {
                    navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                }
                R.id.detailFragment -> {
                    navController.popBackStack(R.id.listFragment, false)
                    navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) {
                            owner.lifecycle.removeObserver(this)
                            navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                        }
                    })
                }
                else -> {
                    navController.popBackStack(R.id.listFragment, false)
                }
            }
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
                libraryViewModel.setSelectedItem(null)

                if (isTwoPaneMode) {
                    val fragment = DetailFragment.newInstance(editable = true, itemType = itemType, item = null)
                    supportFragmentManager.commit {
                        replace(R.id.detailContainer, fragment)
                    }
                } else {
                    val navController = findNavController(R.id.nav_host_fragment)
                    val bundle = Bundle().apply {
                        putBoolean("editable", true)
                        putString("item_type", itemType)
                    }
                    if (navController.currentDestination?.id == R.id.detailFragment) {
                        navController.popBackStack(R.id.listFragment, false)
                        navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                            override fun onResume(owner: LifecycleOwner) {
                                owner.lifecycle.removeObserver(this)
                                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                            }
                        })
                    } else {
                        navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                    }
                }
            }
            .show()
    }

    private fun replaceDetailFragment(item: LibraryItem) {
        if (!isTwoPaneMode) return
        val fragment = DetailFragment.newInstance(
            editable = false,
            itemType = getItemType(item),
            item = item
        )
        supportFragmentManager.commit {
            replace(R.id.detailContainer, fragment)
        }
    }

    private fun replaceWithEmptyFragment() {
        if (!isTwoPaneMode) return
        supportFragmentManager.commit {
            replace(R.id.detailContainer, EmptyDetailFragment())
        }
    }


    private fun getItemType(item: LibraryItem): String = when (item) {
        is Book -> DetailFragment.TYPE_BOOK
        is Newspaper -> DetailFragment.TYPE_NEWSPAPER
        is Disk -> DetailFragment.TYPE_DISK
        else -> ""
    }
}