package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive

sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: List<LibraryItem>) : UiState()
    data class Error(val exception: Throwable) : UiState()
}

class LibraryViewModel : ViewModel() {

    private val repository: LibraryRepository by lazy { LibraryRepository() }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedItem = MutableStateFlow<LibraryItem?>(null)
    val selectedItem: StateFlow<LibraryItem?> = _selectedItem.asStateFlow()

    private val _isAddingItem = MutableStateFlow(false)
    val isAddingItem: StateFlow<Boolean> = _isAddingItem.asStateFlow()

    private val _addItemType = MutableStateFlow<String?>(null)
    val addItemType: StateFlow<String?> = _addItemType.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _scrollToPosition = MutableSharedFlow<Int>(replay = 0)
    val scrollToPosition: SharedFlow<Int> = _scrollToPosition.asSharedFlow()

    private var loadJob: Job? = null

    init {
        loadLibraryItems()
    }

    fun loadLibraryItems(scrollToItemId: Int? = null) {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            if (_uiState.value !is UiState.Loading) {
                _uiState.value = UiState.Loading
            }

            runCatching {
                repository.getItems()
            }.onSuccess { items ->
                ensureActive()
                _uiState.value = UiState.Success(items)
                scrollToItemId?.let { targetId ->
                    val position = items.indexOfFirst { it.id == targetId }
                    if (position != -1) {
                        _scrollToPosition.emit(position)
                    }
                }
            }.onFailure { exception ->
                ensureActive()
                if (exception is CancellationException) {
                    throw exception
                }
                _uiState.value = UiState.Error(exception)
            }
        }
    }

    fun setSelectedItem(item: LibraryItem?) {
        if (_isAddingItem.value) {
            _isAddingItem.value = false
            _addItemType.value = null
        }
        _selectedItem.value = item
    }

    fun startAddItem(itemType: String) {
        if (_selectedItem.value != null) {
            _selectedItem.value = null
        }
        _addItemType.value = itemType
        _isAddingItem.value = true
    }

    fun completeAddItem() {
        if (_isAddingItem.value) {
            _addItemType.value = null
            _isAddingItem.value = false
        }
    }

    fun addNewItem(item: LibraryItem) {
        viewModelScope.launch {
            runCatching {
                repository.addItem(item)
            }.onSuccess {
                _toastMessage.tryEmit("Элемент '${item.name}' добавлен.")
                loadLibraryItems(scrollToItemId = item.id)
            }.onFailure { exception ->
                if (exception is CancellationException) throw exception
                _toastMessage.tryEmit("Ошибка добавления элемента: ${exception.message}")
            }
        }
    }

    fun deleteItemAtPosition(position: Int) {
        viewModelScope.launch {
            val currentItems = (_uiState.value as? UiState.Success)?.data
            if (currentItems != null && position in currentItems.indices) {
                val itemToDelete = currentItems[position]
                runCatching {
                    repository.removeItem(itemToDelete)
                }.onSuccess {
                    _toastMessage.tryEmit("Элемент '${itemToDelete.name}' удален.")
                    loadLibraryItems()
                }.onFailure { exception ->
                    if (exception is CancellationException) throw exception
                    _toastMessage.tryEmit("Ошибка удаления элемента: ${exception.message}")
                }
            } else {
                _toastMessage.tryEmit("Не удалось удалить элемент: список не загружен или позиция неверна.")
            }
        }
    }
}