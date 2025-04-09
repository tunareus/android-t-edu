package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LibraryViewModel : ViewModel() {

    private val _libraryItems = MutableLiveData(initializeLibrary().toMutableList())
    val libraryItems: LiveData<MutableList<LibraryItem>> get() = _libraryItems

    fun onItemCreated(item: LibraryItem) {
        val currentList = _libraryItems.value ?: mutableListOf()
        currentList.add(item)
        currentList.sortBy { it.id }
        _libraryItems.value = currentList
    }

    fun removeItem(position: Int) {
        val currentList = _libraryItems.value ?: return
        if (position !in currentList.indices) return
        val removedItem = currentList.removeAt(position)
        UniqueIdGenerator.releaseId(removedItem.id)
        currentList.sortBy { it.id }
        _libraryItems.value = currentList
    }
}