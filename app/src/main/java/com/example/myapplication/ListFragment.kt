package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LibraryViewModel
    private lateinit var adapter: LibraryItemAdapter
    private var pendingScrollPosition: Int? = null

    interface OnItemSelectedListener {
        fun onItemSelected(item: LibraryItem)
        fun onAddItemClicked()
    }

    private var listener: OnItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("ListFragment", "onAttach")
        if (context is OnItemSelectedListener) {
            listener = context
        } else {
            Log.w("ListFragment", "$context does not implement OnItemSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("ListFragment", "onDetach")
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ListFragment", "onCreateView")
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ListFragment", "onViewCreated")

        setupRecyclerView()
        setupViewModel()
        setupUIListeners()
    }

    private fun setupRecyclerView() {
        Log.d("ListFragment", "setupRecyclerView")
        adapter = LibraryItemAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        adapter.itemClickListener = { item ->
            Log.d("ListFragment", "Item clicked: ${item.name}")
            listener?.onItemSelected(item)
            viewModel.setSelectedItem(item)
        }

        val callback = SwipeToDeleteCallback { position ->
            Log.d("ListFragment", "Swipe detected at position: $position")
            viewModel.deleteItemAtPosition(position)
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
    }


    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]
        Log.d("ListFragment", "Setting up ViewModel observers")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                Log.d("ListFragment", "Received UI State: ${state::class.java.simpleName}")
                handleUiState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scrollToPosition.collectLatest { position ->
                Log.d("ListFragment", "Received scroll to position request: $position")
                pendingScrollPosition = position
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toastMessage.collectLatest { message ->
                Log.d("ListFragment", "Received toast message: $message")
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUIListeners() {
        Log.d("ListFragment", "setupUIListeners")
        binding.addButton.setOnClickListener {
            Log.d("ListFragment", "Add button clicked")
            listener?.onAddItemClicked()
        }
        binding.retryButton.setOnClickListener {
            Log.d("ListFragment", "Retry button clicked")
            viewModel.loadLibraryItems(forceRetry = true)
        }
    }

    private fun handleUiState(state: UiState) {
        Log.d("ListFragment", "Handling UI State: ${state::class.java.simpleName}")
        when (state) {
            is UiState.Idle -> {
                Log.d("ListFragment", "Handling Idle state (showing shimmer)")
                binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.isVisible = true
                binding.recyclerView.isVisible = false
                binding.errorLayout.isVisible = false
            }
            is UiState.Loading -> {
                Log.d("ListFragment", "Handling Loading state (showing shimmer)")
                if (!binding.shimmerLayout.isShimmerStarted) {
                    binding.shimmerLayout.startShimmer()
                }
                binding.shimmerLayout.isVisible = true
                binding.recyclerView.isVisible = false
                binding.errorLayout.isVisible = false
            }
            is UiState.Success -> {
                Log.d("ListFragment", "Handling Success: ${state.data.size} items")
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.isVisible = false
                binding.recyclerView.isVisible = true
                binding.errorLayout.isVisible = false
                adapter.submitList(state.data) {
                    Log.d("ListFragment", "submitList completed.")
                    pendingScrollPosition?.let { position ->
                        binding.recyclerView.post {
                            Log.d("ListFragment", "Executing pending scroll to position: $position")
                            if (position >= 0 && position < adapter.itemCount) {
                                binding.recyclerView.smoothScrollToPosition(position)
                            } else {
                                Log.w("ListFragment", "Pending scroll position $position is out of bounds (itemCount: ${adapter.itemCount})")
                            }
                            pendingScrollPosition = null
                        }
                    }
                }
            }
            is UiState.Error -> {
                Log.d("ListFragment", "Handling Error: ${state.exception.message}")
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.isVisible = false
                binding.recyclerView.isVisible = false
                binding.errorLayout.isVisible = true
                val errorMessage = state.exception.message ?: getString(R.string.unknown_error)
                binding.errorTextView.text = getString(R.string.error_loading_message, errorMessage)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ListFragment", "onDestroyView")
        binding.recyclerView.adapter = null
        _binding = null
    }
}