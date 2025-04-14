package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {

    private val _libraryItems = MutableLiveData<List<LibraryItem>>()
    val libraryItems: LiveData<List<LibraryItem>> get() = _libraryItems

    var selectedItem: LibraryItem? = null

    private val _scrollToPosition = MutableSharedFlow<Int>()
    val scrollToPosition: SharedFlow<Int> = _scrollToPosition

    init {
        if (_libraryItems.value == null) {
            UniqueIdGenerator.reset()
            _libraryItems.value = initializeLibrary()
        }
    }

    fun onItemCreated(item: LibraryItem) {
        val currentList = _libraryItems.value?.toMutableList() ?: mutableListOf()
        currentList.add(item)
        currentList.sortBy { it.id }
        _libraryItems.value = currentList

        val position = currentList.indexOfFirst { it.id == item.id }
        if (position != -1) {
            viewModelScope.launch {
                _scrollToPosition.emit(position)
            }
        }
    }

    fun removeItem(position: Int) {
        val currentList = _libraryItems.value?.toMutableList() ?: return
        if (position !in currentList.indices) return
        val removedItem = currentList.removeAt(position)
        UniqueIdGenerator.releaseId(removedItem.id)
        _libraryItems.value = currentList
    }
}