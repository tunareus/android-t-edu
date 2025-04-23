package com.example.myapplication

import android.content.Context
import android.os.Bundle
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
        if (context is OnItemSelectedListener) {
            listener = context
        } else {
            //
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupViewModel()
        setupUIListeners()
    }

    private fun setupRecyclerView() {
        adapter = LibraryItemAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        adapter.itemClickListener = { item ->
            listener?.onItemSelected(item)
        }

        val callback = SwipeToDeleteCallback { position ->
            viewModel.deleteItemAtPosition(position)
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
    }


    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                handleUiState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scrollToPosition.collectLatest { position ->
                pendingScrollPosition = position
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.toastMessage.collect { message ->
                context?.let { ctx ->
                    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUIListeners() {
        binding.addButton.setOnClickListener {
            listener?.onAddItemClicked()
        }
        binding.retryButton.setOnClickListener {
            viewModel.loadLibraryItems()
        }
    }

    private fun handleUiState(state: UiState) {
        when (state) {
            is UiState.Loading -> {
                if (!binding.shimmerLayout.isShimmerStarted) binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.isVisible = true
                binding.recyclerView.isVisible = false
                binding.errorLayout.isVisible = false
            }
            is UiState.Success -> {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.isVisible = false
                binding.recyclerView.isVisible = true
                binding.errorLayout.isVisible = false
                adapter.submitList(state.data) {
                    pendingScrollPosition?.let { position ->
                        binding.recyclerView.post {
                            if (position >= 0 && position < adapter.itemCount) {
                                binding.recyclerView.smoothScrollToPosition(position)
                            } else {
                                //
                            }
                            pendingScrollPosition = null
                        }
                    }
                }
            }
            is UiState.Error -> {
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
        binding.recyclerView.adapter = null
        _binding = null
    }
}