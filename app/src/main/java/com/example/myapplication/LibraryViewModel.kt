package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LibraryViewModel : ViewModel() {

    private val _libraryItems = MutableLiveData<List<LibraryItem>>(initializeLibrary())
    val libraryItems: LiveData<List<LibraryItem>> get() = _libraryItems

    fun onItemCreated(item: LibraryItem) {
        val currentList = _libraryItems.value?.toMutableList() ?: mutableListOf()
        currentList.add(item)
        currentList.sortBy { it.id }
        _libraryItems.value = currentList
    }

    fun removeItem(position: Int) {
        val currentList = _libraryItems.value?.toMutableList() ?: return
        if (position !in currentList.indices) return
        val removedItem = currentList.removeAt(position)
        UniqueIdGenerator.releaseId(removedItem.id)
        currentList.sortBy { it.id }
        _libraryItems.value = currentList
    }
}