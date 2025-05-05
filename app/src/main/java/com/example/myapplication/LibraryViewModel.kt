package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.local.mapper.toEntity
import com.example.myapplication.data.mapper.toLibraryItem
import com.example.myapplication.data.remote.GoogleBooksApiService
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.SortPreference
import com.example.myapplication.domain.GoogleBookItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.math.max
import kotlin.math.min

enum class AppMode { LOCAL_LIBRARY, GOOGLE_BOOKS }

sealed class GoogleBooksUiState {
    data object Idle : GoogleBooksUiState()
    data object Loading : GoogleBooksUiState()
    data class Success(val books: List<GoogleBookItem>) : GoogleBooksUiState()
    data class Error(val exception: Throwable) : GoogleBooksUiState()
}

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
    private val settingsRepository: SettingsRepository,
    private val googleBooksApiService: GoogleBooksApiService = RetrofitClient.googleBooksApi
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 30
        const val LOAD_MORE_COUNT = 8
        const val PREFETCH_DISTANCE = 5
        const val MIN_SHIMMER_TIME_MS = 1000L
        const val GOOGLE_BOOKS_PAGE_SIZE = 20
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
    private val _googleBooksState = MutableStateFlow<GoogleBooksUiState>(GoogleBooksUiState.Idle)
    val googleBooksState: StateFlow<GoogleBooksUiState> = _googleBooksState.asStateFlow()
    private val _googleSearchQueryAuthor = MutableStateFlow("")
    val googleSearchQueryAuthor: StateFlow<String> = _googleSearchQueryAuthor.asStateFlow()
    private val _googleSearchQueryTitle = MutableStateFlow("")
    val googleSearchQueryTitle: StateFlow<String> = _googleSearchQueryTitle.asStateFlow()
    private val _currentMode = MutableStateFlow(AppMode.LOCAL_LIBRARY)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()
    private val _toastMessage = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var localCurrentOffset = 0
    private var localTotalItemsCount = 0
    private var localLoadJob: Job? = null
    private var googleBooksJob: Job? = null

    init {
        viewModelScope.launch {
            _sortPreference.value = settingsRepository.getSortPreference()
            loadInitialLocalItems()
        }
    }

    fun switchMode(mode: AppMode) {
        if (_currentMode.value == mode) return
        _currentMode.value = mode
        if (mode == AppMode.GOOGLE_BOOKS) {
            _googleBooksState.value = GoogleBooksUiState.Idle
            _googleSearchQueryAuthor.value = ""
            _googleSearchQueryTitle.value = ""
        } else {
            loadInitialLocalItems()
        }
    }

    fun loadInitialLocalItems() {
        localLoadJob?.cancel()
        _paginationState.value = PaginationState.LoadingInitial
        _uiState.value = UiState.InitialLoading
        localLoadJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                localTotalItemsCount = repository.getTotalItemCount()
                val items = repository.getPagedItems(PAGE_SIZE, 0, _sortPreference.value)
                ensureActive()
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < MIN_SHIMMER_TIME_MS) {
                    delay(MIN_SHIMMER_TIME_MS - elapsedTime)
                }
                _displayedItems.value = items
                localCurrentOffset = 0
                _uiState.value = UiState.Success(localTotalItemsCount)
                _paginationState.value = PaginationState.Idle
            } catch (e: CancellationException) {
                _paginationState.value = PaginationState.Idle
            } catch (e: Exception) {
                ensureActive()
                val rootCause = e.cause ?: e
                _uiState.value = UiState.Error(rootCause)
                _paginationState.value = PaginationState.Error(rootCause)
            }
        }
    }

    fun loadMoreLocalItemsTop() {
        if (_paginationState.value !is PaginationState.Idle || localCurrentOffset <= 0) return
        _paginationState.value = PaginationState.LoadingBefore
        viewModelScope.launch {
            try {
                val loadCount = min(LOAD_MORE_COUNT, localCurrentOffset)
                val newOffset = max(0, localCurrentOffset - loadCount)
                val itemsToPrepend = repository.getPagedItems(loadCount, newOffset, _sortPreference.value)
                ensureActive()
                if (itemsToPrepend.isNotEmpty()) {
                    val currentList = _displayedItems.value
                    val itemsToRemoveCount = min(itemsToPrepend.size, currentList.size)
                    _displayedItems.value = itemsToPrepend + currentList.dropLast(itemsToRemoveCount)
                    localCurrentOffset = newOffset
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

    fun loadMoreLocalItemsBottom() {
        val currentListSize = _displayedItems.value.size
        val itemsAfterCurrent = localTotalItemsCount - (localCurrentOffset + currentListSize)
        if (_paginationState.value !is PaginationState.Idle || itemsAfterCurrent <= 0) return
        _paginationState.value = PaginationState.LoadingAfter
        viewModelScope.launch {
            try {
                val nextOffset = localCurrentOffset + currentListSize
                val loadLimit = min(LOAD_MORE_COUNT, itemsAfterCurrent)
                val itemsToAppend = repository.getPagedItems(loadLimit, nextOffset, _sortPreference.value)
                ensureActive()
                if (itemsToAppend.isNotEmpty()) {
                    val currentList = _displayedItems.value
                    val itemsToRemoveCount = min(itemsToAppend.size, currentList.size)
                    _displayedItems.value = currentList.drop(itemsToRemoveCount) + itemsToAppend
                    localCurrentOffset += itemsToRemoveCount
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
            try {
                settingsRepository.saveSortPreference(preference)
                _sortPreference.value = preference
                loadInitialLocalItems()
            } catch (e: Exception) {
                _toastMessage.tryEmit("Не удалось сохранить настройку сортировки")
            }
        }
    }

    fun addManuallyCreatedItem(item: LibraryItem) {
        viewModelScope.launch {
            try {
                val entity = item.toEntity(id = 0, isbnValue = null)
                repository.addItem(entity)
                _toastMessage.tryEmit("Элемент '${item.name}' добавлен.")
                if (_currentMode.value == AppMode.LOCAL_LIBRARY) {
                    loadInitialLocalItems()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _toastMessage.tryEmit("Ошибка добавления элемента: ${e.message}")
            }
        }
    }

    fun deleteLocalItem(item: LibraryItem) {
        viewModelScope.launch {
            try {
                val deleted = repository.removeItem(item)
                if (deleted) {
                    _toastMessage.tryEmit("Элемент '${item.name}' удален.")
                    if (_currentMode.value == AppMode.LOCAL_LIBRARY) {
                        loadInitialLocalItems()
                    }
                } else {
                    _toastMessage.tryEmit("Не удалось удалить '${item.name}'. Элемент не найден.")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _toastMessage.tryEmit("Ошибка удаления элемента: ${e.message}")
            }
        }
    }

    fun setSelectedLocalItem(item: LibraryItem?) {
        if (_isAddingItem.value) completeAddItem()
        _selectedItem.value = item
    }

    fun updateSearchQuery(author: String? = null, title: String? = null) {
        author?.let { _googleSearchQueryAuthor.value = it }
        title?.let { _googleSearchQueryTitle.value = it }
    }

    fun searchGoogleBooks() {
        val authorQuery = _googleSearchQueryAuthor.value.trim()
        val titleQuery = _googleSearchQueryTitle.value.trim()

        if (authorQuery.length < 3 && titleQuery.length < 3) {
            _toastMessage.tryEmit( "Введите минимум 3 символа для автора или названия")
            if (_googleBooksState.value is GoogleBooksUiState.Loading) {
                _googleBooksState.value = GoogleBooksUiState.Idle
            }
            return
        }

        googleBooksJob?.cancel()
        _googleBooksState.value = GoogleBooksUiState.Loading

        googleBooksJob = viewModelScope.launch {
            try {
                val queryParts = mutableListOf<String>()
                if (authorQuery.isNotBlank()) queryParts.add("inauthor:\"$authorQuery\"")
                if (titleQuery.isNotBlank()) queryParts.add("intitle:\"$titleQuery\"")
                val combinedQuery = queryParts.joinToString("+")

                if (combinedQuery.isBlank()) {
                    _googleBooksState.value = GoogleBooksUiState.Idle
                    return@launch
                }

                val response = googleBooksApiService.searchBooks(
                    query = combinedQuery,
                    maxResults = GOOGLE_BOOKS_PAGE_SIZE
                )
                ensureActive()

                val books = response.items?.mapNotNull { it.toDomain() } ?: emptyList()
                _googleBooksState.value = GoogleBooksUiState.Success(books)

            } catch (e: CancellationException) {
                _googleBooksState.value = GoogleBooksUiState.Idle
            } catch (e: Exception) {
                ensureActive()
                _googleBooksState.value = GoogleBooksUiState.Error(e)
                _toastMessage.tryEmit("Ошибка поиска книг: ${e.message ?: "Неизвестная ошибка сети"}")
            }
        }
    }

    fun saveGoogleBookToLocalDb(googleBook: GoogleBookItem) {
        viewModelScope.launch {
            try {
                val isbnToSave = googleBook.isbn
                var isDuplicate = false

                if (!isbnToSave.isNullOrBlank()) {
                    val existingItemByIsbn = repository.findByIsbn(isbnToSave)
                    if (existingItemByIsbn != null) {
                        isDuplicate = true
                        _toastMessage.tryEmit("Книга '${googleBook.title}' (ISBN: $isbnToSave) уже есть в библиотеке.")
                    }
                }

                if (!isDuplicate) {
                    val existingByNameAuthor = repository.findByNameAndAuthor(googleBook.title, googleBook.authorsFormatted)
                    if (existingByNameAuthor.isNotEmpty()) {
                        isDuplicate = true
                        _toastMessage.tryEmit("Книга '${googleBook.title}' от '${googleBook.authorsFormatted}' уже есть в библиотеке (ID: ${existingByNameAuthor.first().id}).")
                    }
                }

                if (!isDuplicate) {
                    val libraryItem = googleBook.toLibraryItem()
                    val entity = libraryItem.toEntity(id = 0, isbnValue = isbnToSave)
                    repository.addItem(entity)
                    _toastMessage.tryEmit("Книга '${googleBook.title}' сохранена.")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _toastMessage.tryEmit("Ошибка сохранения книги: ${e.message}")
            }
        }
    }

    fun startAddItem(itemType: String) {
        if (_selectedItem.value != null) _selectedItem.value = null
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