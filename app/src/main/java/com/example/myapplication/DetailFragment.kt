package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.databinding.ActivityDetailBinding
import kotlinx.coroutines.launch
import com.example.myapplication.util.dpToPx

class DetailFragment : Fragment() {

    companion object {
        const val TYPE_BOOK = "Book"
        const val TYPE_NEWSPAPER = "Newspaper"
        const val TYPE_DISK = "Disk"

        private const val ARG_EDITABLE = "editable"
        private const val ARG_ITEM_TYPE = "item_type"
        private const val ARG_ITEM = "item"

        fun newInstance(editable: Boolean, itemType: String, item: LibraryItem? = null): DetailFragment {
            val fragment = DetailFragment()
            val args = Bundle().apply {
                putBoolean(ARG_EDITABLE, editable)
                putString(ARG_ITEM_TYPE, itemType)
                item?.let { putParcelable(ARG_ITEM, it) }
            }
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: ActivityDetailBinding? = null
    private val binding get() = _binding!!

    private var editable: Boolean = false
    private var itemType: String = TYPE_BOOK
    private lateinit var viewModel: LibraryViewModel
    private lateinit var settingsRepository: SettingsRepository

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingsRepository = SettingsRepository(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext().applicationContext, lifecycleScope)
        val repository = LibraryRepository(database.libraryItemDao())
        val factory = LibraryViewModel.LibraryViewModelFactory(repository, settingsRepository)
        viewModel = ViewModelProvider(requireActivity(), factory)[LibraryViewModel::class.java]

        arguments?.let {
            editable = it.getBoolean(ARG_EDITABLE, false)
            itemType = it.getString(ARG_ITEM_TYPE, TYPE_BOOK) ?: TYPE_BOOK
            val item: LibraryItem? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_ITEM, LibraryItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_ITEM)
            }
            if (!editable && item != null) {
                setupItemData(item)
            } else if (editable && item != null) {
                setupItemData(item)
            }
        } ?: if (editable) {
        } else {
            Toast.makeText(context, R.string.error_loading_details, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        setupUI()
        setupSaveButton()
    }

    private fun setupItemData(item: LibraryItem) {
        binding.nameEditText.setText(item.name)
        binding.availableEditText.setText(if (item.available) R.string.yes else R.string.no)

        when (item) {
            is Book -> {
                binding.pagesEditText.setText(item.pages.toString())
                binding.authorEditText.setText(item.author)
            }
            is Disk -> {
                binding.diskTypeEditText.setText(item.getDiskType())
            }
            is Newspaper -> {
                binding.issueNumberEditText.setText(item.issueNumber.toString())
                binding.monthEditText.setText(item.month.displayName)
            }
        }
    }

    private fun setupUI() {
        val iconResId = when (itemType) {
            TYPE_BOOK -> R.drawable.ic_book
            TYPE_DISK -> R.drawable.ic_disk
            TYPE_NEWSPAPER -> R.drawable.ic_newspaper
            else -> R.drawable.ic_item
        }
        binding.iconImageView.setImageResource(iconResId)

        binding.pagesEditText.isVisible = itemType == TYPE_BOOK
        binding.authorEditText.isVisible = itemType == TYPE_BOOK
        binding.diskTypeEditText.isVisible = itemType == TYPE_DISK
        binding.issueNumberEditText.isVisible = itemType == TYPE_NEWSPAPER
        binding.monthEditText.isVisible = itemType == TYPE_NEWSPAPER

        updateConstraints()

        val isEnabled = editable
        listOf(
            binding.nameEditText, binding.pagesEditText, binding.authorEditText,
            binding.diskTypeEditText, binding.issueNumberEditText, binding.monthEditText,
            binding.availableEditText
        ).forEach { it.isEnabled = isEnabled }

        binding.saveButton.isVisible = editable

        if (!editable) {
            binding.availableEditText.hint = getString(R.string.availability_status_hint)
        } else {
            binding.availableEditText.hint = getString(R.string.available_hint_editable)
        }
    }

    private fun updateConstraints() {
        val constraintSet = ConstraintSet()
        val rootLayout = binding.root as? ConstraintLayout ?: return
        constraintSet.clone(rootLayout)

        val bottomFieldId = when (itemType) {
            TYPE_BOOK -> binding.authorEditText.id
            TYPE_DISK -> binding.diskTypeEditText.id
            TYPE_NEWSPAPER -> binding.monthEditText.id
            else -> binding.nameEditText.id
        }

        val bottomFieldView = rootLayout.findViewById<View>(bottomFieldId)
        if (bottomFieldView != null && bottomFieldView.isVisible) {
            constraintSet.connect(
                binding.availableEditText.id, ConstraintSet.TOP,
                bottomFieldId, ConstraintSet.BOTTOM,
                requireContext().dpToPx(4)
            )
        } else {
            val fallbackId = binding.nameEditText.id
            constraintSet.connect(
                binding.availableEditText.id, ConstraintSet.TOP,
                fallbackId, ConstraintSet.BOTTOM,
                requireContext().dpToPx(4)
            )
        }

        constraintSet.connect(
            binding.saveButton.id, ConstraintSet.TOP,
            binding.availableEditText.id, ConstraintSet.BOTTOM,
            requireContext().dpToPx(24)
        )

        constraintSet.applyTo(rootLayout)
    }

    private fun setupSaveButton() {
        if (!editable) {
            binding.saveButton.isVisible = false
            return
        }
        binding.saveButton.isVisible = true

        binding.saveButton.setOnClickListener {
            binding.nameEditText.error = null
            binding.availableEditText.error = null
            binding.pagesEditText.error = null
            binding.authorEditText.error = null
            binding.diskTypeEditText.error = null
            binding.issueNumberEditText.error = null
            binding.monthEditText.error = null

            val name = binding.nameEditText.text.toString().trim()
            val availableStr = binding.availableEditText.text.toString().trim()

            if (name.isEmpty()) {
                binding.nameEditText.error = getString(R.string.error_validation_name_empty)
                return@setOnClickListener
            }

            val available = when {
                availableStr.equals(getString(R.string.yes), ignoreCase = true) -> true
                availableStr.equals(getString(R.string.no), ignoreCase = true) -> false
                else -> {
                    binding.availableEditText.error = getString(R.string.error_validation_available_format)
                    return@setOnClickListener
                }
            }

            val newItem: LibraryItem

            try {
                newItem = when (itemType) {
                    TYPE_BOOK -> {
                        val pagesStr = binding.pagesEditText.text.toString()
                        val pages = pagesStr.toIntOrNull()
                        if (pages == null || pages <= 0) {
                            binding.pagesEditText.error = getString(R.string.error_validation_pages_invalid)
                            return@setOnClickListener
                        }
                        val author = binding.authorEditText.text.toString().trim()
                        if (author.isEmpty()) {
                            binding.authorEditText.error = getString(R.string.error_validation_author_empty)
                            return@setOnClickListener
                        }
                        Book(0, available, name, pages, author)
                    }
                    TYPE_NEWSPAPER -> {
                        val issueNumberStr = binding.issueNumberEditText.text.toString()
                        val issueNumber = issueNumberStr.toIntOrNull()
                        if (issueNumber == null || issueNumber <= 0) {
                            binding.issueNumberEditText.error = getString(R.string.error_validation_issue_invalid)
                            return@setOnClickListener
                        }
                        val monthInput = binding.monthEditText.text.toString().trim()
                        val selectedMonth = Month.entries.firstOrNull { it.displayName.equals(monthInput, ignoreCase = true) }
                        if (selectedMonth == null) {
                            binding.monthEditText.error = getString(R.string.error_validation_month_invalid)
                            return@setOnClickListener
                        }
                        Newspaper(0, available, name, issueNumber, selectedMonth)
                    }
                    TYPE_DISK -> {
                        val diskType = binding.diskTypeEditText.text.toString().trim()
                        if (diskType.isEmpty()) {
                            binding.diskTypeEditText.error = getString(R.string.error_validation_disktype_empty)
                            return@setOnClickListener
                        }
                        Disk(0, available, name, diskType)
                    }
                    else -> {
                        Toast.makeText(context, R.string.error_unknown_item_type, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(context, R.string.error_validation_number_format, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } catch (e: Exception) {
                Toast.makeText(context, R.string.error_validation_generic, Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
                return@setOnClickListener
            }

            binding.saveButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.addNewItem(newItem)
                viewModel.completeAddItem()

                if (!isTwoPaneMode()) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun isTwoPaneMode(): Boolean {
        return requireActivity().findViewById<View>(R.id.detailContainer) != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}