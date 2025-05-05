package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.SortField
import com.example.myapplication.data.settings.SortOrder
import com.example.myapplication.data.settings.SortPreference
import com.example.myapplication.databinding.FragmentListBinding
import com.example.myapplication.ui.adapter.GoogleBooksAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LibraryViewModel
    private lateinit var localAdapter: LibraryItemAdapter
    private lateinit var googleBooksAdapter: GoogleBooksAdapter
    private lateinit var settingsRepository: SettingsRepository

    interface OnItemSelectedListener {
        fun onItemSelected(item: LibraryItem)
        fun onAddItemClicked()
    }
    private var listener: OnItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnItemSelectedListener
        settingsRepository = SettingsRepository(requireContext().applicationContext)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupAdapters()
        setupRecyclerView()
        setupModeSwitcher()
        setupSortSpinner()
        setupGoogleSearch()
        setupUIListeners()
        observeViewModel()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext().applicationContext, viewLifecycleOwner.lifecycleScope)
        val repository = LibraryRepository(database.libraryItemDao())
        val factory = LibraryViewModel.LibraryViewModelFactory(repository, settingsRepository)
        viewModel = ViewModelProvider(requireActivity(), factory)[LibraryViewModel::class.java]
    }

    private fun setupAdapters() {
        localAdapter = LibraryItemAdapter().apply {
            itemClickListener = { item ->
                viewModel.setSelectedLocalItem(item)
                listener?.onItemSelected(item)
            }
        }
        googleBooksAdapter = GoogleBooksAdapter { googleBook ->
            viewModel.saveGoogleBookToLocalDb(googleBook)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        (binding.recyclerView.itemAnimator as? SimpleItemAnimator)?.let {
            it.supportsChangeAnimations = false
            it.addDuration = 0
            it.removeDuration = 0
            it.moveDuration = 0
        }

        val swipeCallback = SwipeToDeleteCallback { position ->
            if (binding.recyclerView.adapter == localAdapter) {
                val itemToDelete = localAdapter.currentList.getOrNull(position) as? AdapterItem.Data
                itemToDelete?.let { viewModel.deleteLocalItem(it.libraryItem) }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (recyclerView.adapter != localAdapter) return
                super.onScrolled(recyclerView, dx, dy)
                if (dy == 0 && dx == 0) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCountInAdapter = localAdapter.itemCount
                if (totalItemCountInAdapter == 0) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                if (dy > 0 && lastVisible >= totalItemCountInAdapter - 1 - LibraryViewModel.PREFETCH_DISTANCE) {
                    viewModel.loadMoreLocalItemsBottom()
                }
                if (dy < 0 && firstVisible <= LibraryViewModel.PREFETCH_DISTANCE) {
                    viewModel.loadMoreLocalItemsTop()
                }
            }
        })
    }

    private fun setupModeSwitcher() {
        binding.buttonLocalLibrary.setOnClickListener { viewModel.switchMode(AppMode.LOCAL_LIBRARY) }
        binding.buttonGoogleBooks.setOnClickListener { viewModel.switchMode(AppMode.GOOGLE_BOOKS) }
    }

    private fun setupSortSpinner() {
        val sortOptions = listOf(
            getString(R.string.sort_date_desc), getString(R.string.sort_date_asc),
            getString(R.string.sort_name_asc), getString(R.string.sort_name_desc)
        )
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = spinnerAdapter
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currentPref = viewModel.sortPreference.value
                val selectedPref = when (position) {
                    0 -> SortPreference(SortField.DATE_ADDED, SortOrder.DESC)
                    1 -> SortPreference(SortField.DATE_ADDED, SortOrder.ASC)
                    2 -> SortPreference(SortField.NAME, SortOrder.ASC)
                    3 -> SortPreference(SortField.NAME, SortOrder.DESC)
                    else -> currentPref
                }
                if (selectedPref != currentPref) viewModel.setSortPreference(selectedPref)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateSpinnerSelection(preference: SortPreference) {
        val position = when (preference.field) {
            SortField.DATE_ADDED -> if (preference.order == SortOrder.DESC) 0 else 1
            SortField.NAME -> if (preference.order == SortOrder.ASC) 2 else 3
        }
        if (binding.sortSpinner.selectedItemPosition != position) {
            val listener = binding.sortSpinner.onItemSelectedListener
            binding.sortSpinner.onItemSelectedListener = null
            binding.sortSpinner.setSelection(position, false)
            binding.sortSpinner.onItemSelectedListener = listener
        }
    }

    private fun setupGoogleSearch() {
        binding.googleSearchButton.isEnabled = false
        val textWatcher = { _: CharSequence?, _: Int, _: Int, _: Int ->
            val author = binding.googleAuthorInput.text.toString().trim()
            val title = binding.googleTitleInput.text.toString().trim()
            binding.googleSearchButton.isEnabled = author.length >= 3 || title.length >= 3
            viewModel.updateSearchQuery(author = author, title = title)
        }
        binding.googleAuthorInput.doOnTextChanged(textWatcher)
        binding.googleTitleInput.doOnTextChanged(textWatcher)

        binding.googleSearchButton.setOnClickListener {
            hideKeyboard(it)
            viewModel.searchGoogleBooks()
        }
    }

    private fun setupUIListeners() {
        binding.addButton.setOnClickListener { listener?.onAddItemClicked() }
        binding.retryButton.setOnClickListener {
            if (viewModel.currentMode.value == AppMode.LOCAL_LIBRARY) {
                viewModel.loadInitialLocalItems()
            } else {
                viewModel.searchGoogleBooks()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentMode.collectLatest { mode ->
                updateUiForMode(mode)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (viewModel.currentMode.value == AppMode.LOCAL_LIBRARY) {
                    handleLocalLibraryUiState(state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.displayedItems, viewModel.paginationState) { items, pagState ->
                if (viewModel.currentMode.value == AppMode.LOCAL_LIBRARY) {
                    mutableListOf<AdapterItem>().apply {
                        if (pagState is PaginationState.LoadingBefore) add(AdapterItem.LoadingTop)
                        addAll(items.map { AdapterItem.Data(it) })
                        if (pagState is PaginationState.LoadingAfter) add(AdapterItem.LoadingBottom)
                    }
                } else { emptyList() }
            }.collectLatest { adapterItems ->
                if (_binding != null && binding.recyclerView.adapter == localAdapter) {
                    localAdapter.submitList(adapterItems) {
                        handleLocalEmptyState(adapterItems.none { it is AdapterItem.Data } && viewModel.uiState.value is UiState.Success)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.googleBooksState.collectLatest { state ->
                if (viewModel.currentMode.value == AppMode.GOOGLE_BOOKS) {
                    handleGoogleBooksUiState(state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortPreference.collectLatest { preference ->
                if (_binding != null) updateSpinnerSelection(preference)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toastMessage.collect { message ->
                context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun updateUiForMode(mode: AppMode) {
        if (_binding == null) return
        val isLocalMode = mode == AppMode.LOCAL_LIBRARY
        binding.sortSpinner.isVisible = isLocalMode
        binding.googleSearchLayout.isVisible = !isLocalMode
        binding.addButton.isVisible = isLocalMode

        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.isVisible = false
        binding.errorLayout.isVisible = false
        binding.emptyLayout.isVisible = false
        binding.recyclerView.isVisible = false

        if (isLocalMode) {
            binding.recyclerView.adapter = localAdapter
            handleLocalLibraryUiState(viewModel.uiState.value)
            localAdapter.submitList(viewModel.displayedItems.value.map { AdapterItem.Data(it) }) {
                handleLocalEmptyState(viewModel.displayedItems.value.isEmpty() && viewModel.uiState.value is UiState.Success)
            }
        } else {
            binding.googleAuthorInput.text?.clear()
            binding.googleTitleInput.text?.clear()

            binding.recyclerView.adapter = googleBooksAdapter
            handleGoogleBooksUiState(viewModel.googleBooksState.value)
        }
    }

    private fun handleLocalLibraryUiState(state: UiState) {
        if (_binding == null || viewModel.currentMode.value != AppMode.LOCAL_LIBRARY) return
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.isVisible = false
        binding.errorLayout.isVisible = false
        binding.emptyLayout.isVisible = false
        binding.recyclerView.isVisible = false

        when (state) {
            is UiState.InitialLoading -> {
                binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.isVisible = true
            }
            is UiState.Success -> {
                binding.recyclerView.isVisible = true
                handleLocalEmptyState(state.itemCount == 0)
            }
            is UiState.Error -> {
                binding.errorLayout.isVisible = true
                val errorMessage = state.exception.localizedMessage ?: state.exception.toString()
                binding.errorTextView.text = getString(R.string.error_loading_message, errorMessage)
            }
            is UiState.Loading -> { }
        }
    }

    private fun handleLocalEmptyState(isEmpty: Boolean) {
        if (_binding == null || viewModel.currentMode.value != AppMode.LOCAL_LIBRARY) return
        val isLoading = viewModel.uiState.value is UiState.InitialLoading || viewModel.paginationState.value != PaginationState.Idle
        val isError = viewModel.uiState.value is UiState.Error

        if (isEmpty && !isLoading && !isError) {
            binding.emptyLayout.isVisible = true
            binding.recyclerView.isVisible = false
        } else {
            binding.emptyLayout.isVisible = false
            if (!isLoading && !isError && !isEmpty) {
                binding.recyclerView.isVisible = true
            } else {
                binding.recyclerView.isVisible = false
            }
        }
    }

    private fun handleGoogleBooksUiState(state: GoogleBooksUiState) {
        if (_binding == null || viewModel.currentMode.value != AppMode.GOOGLE_BOOKS) return
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.isVisible = state is GoogleBooksUiState.Loading
        binding.errorLayout.isVisible = state is GoogleBooksUiState.Error
        binding.recyclerView.isVisible = state is GoogleBooksUiState.Success

        when (state) {
            is GoogleBooksUiState.Loading -> binding.shimmerLayout.startShimmer()
            is GoogleBooksUiState.Success -> {
                googleBooksAdapter.submitList(state.books)
                binding.googleSearchLayout.isVisible = state.books.isEmpty()
                binding.recyclerView.isVisible = state.books.isNotEmpty()
            }
            is GoogleBooksUiState.Error -> {
                val errorMessage = state.exception.localizedMessage ?: state.exception.toString()
                binding.errorTextView.text = getString(R.string.error_loading_message, errorMessage)
                binding.googleSearchLayout.isVisible = true
                binding.recyclerView.isVisible = false
            }
            is GoogleBooksUiState.Idle -> {
                binding.googleSearchLayout.isVisible = true
                binding.recyclerView.isVisible = false
                googleBooksAdapter.submitList(emptyList())
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}