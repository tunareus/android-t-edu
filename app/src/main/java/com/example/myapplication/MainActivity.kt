package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import com.example.myapplication.databinding.ActivityMainBinding
import android.content.DialogInterface

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
                libraryViewModel.selectedItem.value?.let { item ->
                    onItemSelected(item)
                }
            }
        }

        libraryViewModel.selectedItem.observe(this) { item ->
            if (isTwoPaneMode) {
                if (item != null) {
                    val fragment = DetailFragment.newInstance(
                        editable = false,
                        itemType = getItemType(item),
                        item = item
                    )
                    supportFragmentManager.commit {
                        replace(R.id.detailContainer, fragment)
                    }
                } else {
                    supportFragmentManager.commit {
                        replace(R.id.detailContainer, EmptyDetailFragment())
                    }
                }
            } else {
                //
            }
        }

        if (isTwoPaneMode) {
            if (supportFragmentManager.findFragmentById(R.id.listContainer) !is ListFragment) {
                supportFragmentManager.commit {
                    replace(R.id.listContainer, ListFragment())
                }
            }

            if (libraryViewModel.selectedItem.value == null) {
                supportFragmentManager.commit {
                    replace(R.id.detailContainer, EmptyDetailFragment())
                }
            }
        } else {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            if (savedInstanceState == null) {
                navController.navigate(R.id.listFragment)
            }
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

        if (isTwoPaneMode) {
            val fragment = DetailFragment.newInstance(
                editable = false,
                itemType = getItemType(item),
                item = item
            )
            supportFragmentManager.commit {
                replace(R.id.detailContainer, fragment)
            }
        } else {
            val navController = findNavController(R.id.nav_host_fragment)

            when (navController.currentDestination?.id) {
                R.id.listFragment -> {
                    val bundle = Bundle().apply {
                        putBoolean("editable", false)
                        putString("item_type", getItemType(item))
                        putParcelable("item", item)
                    }
                    navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                }
                R.id.detailFragment -> {
                    navController.popBackStack(R.id.listFragment, false)

                    navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) {
                            owner.lifecycle.removeObserver(this)
                            val bundle = Bundle().apply {
                                putBoolean("editable", false)
                                putString("item_type", getItemType(item))
                                putParcelable("item", item)
                            }
                            navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                        }
                    })
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

        val optionsRu = arrayOf(
            "Книга",
            "Газета",
            "Диск"
        )

        AlertDialog.Builder(this)
            .setTitle("Выберите тип элемента")
            .setItems(optionsRu) { _: DialogInterface, which: Int ->
                val itemType = optionTypes[which]

                if (isTwoPaneMode) {
                    val fragment = DetailFragment.newInstance(editable = true, itemType = itemType)
                    supportFragmentManager.commit {
                        replace(R.id.detailContainer, fragment)
                    }
                } else {
                    val navController = findNavController(R.id.nav_host_fragment)

                    if (navController.currentDestination?.id == R.id.listFragment) {
                        val bundle = Bundle().apply {
                            putBoolean("editable", true)
                            putString("item_type", itemType)
                        }
                        navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                    } else {
                        navController.navigate(R.id.listFragment)
                        navController.currentBackStackEntry?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
                            override fun onResume(owner: LifecycleOwner) {
                                val bundle = Bundle().apply {
                                    putBoolean("editable", true)
                                    putString("item_type", itemType)
                                }
                                navController.navigate(R.id.action_listFragment_to_detailFragment, bundle)
                                owner.lifecycle.removeObserver(this)
                            }
                        })
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