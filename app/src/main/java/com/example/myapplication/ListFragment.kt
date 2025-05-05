package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.SortField
import com.example.myapplication.data.settings.SortOrder
import com.example.myapplication.data.settings.SortPreference
import com.example.myapplication.databinding.FragmentListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LibraryViewModel
    private lateinit var adapter: LibraryItemAdapter
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupSortSpinner()
        setupRecyclerView()
        setupUIListeners()
        observeViewModel()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext().applicationContext, viewLifecycleOwner.lifecycleScope)
        val repository = LibraryRepository(database.libraryItemDao())
        val factory = LibraryViewModel.LibraryViewModelFactory(repository, settingsRepository)
        viewModel = ViewModelProvider(requireActivity(), factory)[LibraryViewModel::class.java]
    }

    private fun setupSortSpinner() {
        val sortOptions = listOf(
            getString(R.string.sort_date_desc),
            getString(R.string.sort_date_asc),
            getString(R.string.sort_name_asc),
            getString(R.string.sort_name_desc)
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
                if (selectedPref != currentPref) {
                    viewModel.setSortPreference(selectedPref)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
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

    private fun setupRecyclerView() {
        adapter = LibraryItemAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)

        adapter.itemClickListener = { item ->
            viewModel.setSelectedItem(item)
            listener?.onItemSelected(item)
        }

        val callback = SwipeToDeleteCallback { position ->
            val itemToDelete = adapter.currentList.getOrNull(position) as? AdapterItem.Data
            itemToDelete?.let {
                viewModel.deleteItem(it.libraryItem)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy == 0) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCountInAdapter = adapter.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                if (dy > 0 && totalItemCountInAdapter > 0 && lastVisible >= totalItemCountInAdapter - LibraryViewModel.PREFETCH_DISTANCE) {
                    viewModel.loadMoreItemsBottom()
                }

                if (dy < 0 && totalItemCountInAdapter > 0 && firstVisible <= LibraryViewModel.PREFETCH_DISTANCE) {
                    viewModel.loadMoreItemsTop()
                }
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                handleUiState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.displayedItems,
                viewModel.paginationState
            ) { items, pagState ->
                mutableListOf<AdapterItem>().apply {
                    if (pagState is PaginationState.LoadingBefore) add(AdapterItem.LoadingTop)
                    addAll(items.map { AdapterItem.Data(it) })
                    if (pagState is PaginationState.LoadingAfter) add(AdapterItem.LoadingBottom)
                }
            }.collectLatest { adapterItems ->
                if (_binding != null) {
                    adapter.submitList(adapterItems) {
                        handleEmptyState(adapterItems.none { it is AdapterItem.Data } && viewModel.uiState.value is UiState.Success)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortPreference.collectLatest { preference ->
                if (_binding != null) {
                    updateSpinnerSelection(preference)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toastMessage.collect { message ->
                context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun setupUIListeners() {
        binding.addButton.setOnClickListener {
            listener?.onAddItemClicked()
        }
        binding.retryButton.setOnClickListener {
            viewModel.loadInitialItems()
        }
    }

    private fun handleUiState(state: UiState) {
        if (_binding == null) return

        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.isVisible = false
        binding.recyclerView.isVisible = false
        binding.errorLayout.isVisible = false
        binding.emptyLayout.isVisible = false

        when (state) {
            is UiState.InitialLoading -> {
                binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.isVisible = true
            }
            is UiState.Success -> {
                binding.recyclerView.isVisible = true
            }
            is UiState.Error -> {
                binding.errorLayout.isVisible = true
                val errorMessage = state.exception.localizedMessage ?: state.exception.toString()
                binding.errorTextView.text = getString(R.string.error_loading_message, errorMessage)
            }
            is UiState.Loading -> { }
        }
    }

    private fun handleEmptyState(isEmpty: Boolean) {
        if (_binding == null) return

        val isSuccessState = viewModel.uiState.value is UiState.Success
        val isLoading = viewModel.uiState.value is UiState.InitialLoading || viewModel.paginationState.value != PaginationState.Idle
        val isError = viewModel.uiState.value is UiState.Error

        if (isEmpty && isSuccessState && !isLoading && !isError) {
            binding.emptyLayout.isVisible = true
            binding.recyclerView.isVisible = false
        } else {
            binding.emptyLayout.isVisible = false
            if (!isLoading && !isError && !isEmpty) {
                binding.recyclerView.isVisible = true
            } else if (isLoading || isError) {
                binding.recyclerView.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}