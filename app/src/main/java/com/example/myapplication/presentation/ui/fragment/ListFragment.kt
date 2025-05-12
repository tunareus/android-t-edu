package com.example.myapplication.presentation.ui.fragment

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
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentListBinding
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.SortField
import com.example.myapplication.domain.model.SortOrder
import com.example.myapplication.domain.model.SortPreference
import com.example.myapplication.presentation.ui.SwipeToDeleteCallback
import com.example.myapplication.presentation.ui.adapter.AdapterItem
import com.example.myapplication.presentation.ui.adapter.GoogleBooksAdapter
import com.example.myapplication.presentation.ui.adapter.LibraryItemAdapter
import com.example.myapplication.presentation.viewmodel.AppMode
import com.example.myapplication.presentation.viewmodel.GoogleBooksUiState
import com.example.myapplication.presentation.viewmodel.LibraryViewModel
import com.example.myapplication.presentation.viewmodel.LibraryViewModelFactory
import com.example.myapplication.presentation.viewmodel.PaginationState
import com.example.myapplication.presentation.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LibraryViewModel
    private lateinit var localAdapter: LibraryItemAdapter
    private lateinit var googleBooksAdapter: GoogleBooksAdapter

    interface OnItemSelectedListener {
        fun onItemSelected(item: LibraryItem)
        fun onAddItemClicked()
    }
    private var listener: OnItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnItemSelectedListener
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

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupViewModel() {
        val myApplication = requireActivity().application as MyApplication
        val factory = LibraryViewModelFactory(
            myApplication,
            myApplication.getPagedLocalItemsUseCase,
            myApplication.getTotalLocalItemCountUseCase,
            myApplication.addLocalItemUseCase,
            myApplication.deleteLocalItemUseCase,
            myApplication.getLocalItemByIdUseCase,
            myApplication.findLocalBookByIsbnUseCase,
            myApplication.findLocalBookByNameAndAuthorUseCase,
            myApplication.searchGoogleBooksUseCase,
            myApplication.saveGoogleBookToLocalLibraryUseCase,
            myApplication.getSortPreferenceUseCase,
            myApplication.setSortPreferenceUseCase
        )
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
        (binding.recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        val swipeCallback = SwipeToDeleteCallback { position ->
            if (binding.recyclerView.adapter == localAdapter) {
                (localAdapter.currentList.getOrNull(position) as? AdapterItem.Data)?.libraryItem?.let {
                    viewModel.deleteLocalItem(it)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (recyclerView.adapter != localAdapter || (dx == 0 && dy == 0)) return
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCountInAdapter = localAdapter.itemCount
                if (totalItemCountInAdapter == 0) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val prefetch = LibraryViewModel.PREFETCH_DISTANCE

                if (dy > 0 && lastVisible >= totalItemCountInAdapter - 1 - prefetch) {
                    viewModel.loadMoreLocalItemsBottom()
                }
                if (dy < 0 && firstVisible <= prefetch) {
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
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
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
        binding.googleTitleInput.setOnEditorActionListener { _, _, _ ->
            if (binding.googleSearchButton.isEnabled) {
                binding.googleSearchButton.performClick()
                true
            } else false
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
                    val isTrulyEmpty = viewModel.displayedItems.value.isEmpty() &&
                            viewModel.paginationState.value == PaginationState.Idle &&
                            state is UiState.Success
                    handleLocalLibraryUiState(state, isTrulyEmpty)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.displayedItems, viewModel.paginationState, viewModel.uiState) { items, pagState, uiStateVal ->
                val isTrulyEmpty = items.isEmpty() && pagState == PaginationState.Idle && uiStateVal is UiState.Success
                Triple(items, pagState, isTrulyEmpty)
            }.collectLatest { (items, pagState, isTrulyEmpty) ->
                if (_binding != null && viewModel.currentMode.value == AppMode.LOCAL_LIBRARY) {
                    val adapterItems = mutableListOf<AdapterItem>().apply {
                        if (pagState is PaginationState.LoadingBefore) add(AdapterItem.LoadingTop)
                        addAll(items.map { AdapterItem.Data(it) })
                        if (pagState is PaginationState.LoadingAfter) add(AdapterItem.LoadingBottom)
                    }
                    localAdapter.submitList(adapterItems) {
                        handleLocalLibraryUiState(viewModel.uiState.value, isTrulyEmpty)
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
        binding.addButton.isVisible = isLocalMode
        binding.googleSearchLayout.isVisible = !isLocalMode

        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.isVisible = false
        binding.recyclerView.isVisible = false
        binding.emptyLayout.isVisible = false
        binding.errorLayout.isVisible = false

        if (isLocalMode) {
            binding.recyclerView.adapter = localAdapter
            val isTrulyEmpty = viewModel.displayedItems.value.isEmpty() &&
                    viewModel.paginationState.value == PaginationState.Idle &&
                    viewModel.uiState.value is UiState.Success
            handleLocalLibraryUiState(viewModel.uiState.value, isTrulyEmpty)
        } else {
            binding.recyclerView.adapter = googleBooksAdapter
            binding.googleAuthorInput.text?.clear()
            binding.googleTitleInput.text?.clear()
            handleGoogleBooksUiState(viewModel.googleBooksState.value)
        }
    }

    private fun handleLocalLibraryUiState(state: UiState, isCurrentListEmptyAndStable: Boolean) {
        if (_binding == null || viewModel.currentMode.value != AppMode.LOCAL_LIBRARY) return

        val isLoadingInitialOrPagination = state is UiState.InitialLoading || viewModel.paginationState.value is PaginationState.LoadingInitial
        val isError = state is UiState.Error

        binding.shimmerLayout.isVisible = false
        binding.recyclerView.isVisible = false
        binding.emptyLayout.isVisible = false
        binding.errorLayout.isVisible = false
        binding.shimmerLayout.stopShimmer()

        when {
            isLoadingInitialOrPagination -> {
                binding.shimmerLayout.isVisible = true
                binding.shimmerLayout.startShimmer()
            }
            isError -> {
                binding.errorLayout.isVisible = true
                binding.errorTextView.text = (state as UiState.Error).message
            }
            else -> {
                if (isCurrentListEmptyAndStable) {
                    binding.emptyLayout.isVisible = true
                    binding.emptyLayout.text = getString(R.string.empty_list_message)
                    binding.emptyLayout.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_item, 0, 0)
                } else {
                    val isPaginatingNonInitial = viewModel.paginationState.value is PaginationState.LoadingBefore ||
                            viewModel.paginationState.value is PaginationState.LoadingAfter
                    binding.recyclerView.isVisible = !viewModel.displayedItems.value.isEmpty() || isPaginatingNonInitial
                }
            }
        }
    }

    private fun handleGoogleBooksUiState(state: GoogleBooksUiState) {
        if (_binding == null || viewModel.currentMode.value != AppMode.GOOGLE_BOOKS) return

        val isLoading = state is GoogleBooksUiState.Loading
        val isError = state is GoogleBooksUiState.Error
        val isSuccess = state is GoogleBooksUiState.Success
        val books = if (isSuccess) (state as GoogleBooksUiState.Success).books else emptyList()

        val searchPerformedAndEmpty = isSuccess && books.isEmpty() &&
                (viewModel.googleSearchQueryAuthor.value.trim().isNotEmpty() ||
                        viewModel.googleSearchQueryTitle.value.trim().isNotEmpty())

        binding.shimmerLayout.isVisible = false
        binding.recyclerView.isVisible = false
        binding.emptyLayout.isVisible = false
        binding.errorLayout.isVisible = false
        binding.googleSearchLayout.isVisible = true
        binding.shimmerLayout.stopShimmer()

        when {
            isLoading -> {
                binding.shimmerLayout.isVisible = true
                binding.shimmerLayout.startShimmer()
                binding.googleSearchLayout.isVisible = false
            }
            isError -> {
                binding.errorLayout.isVisible = true
                binding.errorTextView.text = (state as GoogleBooksUiState.Error).message
            }
            isSuccess -> {
                if (books.isNotEmpty()) {
                    binding.recyclerView.isVisible = true
                    googleBooksAdapter.submitList(books)
                } else {
                    googleBooksAdapter.submitList(emptyList())
                    if (searchPerformedAndEmpty) {
                        binding.emptyLayout.isVisible = true
                        binding.emptyLayout.text = getString(R.string.google_books_no_results)
                        binding.emptyLayout.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_search_off, 0, 0)
                    }
                }
            }
            state is GoogleBooksUiState.Idle -> {
                googleBooksAdapter.submitList(emptyList())
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
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
}