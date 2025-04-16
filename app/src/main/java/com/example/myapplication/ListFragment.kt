package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        adapter = LibraryItemAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        adapter.itemClickListener = { item ->
            listener?.onItemSelected(item)
        }

        binding.addButton.setOnClickListener {
            listener?.onAddItemClicked()
        }

        val callback = SwipeToDeleteCallback { position ->
            viewModel.removeItem(position)
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)

        setupViewModel()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]

        viewModel.libraryItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items) {
                pendingScrollPosition?.let { position ->
                    binding.recyclerView.post {
                        if (position < adapter.itemCount) {
                            binding.recyclerView.smoothScrollToPosition(position)
                        }
                        pendingScrollPosition = null
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.scrollToPosition.collectLatest { position ->
                pendingScrollPosition = position
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}