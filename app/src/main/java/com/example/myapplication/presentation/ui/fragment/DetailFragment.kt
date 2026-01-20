package com.example.myapplication.presentation.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.MyApplication
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityDetailBinding
import com.example.myapplication.domain.model.Book
import com.example.myapplication.domain.model.Disk
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.Month
import com.example.myapplication.domain.model.Newspaper
import com.example.myapplication.presentation.util.dpToPx
import com.example.myapplication.presentation.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class DetailFragment : Fragment() {

    companion object {
        const val TYPE_BOOK = "Book"
        const val TYPE_NEWSPAPER = "Newspaper"
        const val TYPE_DISK = "Disk"

        private const val ARG_EDITABLE = "editable"
        private const val ARG_ITEM_TYPE = "itemType"
        private const val ARG_ITEM = "item"

        fun newInstance(editable: Boolean, itemType: String, item: LibraryItem? = null): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_EDITABLE, editable)
                    putString(ARG_ITEM_TYPE, itemType)
                    item?.let { putParcelable(ARG_ITEM, it) }
                }
            }
        }
    }

    private var _binding: ActivityDetailBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var isViewEditable: Boolean = false
    private lateinit var currentViewItemType: String
    private lateinit var viewModel: LibraryViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[LibraryViewModel::class.java]

        arguments?.let { args ->
            isViewEditable = args.getBoolean(ARG_EDITABLE, false)
            val itemArg: LibraryItem? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable(ARG_ITEM, LibraryItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable(ARG_ITEM)
            }

            if (itemArg != null) {
                setupItemData(itemArg)
            } else {
                currentViewItemType = args.getString(ARG_ITEM_TYPE) ?: TYPE_BOOK
                if (!isViewEditable) {
                    Toast.makeText(context, R.string.error_loading_details, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return
                }
            }
        } ?: run {
            Toast.makeText(context, R.string.error_loading_details, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        if (!::currentViewItemType.isInitialized) {
            currentViewItemType = TYPE_BOOK
        }

        setupToolbarTitle()
        setupUI()
        setupSaveButton()
    }

    private fun setupToolbarTitle() {
        val args = arguments ?: return
        val isNewItemMode = args.getBoolean("editable", false)

        val finalTitle: String
        if (isNewItemMode) {
            val itemTypeForTitle = args.getString("itemType") ?: TYPE_BOOK
            val itemTypeDisplay = when (itemTypeForTitle) {
                TYPE_BOOK -> getString(R.string.item_type_book)
                TYPE_NEWSPAPER -> getString(R.string.item_type_newspaper)
                TYPE_DISK -> getString(R.string.item_type_disk)
                else -> ""
            }
            finalTitle = getString(R.string.add_item_title_prefix) + " " + itemTypeDisplay.lowercase()
        } else {
            val item: LibraryItem? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getParcelable(ARG_ITEM, LibraryItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getParcelable(ARG_ITEM)
            }
            finalTitle = item?.name ?: getString(R.string.details_fragment_title_default)
        }
        (activity as? AppCompatActivity)?.supportActionBar?.title = finalTitle
    }

    private fun setupItemData(item: LibraryItem) {
        binding.nameEditText.setText(item.name)
        binding.availableEditText.setText(if (item.available) getString(R.string.yes) else getString(R.string.no))

        when (item) {
            is Book -> {
                binding.pagesEditText.setText(item.pages.toString())
                binding.authorEditText.setText(item.author)
                currentViewItemType = TYPE_BOOK
            }
            is Disk -> {
                binding.diskTypeEditText.setText(item.getDiskType())
                currentViewItemType = TYPE_DISK
            }
            is Newspaper -> {
                binding.issueNumberEditText.setText(item.issueNumber.toString())
                binding.monthEditText.setText(item.month.displayName)
                currentViewItemType = TYPE_NEWSPAPER
            }
        }
    }

    private fun setupUI() {
        val iconResId = when (currentViewItemType) {
            TYPE_BOOK -> R.drawable.ic_book
            TYPE_DISK -> R.drawable.ic_disk
            TYPE_NEWSPAPER -> R.drawable.ic_newspaper
            else -> R.drawable.ic_item
        }
        binding.iconImageView.setImageResource(iconResId)

        binding.pagesEditText.isVisible = currentViewItemType == TYPE_BOOK
        binding.authorEditText.isVisible = currentViewItemType == TYPE_BOOK
        binding.diskTypeEditText.isVisible = currentViewItemType == TYPE_DISK
        binding.issueNumberEditText.isVisible = currentViewItemType == TYPE_NEWSPAPER
        binding.monthEditText.isVisible = currentViewItemType == TYPE_NEWSPAPER

        updateConstraints()

        listOf(
            binding.nameEditText, binding.pagesEditText, binding.authorEditText,
            binding.diskTypeEditText, binding.issueNumberEditText, binding.monthEditText,
            binding.availableEditText
        ).forEach { it.isEnabled = isViewEditable }

        binding.saveButton.isVisible = isViewEditable

        binding.availableEditText.hint = if (isViewEditable) {
            getString(R.string.available_hint_editable)
        } else {
            getString(R.string.availability_status_hint)
        }
    }

    private fun updateConstraints() {
        val constraintSet = ConstraintSet()
        val rootLayout = binding.root as? ConstraintLayout ?: return
        constraintSet.clone(rootLayout)

        val topAnchorIdForAvailable = when (currentViewItemType) {
            TYPE_BOOK -> binding.authorEditText.id
            TYPE_DISK -> binding.diskTypeEditText.id
            TYPE_NEWSPAPER -> binding.monthEditText.id
            else -> binding.nameEditText.id
        }

        constraintSet.connect(
            binding.availableEditText.id, ConstraintSet.TOP,
            topAnchorIdForAvailable, ConstraintSet.BOTTOM,
            requireContext().dpToPx(8)
        )

        constraintSet.connect(
            binding.saveButton.id, ConstraintSet.TOP,
            binding.availableEditText.id, ConstraintSet.BOTTOM,
            requireContext().dpToPx(24)
        )
        constraintSet.applyTo(rootLayout)
    }


    private fun setupSaveButton() {
        if (!isViewEditable) {
            binding.saveButton.isVisible = false
            return
        }
        binding.saveButton.isVisible = true

        binding.saveButton.setOnClickListener {
            listOf(
                binding.nameEditText, binding.availableEditText, binding.pagesEditText,
                binding.authorEditText, binding.diskTypeEditText, binding.issueNumberEditText,
                binding.monthEditText
            ).forEach { it.error = null }

            val name = binding.nameEditText.text.toString().trim()
            if (name.isEmpty()) {
                binding.nameEditText.error = getString(R.string.error_validation_name_empty)
                return@setOnClickListener
            }

            val availableStr = binding.availableEditText.text.toString().trim()
            val available = when {
                availableStr.equals(getString(R.string.yes), ignoreCase = true) -> true
                availableStr.equals(getString(R.string.no), ignoreCase = true) -> false
                else -> {
                    binding.availableEditText.error = getString(R.string.error_validation_available_format)
                    return@setOnClickListener
                }
            }

            val newItem: LibraryItem = try {
                when (currentViewItemType) {
                    TYPE_BOOK -> {
                        val pagesStr = binding.pagesEditText.text.toString()
                        val pages = pagesStr.toIntOrNull()
                        if (pages == null || pages <= 0) {
                            binding.pagesEditText.error = getString(R.string.error_validation_pages_invalid)
                            throw ValidationException()
                        }
                        val author = binding.authorEditText.text.toString().trim()
                        if (author.isEmpty()) {
                            binding.authorEditText.error = getString(R.string.error_validation_author_empty)
                            throw ValidationException()
                        }
                        Book(0, available, name, pages, author)
                    }
                    TYPE_NEWSPAPER -> {
                        val issueNumberStr = binding.issueNumberEditText.text.toString()
                        val issueNumber = issueNumberStr.toIntOrNull()
                        if (issueNumber == null || issueNumber <= 0) {
                            binding.issueNumberEditText.error = getString(R.string.error_validation_issue_invalid)
                            throw ValidationException()
                        }
                        val monthInput = binding.monthEditText.text.toString().trim()
                        val selectedMonth = Month.entries.firstOrNull { it.displayName.equals(monthInput, ignoreCase = true) }
                        if (selectedMonth == null) {
                            binding.monthEditText.error = getString(R.string.error_validation_month_invalid)
                            throw ValidationException()
                        }
                        Newspaper(0, available, name, issueNumber, selectedMonth)
                    }
                    TYPE_DISK -> {
                        val diskType = binding.diskTypeEditText.text.toString().trim()
                        if (diskType.isEmpty()) {
                            binding.diskTypeEditText.error = getString(R.string.error_validation_disktype_empty)
                            throw ValidationException()
                        }
                        Disk(0, available, name, diskType)
                    }
                    else -> {
                        Toast.makeText(context, R.string.error_unknown_item_type, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            } catch (e: ValidationException) {
                return@setOnClickListener
            } catch (e: NumberFormatException) {
                Toast.makeText(context, R.string.error_validation_number_format, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.error_item_creation_failed) + ": " + e.localizedMessage, Toast.LENGTH_LONG).show()
                binding.saveButton.isEnabled = true
                return@setOnClickListener
            }

            binding.saveButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.addManuallyCreatedItem(newItem)
                viewModel.completeAddItem()

                if (!isTwoPaneMode()) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private class ValidationException : Exception()

    private fun isTwoPaneMode(): Boolean {
        return requireActivity().findViewById<View>(R.id.detailContainer) != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}