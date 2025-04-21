package com.example.myapplication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val data: List<LibraryItem>) : UiState()
    data class Error(val exception: Throwable) : UiState()
}

class LibraryViewModel : ViewModel() {

    private val repository: LibraryRepository by lazy {
        Log.d("ViewModel", ">>> Initializing LibraryRepository...")
        try {
            LibraryRepository().also {
                Log.d("ViewModel", "<<< LibraryRepository Initialized")
            }
        } catch (t: Throwable) {
            Log.e("ViewModel", "!!! CRASH DURING REPOSITORY INIT !!!", t)
            throw t
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedItem = MutableLiveData<LibraryItem?>()
    val selectedItem: LiveData<LibraryItem?> get() = _selectedItem

    private val _scrollToPosition = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val scrollToPosition: SharedFlow<Int> = _scrollToPosition.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var loadJob: Job? = null
    private var initialLoadTriggered = false

    init {
        Log.d("ViewModel", "init called. Initial state is set to Idle.")
        Log.d("ViewModel", "Accessing repository in init to trigger lazy init...")
        try {
            repository
            Log.d("ViewModel", "Repository accessed successfully in init.")
            if (!initialLoadTriggered) {
                Log.d("ViewModel", "Calling loadLibraryItems from init...")
                loadLibraryItems()
                initialLoadTriggered = true
            } else {
                Log.d("ViewModel", "loadLibraryItems already triggered, skipping call from init.")
            }
        } catch (t: Throwable) {
            Log.e("ViewModel", "Error accessing/initializing repository in init", t)
            _uiState.value = UiState.Error(t)
            initialLoadTriggered = true
        }
    }

    fun loadLibraryItems(forceRetry: Boolean = false, scrollToItemId: Int? = null) {
        Log.d("ViewModel", "+++ Entering loadLibraryItems function. forceRetry=$forceRetry, scrollToItemId=$scrollToItemId")

        Log.d("ViewModel", "Current state before launch check: ${_uiState.value::class.java.simpleName}")
        if (_uiState.value is UiState.Loading && !forceRetry) {
            Log.d("ViewModel", "Load already in progress and not forceRetry. Skipping.")
            return
        }
        if (_uiState.value is UiState.Error && !forceRetry) {
            Log.d("ViewModel", "In Error state and not forceRetry. Skipping.")
            return
        }

        if (loadJob?.isActive == true) {
            Log.d("ViewModel", "Previous job is active, cancelling...")
            loadJob?.cancel()
        } else {
            Log.d("ViewModel", "No active previous job to cancel.")
        }
        Log.d("ViewModel", "Preparing to launch new coroutine.")

        try {
            _uiState.value = UiState.Loading
            Log.d("ViewModel", ">>> State set to Loading (before launch)")

            loadJob = viewModelScope.launch {
                Log.d("ViewModel", ">>> viewModelScope.launch **STARTED**")
                val coroutineStartTime = System.currentTimeMillis()

                try {
                    Log.d("ViewModel", "Coroutine: Current state is ${_uiState.value::class.java.simpleName}")
                    Log.d("ViewModel", "Coroutine: Calling repository.getItems()")

                    val items = repository.getItems()

                    Log.d("ViewModel", "Coroutine: repository.getItems() returned ${items.size} items")

                    val elapsedTime = System.currentTimeMillis() - coroutineStartTime

                    Log.d("ViewModel", "Coroutine: Data loaded in $elapsedTime ms.")

                    if (elapsedTime < 1000) {
                        val delayNeeded = 1000 - elapsedTime
                        Log.d("ViewModel", "Coroutine: Delaying for shimmer: $delayNeeded ms")
                        delay(delayNeeded)
                    } else {
                        Log.d("ViewModel", "Coroutine: No shimmer delay needed (elapsed: $elapsedTime ms)")
                    }

                    if (isActive) {
                        _uiState.value = UiState.Success(items)
                        Log.d("ViewModel", "Coroutine: State changed to Success with ${items.size} items")

                        scrollToItemId?.let { targetId ->
                            val position = items.indexOfFirst { it.id == targetId }
                            if (position != -1) {
                                Log.d("ViewModel", "Requesting scroll to added item ID $targetId at position $position")
                                _scrollToPosition.emit(position)
                            } else {
                                Log.w("ViewModel", "Scroll target ID $targetId not found in the list after load.")
                            }
                        }

                    } else {
                        Log.d("ViewModel", "Coroutine: Job cancelled before setting Success state.")
                    }

                } catch (e: IOException) {
                    Log.e("ViewModel", "Coroutine: IOException caught: ${e.message}", e)
                    val errorTime = System.currentTimeMillis()
                    val elapsedTime = errorTime - coroutineStartTime
                    if (elapsedTime < 1000) {
                        Log.d("ViewModel", "Coroutine: Error occurred quickly, delaying for shimmer (IOException)")
                        delay(1000 - elapsedTime)
                    }
                    if (isActive) {
                        _uiState.value = UiState.Error(e)
                        Log.d("ViewModel", "Coroutine: State changed to Error (IOException)")
                    } else {
                        Log.d("ViewModel", "Coroutine: Job cancelled before setting Error state (IOException).")
                    }
                } catch (t: Throwable) {
                    Log.e("ViewModel", "!!! Coroutine: Uncaught Throwable inside launch !!!", t)
                    val errorTime = System.currentTimeMillis()
                    val elapsedTime = errorTime - coroutineStartTime
                    if (elapsedTime < 1000) {
                        Log.d("ViewModel", "Coroutine: Error occurred quickly, delaying for shimmer (Throwable)")
                        delay(1000 - elapsedTime)
                    }
                    if (isActive) {
                        _uiState.value = UiState.Error(t)
                        Log.d("ViewModel", "Coroutine: State changed to Error (Throwable)")
                    } else {
                        Log.d("ViewModel", "Coroutine: Job cancelled before setting Error state (Throwable).")
                    }
                } finally {
                    Log.d("ViewModel", "<<< Coroutine execution finished (normally or exceptionally).")
                }
            }
            Log.d("ViewModel", "--- viewModelScope.launch call finished (coroutine launched). Job: $loadJob, isActive: ${loadJob?.isActive}")

        } catch (t: Throwable) {
            Log.e("ViewModel", "!!! CRITICAL ERROR before or during launch !!!", t)
            _uiState.value = UiState.Error(t)
        }
    }

    fun setSelectedItem(item: LibraryItem?) {
        Log.d("ViewModel", "setSelectedItem called with item: ${item?.name ?: "null"}")
        _selectedItem.value = item
    }

    fun addNewItem(item: LibraryItem) {
        viewModelScope.launch {
            Log.d("ViewModel", "addNewItem started for: ${item.name}")
            try {
                repository.addItem(item)
                Log.d("ViewModel", "Item added to repo. Reloading list and requesting scroll.")
                _toastMessage.emit("Элемент '${item.name}' добавлен.")
                loadLibraryItems(forceRetry = true, scrollToItemId = item.id)

            } catch (e: Exception) {
                Log.e("ViewModel", "Error adding item: ${e.message}", e)
                _toastMessage.emit("Ошибка добавления элемента: ${e.message}")
            }
        }
    }

    fun deleteItemAtPosition(position: Int) {
        viewModelScope.launch {
            Log.d("ViewModel", "deleteItemAtPosition started for pos: $position")
            val currentList = (_uiState.value as? UiState.Success)?.data
            if (currentList != null && position in currentList.indices) {
                val itemToDelete = currentList[position]
                Log.d("ViewModel", "Attempting to remove item: ${itemToDelete.name}")
                try {
                    repository.removeItem(itemToDelete)
                    Log.d("ViewModel", "Item removed from repo. Reloading list.")
                    _toastMessage.emit("Элемент '${itemToDelete.name}' удален.")
                    loadLibraryItems(forceRetry = true)
                } catch (e: Exception) {
                    Log.e("ViewModel", "Error removing item: ${e.message}", e)
                    _toastMessage.emit("Ошибка удаления элемента: ${e.message}")
                }
            } else {
                Log.w("ViewModel", "Could not remove item at position $position. List state: ${_uiState.value::class.java.simpleName}, list size: ${currentList?.size ?: "N/A"}")
                _toastMessage.emit("Не удалось удалить элемент: список не загружен или позиция неверна.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "onCleared called, cancelling job.")
        loadJob?.cancel()
    }
}