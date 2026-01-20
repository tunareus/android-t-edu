package com.example.myapplication.presentation.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.domain.model.GoogleBookDetails
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.domain.usecase.googlebooks.SaveGoogleBookToLocalLibraryUseCase
import com.example.myapplication.domain.usecase.googlebooks.SearchGoogleBooksUseCase
import com.example.myapplication.domain.usecase.library.AddLocalItemUseCase
import com.example.myapplication.domain.usecase.library.DeleteLocalItemUseCase
import com.example.myapplication.domain.usecase.library.GetPagedLocalItemsUseCase
import com.example.myapplication.domain.usecase.library.GetTotalLocalItemCountUseCase
import com.example.myapplication.domain.usecase.settings.GetSortPreferenceUseCase
import com.example.myapplication.domain.usecase.settings.SetSortPreferenceUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

enum class AppMode { LOCAL_LIBRARY, GOOGLE_BOOKS }

sealed class GoogleBooksUiState {
    data object Idle : GoogleBooksUiState()
    data object Loading : GoogleBooksUiState()
    data class Success(val books: List<GoogleBookDetails>) : GoogleBooksUiState()
    data class Error(val message: String) : GoogleBooksUiState()
}

sealed class UiState {
    data object InitialLoading : UiState()
    data object Loading : UiState()
    data class Success(val itemCount: Int) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class PaginationState {
    data object Idle : PaginationState()
    data object LoadingInitial : PaginationState()
    data object LoadingBefore : PaginationState()
    data object LoadingAfter : PaginationState()
}

class LibraryViewModel @Inject constructor(
    application: Application,
    private val getPagedLocalItemsUseCase: GetPagedLocalItemsUseCase,
    private val getTotalLocalItemCountUseCase: GetTotalLocalItemCountUseCase,
    private val addLocalItemUseCase: AddLocalItemUseCase,
    private val deleteLocalItemUseCase: DeleteLocalItemUseCase,
    private val searchGoogleBooksUseCase: SearchGoogleBooksUseCase,
    private val saveGoogleBookToLocalLibraryUseCase: SaveGoogleBookToLocalLibraryUseCase,
    private val getSortPreferenceUseCase: GetSortPreferenceUseCase,
    private val setSortPreferenceUseCase: SetSortPreferenceUseCase
) : AndroidViewModel(application) {

    companion object {
        const val PAGE_SIZE = 30
        const val LOAD_MORE_COUNT = 8
        const val PREFETCH_DISTANCE = 5
        const val MIN_SHIMMER_TIME_MS = 1000L
        const val GOOGLE_BOOKS_PAGE_SIZE = 20
        private const val TAG = "LibraryViewModel"
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

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
    private fun getString(resId: Int, vararg formatArgs: Any): String = getApplication<Application>().getString(resId, *formatArgs)

    init {
        viewModelScope.launch {
            getSortPreferenceUseCase().fold(
                onSuccess = { pref -> _sortPreference.value = pref },
                onFailure = {
                    Log.e(TAG, "Error loading sort preference", it)
                    _toastMessage.tryEmit(
                        it.localizedMessage ?: getString(R.string.error_loading_settings)
                    )
                }
            )
            loadInitialLocalItems()
        }
    }

    fun switchMode(mode: AppMode) {
        if (_currentMode.value == mode) return
        Log.d(TAG, "Switching mode to: $mode")
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
        Log.d(TAG, "loadInitialLocalItems called")
        localLoadJob?.cancel()
        _paginationState.value = PaginationState.LoadingInitial
        _uiState.value = UiState.InitialLoading
        localLoadJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                getTotalLocalItemCountUseCase().fold(
                    onSuccess = { count ->
                        Log.d(TAG, "Total item count: $count")
                        localTotalItemsCount = count
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to get total item count", it)
                        handleGenericError(it, ErrorSource.LOCAL_TOTAL_COUNT)
                        return@launch
                    }
                )

                getPagedLocalItemsUseCase(PAGE_SIZE, 0, _sortPreference.value).fold(
                    onSuccess = { items ->
                        Log.d(TAG, "Loaded initial items: ${items.size}")
                        val elapsedTime = System.currentTimeMillis() - startTime
                        if (elapsedTime < MIN_SHIMMER_TIME_MS && items.isNotEmpty()) {
                            delay(MIN_SHIMMER_TIME_MS - elapsedTime)
                        }
                        _displayedItems.value = items
                        localCurrentOffset = 0
                        _uiState.value = UiState.Success(localTotalItemsCount)
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to load initial items", it)
                        handleGenericError(it, ErrorSource.LOCAL_INITIAL_LOAD)
                    }
                )
            } catch (e: CancellationException) {
                Log.d(TAG, "loadInitialLocalItems cancelled")
            } finally {
                if (_paginationState.value == PaginationState.LoadingInitial) {
                    _paginationState.value = PaginationState.Idle
                }
            }
        }
    }

    fun loadMoreLocalItemsTop() {
        if (_paginationState.value !is PaginationState.Idle || localCurrentOffset <= 0) return
        Log.d(TAG, "loadMoreLocalItemsTop called, offset: $localCurrentOffset")
        _paginationState.value = PaginationState.LoadingBefore
        viewModelScope.launch {
            try {
                val loadCount = min(LOAD_MORE_COUNT, localCurrentOffset)
                val newOffset = max(0, localCurrentOffset - loadCount)

                getPagedLocalItemsUseCase(loadCount, newOffset, _sortPreference.value).fold(
                    onSuccess = { itemsToPrepend ->
                        Log.d(TAG, "Loaded items for top: ${itemsToPrepend.size}")
                        if (itemsToPrepend.isNotEmpty()) {
                            val currentList = _displayedItems.value
                            val itemsToRemoveCount = min(itemsToPrepend.size, max(0, currentList.size + itemsToPrepend.size - PAGE_SIZE))
                            _displayedItems.value = itemsToPrepend + currentList.dropLast(itemsToRemoveCount)
                            localCurrentOffset = newOffset
                        }
                    },
                    onFailure = {
                        Log.e(TAG, "Error loading previous items", it)
                        _toastMessage.tryEmit(formatErrorMessage(it, ErrorSource.LOCAL_PAGINATION_TOP))
                    }
                )
            } catch (e: CancellationException) {
                Log.d(TAG, "loadMoreLocalItemsTop cancelled")
            } finally {
                if (_paginationState.value == PaginationState.LoadingBefore) {
                    _paginationState.value = PaginationState.Idle
                }
            }
        }
    }

    fun loadMoreLocalItemsBottom() {
        val currentListSize = _displayedItems.value.size
        val itemsEffectivelyInView = localCurrentOffset + currentListSize
        if (itemsEffectivelyInView >= localTotalItemsCount || _paginationState.value !is PaginationState.Idle) return
        Log.d(TAG, "loadMoreLocalItemsBottom called, offset: $localCurrentOffset, currentListSize: $currentListSize, total: $localTotalItemsCount")
        _paginationState.value = PaginationState.LoadingAfter
        viewModelScope.launch {
            try {
                val nextOffsetToLoadFrom = localCurrentOffset + currentListSize
                val itemsRemaining = localTotalItemsCount - nextOffsetToLoadFrom
                val loadLimit = min(LOAD_MORE_COUNT, itemsRemaining)

                if (loadLimit <= 0) {
                    _paginationState.value = PaginationState.Idle
                    return@launch
                }

                getPagedLocalItemsUseCase(loadLimit, nextOffsetToLoadFrom, _sortPreference.value).fold(
                    onSuccess = { itemsToAppend ->
                        Log.d(TAG, "Loaded items for bottom: ${itemsToAppend.size}")
                        if (itemsToAppend.isNotEmpty()) {
                            val currentList = _displayedItems.value
                            val itemsToRemoveCount = min(itemsToAppend.size, max(0, currentList.size + itemsToAppend.size - PAGE_SIZE))
                            _displayedItems.value = currentList.drop(itemsToRemoveCount) + itemsToAppend
                            localCurrentOffset += itemsToRemoveCount
                        }
                    },
                    onFailure = {
                        Log.e(TAG, "Error loading next items", it)
                        _toastMessage.tryEmit(formatErrorMessage(it, ErrorSource.LOCAL_PAGINATION_BOTTOM))
                    }
                )
            } catch (e: CancellationException) {
                Log.d(TAG, "loadMoreLocalItemsBottom cancelled")
            } finally {
                if (_paginationState.value == PaginationState.LoadingAfter) {
                    _paginationState.value = PaginationState.Idle
                }
            }
        }
    }

    fun setSortPreference(preference: SortPreference) {
        if (_sortPreference.value == preference || _paginationState.value !is PaginationState.Idle) return
        Log.d(TAG, "Setting sort preference to: $preference")
        viewModelScope.launch {
            setSortPreferenceUseCase(preference).fold(
                onSuccess = {
                    _sortPreference.value = preference
                    loadInitialLocalItems()
                },
                onFailure = {
                    Log.e(TAG, "Failed to save sort preference", it)
                    _toastMessage.tryEmit(
                        it.localizedMessage ?: getString(R.string.error_saving_settings)
                    )
                }
            )
        }
    }

    fun addManuallyCreatedItem(item: LibraryItem) {
        Log.d(TAG, "Adding manually created item: ${item.name}")
        viewModelScope.launch {
            addLocalItemUseCase(item).fold(
                onSuccess = { _ ->
                    _toastMessage.tryEmit(getString(R.string.item_added_success, item.name))
                    if (_currentMode.value == AppMode.LOCAL_LIBRARY) {
                        loadInitialLocalItems()
                    }
                },
                onFailure = {
                    Log.e(TAG, "Error adding item", it)
                    _toastMessage.tryEmit(formatErrorMessage(it, ErrorSource.LOCAL_ADD_ITEM))
                }
            )
        }
    }

    fun deleteLocalItem(item: LibraryItem) {
        Log.d(TAG, "Deleting local item: ${item.name}")
        viewModelScope.launch {
            deleteLocalItemUseCase(item).fold(
                onSuccess = { deleted ->
                    if (deleted) {
                        _toastMessage.tryEmit(getString(R.string.item_deleted_success, item.name))
                        if (_currentMode.value == AppMode.LOCAL_LIBRARY) {
                            loadInitialLocalItems()
                        }
                    } else {
                        _toastMessage.tryEmit(getString(R.string.item_delete_not_found, item.name))
                    }
                },
                onFailure = {
                    Log.e(TAG, "Error deleting item", it)
                    _toastMessage.tryEmit(formatErrorMessage(it, ErrorSource.LOCAL_DELETE_ITEM))
                }
            )
        }
    }

    fun setSelectedLocalItem(item: LibraryItem?) {
        Log.d(TAG, "Setting selected local item: ${item?.name}")
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
        Log.d(TAG, "Searching Google Books with author: '$authorQuery', title: '$titleQuery'")

        if (authorQuery.length < 3 && titleQuery.length < 3) {
            _toastMessage.tryEmit(getString(R.string.google_search_error_min_chars))
            if (_googleBooksState.value is GoogleBooksUiState.Loading) {
                _googleBooksState.value = GoogleBooksUiState.Idle
            }
            return
        }

        googleBooksJob?.cancel()
        _googleBooksState.value = GoogleBooksUiState.Loading
        viewModelScope.launch {
            searchGoogleBooksUseCase(authorQuery, titleQuery, GOOGLE_BOOKS_PAGE_SIZE).fold(
                onSuccess = { books ->
                    Log.d(TAG, "Google Books search success, found: ${books.size}")
                    _googleBooksState.value = GoogleBooksUiState.Success(books)
                    if (books.isEmpty() && (authorQuery.isNotEmpty() || titleQuery.isNotEmpty())) {
                        _toastMessage.tryEmit(getString(R.string.google_books_no_results))
                    }
                },
                onFailure = {
                    Log.e(TAG, "Google Books search error", it)
                    val errorMessage = formatErrorMessage(it, ErrorSource.GOOGLE_SEARCH)
                    _googleBooksState.value = GoogleBooksUiState.Error(errorMessage)
                }
            )
        }
    }

    fun saveGoogleBookToLocalDb(googleBook: GoogleBookDetails) {
        Log.d(TAG, "Saving Google Book to local DB: ${googleBook.title}")
        viewModelScope.launch {
            saveGoogleBookToLocalLibraryUseCase(googleBook).let { result ->
                when (result) {
                    is SaveGoogleBookToLocalLibraryUseCase.SaveResult.Success -> {
                        _toastMessage.tryEmit(getString(R.string.google_save_success, result.bookTitle))
                        if (_currentMode.value == AppMode.LOCAL_LIBRARY) {
                            loadInitialLocalItems()
                        }
                    }
                    is SaveGoogleBookToLocalLibraryUseCase.SaveResult.AlreadyExists -> {
                        _toastMessage.tryEmit(getString(R.string.google_save_already_exists_detailed, result.bookTitle, result.reason))
                    }
                    is SaveGoogleBookToLocalLibraryUseCase.SaveResult.Failure -> {
                        Log.e(TAG, "Error saving Google Book", result.error)
                        _toastMessage.tryEmit(getString(R.string.google_save_error, formatErrorMessage(result.error, ErrorSource.GOOGLE_SAVE)))
                    }
                }
            }
        }
    }

    fun startAddItem(itemType: String) {
        Log.d(TAG, "Starting add item of type: $itemType")
        if (_selectedItem.value != null) _selectedItem.value = null
        _addItemType.value = itemType
        _isAddingItem.value = true
    }

    fun completeAddItem() {
        Log.d(TAG, "Completing add item")
        _addItemType.value = null
        _isAddingItem.value = false
    }

    private enum class ErrorSource {
        LOCAL_INITIAL_LOAD, LOCAL_TOTAL_COUNT, LOCAL_PAGINATION_TOP, LOCAL_PAGINATION_BOTTOM,
        LOCAL_ADD_ITEM, LOCAL_DELETE_ITEM,
        GOOGLE_SEARCH, GOOGLE_SAVE,
        SETTINGS_LOAD, SETTINGS_SAVE
    }

    private fun handleGenericError(throwable: Throwable, source: ErrorSource) {
        val message = formatErrorMessage(throwable, source)
        Log.w(TAG, "Handling generic error from $source: $message", throwable)
        when (source) {
            ErrorSource.LOCAL_INITIAL_LOAD, ErrorSource.LOCAL_TOTAL_COUNT -> {
                _uiState.value = UiState.Error(message)
                _paginationState.value = PaginationState.Idle
            }
            ErrorSource.GOOGLE_SEARCH -> {
                _googleBooksState.value = GoogleBooksUiState.Error(message)
            }
            else -> {
                _toastMessage.tryEmit(message)
            }
        }
    }

    private fun formatErrorMessage(throwable: Throwable, source: ErrorSource): String {
        Log.w(TAG, "Formatting error message for source: $source, error: ${throwable::class.java.simpleName}", throwable)
        return when (throwable) {
            is SQLiteException -> getString(R.string.error_database_operation_failed)
            is IOException -> {
                if (throwable.message?.startsWith("HTTP error") == true) {
                    val parts = throwable.message?.split(" ")
                    val httpCode = parts?.getOrNull(2)?.removeSuffix(":")?.toIntOrNull()
                    if (httpCode != null) {
                        getString(R.string.error_server_communication, httpCode)
                    } else {
                        throwable.message ?: getString(R.string.error_network_connection)
                    }
                } else {
                    getString(R.string.error_network_connection)
                }
            }
            is IllegalArgumentException -> throwable.localizedMessage ?: getString(R.string.error_validation_generic)
            else -> {
                val defaultForSource = when (source) {
                    ErrorSource.LOCAL_INITIAL_LOAD, ErrorSource.LOCAL_TOTAL_COUNT -> getString(R.string.error_loading_data)
                    ErrorSource.LOCAL_PAGINATION_TOP, ErrorSource.LOCAL_PAGINATION_BOTTOM -> getString(R.string.error_loading_more_data)
                    ErrorSource.LOCAL_ADD_ITEM -> getString(R.string.error_adding_item)
                    ErrorSource.LOCAL_DELETE_ITEM -> getString(R.string.error_deleting_item)
                    ErrorSource.GOOGLE_SEARCH -> getString(R.string.google_search_error_generic_vm)
                    ErrorSource.GOOGLE_SAVE -> getString(R.string.google_save_error_generic_vm)
                    ErrorSource.SETTINGS_LOAD -> getString(R.string.error_loading_settings)
                    ErrorSource.SETTINGS_SAVE -> getString(R.string.error_saving_settings)
                }
                throwable.localizedMessage?.takeIf { it.isNotBlank() } ?: defaultForSource
            }
        }
    }
}