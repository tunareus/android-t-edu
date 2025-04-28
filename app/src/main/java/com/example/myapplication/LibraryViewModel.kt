package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.SortPreference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.math.max
import kotlin.math.min

sealed class UiState {
    data object InitialLoading : UiState()
    data object Loading : UiState()
    data class Success(val itemCount: Int) : UiState()
    data class Error(val exception: Throwable) : UiState()
}
sealed class PaginationState {
    data object Idle : PaginationState()
    data object LoadingInitial : PaginationState()
    data object LoadingBefore : PaginationState()
    data object LoadingAfter : PaginationState()
    data class Error(val exception: Throwable) : PaginationState()
}

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 30
        const val LOAD_MORE_COUNT = 15
        const val PREFETCH_DISTANCE = 10
        const val MIN_SHIMMER_TIME_MS = 1000L
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.InitialLoading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _displayedItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val displayedItems: StateFlow<List<LibraryItem>> = _displayedItems.asStateFlow()

    private val _paginationState = MutableStateFlow<PaginationState>(PaginationState.Idle)
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    private val _sortPreference = MutableStateFlow(SortPreference())
    val sortPreference: StateFlow<SortPreference> = _sortPreference.asStateFlow()

    private val _selectedItem = MutableStateFlow<LibraryItem?>(null)
    val selectedItem: StateFlow<LibraryItem?> = _selectedItem.asStateFlow()
    private val _isAddingItem = MutableStateFlow(false)
    val isAddingItem: StateFlow<Boolean> = _isAddingItem.asStateFlow()
    private val _addItemType = MutableStateFlow<String?>(null)
    val addItemType: StateFlow<String?> = _addItemType.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var currentOffset = 0
    private var totalItemsCount = 0
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            _sortPreference.value = settingsRepository.getSortPreference()
            loadInitialItems()
        }
    }

    fun loadInitialItems() {
        loadJob?.cancel()
        _paginationState.value = PaginationState.LoadingInitial
        _uiState.value = UiState.InitialLoading

        loadJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                totalItemsCount = repository.getTotalItemCount()
                val items = repository.getPagedItems(PAGE_SIZE, 0, _sortPreference.value)
                ensureActive()

                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < MIN_SHIMMER_TIME_MS) {
                    delay(MIN_SHIMMER_TIME_MS - elapsedTime)
                }

                _displayedItems.value = items
                currentOffset = 0
                _uiState.value = UiState.Success(totalItemsCount)
                _paginationState.value = PaginationState.Idle

            } catch (e: CancellationException) {
                _paginationState.value = PaginationState.Idle
            } catch (e: Exception) {
                ensureActive()
                _uiState.value = UiState.Error(e)
                _paginationState.value = PaginationState.Error(e)
            }
        }
    }

    fun loadMoreItemsTop() {
        if (_paginationState.value !is PaginationState.Idle || currentOffset <= 0) return

        _paginationState.value = PaginationState.LoadingBefore
        viewModelScope.launch {
            try {
                val loadCount = min(LOAD_MORE_COUNT, currentOffset)
                val newOffset = max(0, currentOffset - loadCount)
                val itemsToPrepend = repository.getPagedItems(loadCount, newOffset, _sortPreference.value)
                ensureActive()

                if (itemsToPrepend.isNotEmpty()) {
                    val currentList = _displayedItems.value
                    val itemsToRemoveCount = min(itemsToPrepend.size, currentList.size)
                    _displayedItems.value = itemsToPrepend + currentList.dropLast(itemsToRemoveCount)
                    currentOffset = newOffset
                }
                _paginationState.value = PaginationState.Idle

            } catch (e: CancellationException) {
                _paginationState.value = PaginationState.Idle
            } catch (e: Exception) {
                ensureActive()
                _toastMessage.tryEmit("Ошибка загрузки предыдущих элементов: ${e.message}")
                _paginationState.value = PaginationState.Idle
            }
        }
    }

    fun loadMoreItemsBottom() {
        val currentListSize = _displayedItems.value.size
        val itemsAfterCurrent = totalItemsCount - (currentOffset + currentListSize)

        if (_paginationState.value !is PaginationState.Idle || itemsAfterCurrent <= 0) return

        _paginationState.value = PaginationState.LoadingAfter
        viewModelScope.launch {
            try {
                val nextOffset = currentOffset + currentListSize
                val loadLimit = min(LOAD_MORE_COUNT, itemsAfterCurrent)
                val itemsToAppend = repository.getPagedItems(loadLimit, nextOffset, _sortPreference.value)
                ensureActive()

                if (itemsToAppend.isNotEmpty()) {
                    val currentList = _displayedItems.value
                    val itemsToRemoveCount = min(itemsToAppend.size, currentList.size)
                    _displayedItems.value = currentList.drop(itemsToRemoveCount) + itemsToAppend
                    currentOffset += itemsToRemoveCount
                }
                _paginationState.value = PaginationState.Idle

            } catch (e: CancellationException) {
                _paginationState.value = PaginationState.Idle
            } catch (e: Exception) {
                ensureActive()
                _toastMessage.tryEmit("Ошибка загрузки следующих элементов: ${e.message}")
                _paginationState.value = PaginationState.Idle
            }
        }
    }

    fun setSortPreference(preference: SortPreference) {
        if (_sortPreference.value == preference || _paginationState.value !is PaginationState.Idle) return

        viewModelScope.launch {
            settingsRepository.saveSortPreference(preference)
            _sortPreference.value = preference
            loadInitialItems()
        }
    }

    fun addNewItem(item: LibraryItem) {
        viewModelScope.launch {
            try {
                repository.addItem(item)
                _toastMessage.tryEmit("Элемент '${item.name}' добавлен.")
                loadInitialItems()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _toastMessage.tryEmit("Ошибка добавления элемента: ${e.message}")
            }
        }
    }

    fun deleteItem(item: LibraryItem) {
        viewModelScope.launch {
            try {
                val deleted = repository.removeItem(item)
                if (deleted) {
                    _toastMessage.tryEmit("Элемент '${item.name}' удален.")
                    loadInitialItems()
                } else {
                    _toastMessage.tryEmit("Не удалось удалить '${item.name}'. Элемент не найден.")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _toastMessage.tryEmit("Ошибка удаления элемента: ${e.message}")
            }
        }
    }

    fun setSelectedItem(item: LibraryItem?) {
        if (_isAddingItem.value) {
            completeAddItem()
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
        _addItemType.value = null
        _isAddingItem.value = false
    }

    class LibraryViewModelFactory(
        private val repository: LibraryRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(repository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}