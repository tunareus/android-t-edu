package com.example.myapplication

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.ActivityDetailBinding

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
                if (item != null) putParcelable(ARG_ITEM, item)
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

        viewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]

        arguments?.let {
            editable = it.getBoolean(ARG_EDITABLE, false)
            itemType = it.getString(ARG_ITEM_TYPE, TYPE_BOOK) ?: TYPE_BOOK
            val item = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_ITEM, LibraryItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_ITEM)
            }
            if (item != null) {
                setupItemData(item)
            }
        }

        setupUI()
        setupSaveButton()
    }

    private fun setupItemData(item: LibraryItem) {
        when (itemType) {
            TYPE_BOOK -> {
                val book = item as? Book
                book?.let {
                    binding.nameEditText.setText(it.name)
                    binding.pagesEditText.setText(it.pages.toString())
                    binding.authorEditText.setText(it.author)
                    binding.availableEditText.setText(if (it.available) "Да" else "Нет")
                }
            }
            TYPE_DISK -> {
                val disk = item as? Disk
                disk?.let {
                    binding.nameEditText.setText(it.name)
                    binding.diskTypeEditText.setText(it.getDiskType())
                    binding.availableEditText.setText(if (it.available) "Да" else "Нет")
                }
            }
            TYPE_NEWSPAPER -> {
                val newspaper = item as? Newspaper
                newspaper?.let {
                    binding.nameEditText.setText(it.name)
                    binding.issueNumberEditText.setText(it.issueNumber.toString())
                    binding.monthEditText.setText(it.month.displayName)
                    binding.availableEditText.setText(if (it.available) "Да" else "Нет")
                }
            }
        }
    }

    private fun setupUI() {
        when (itemType) {
            TYPE_BOOK -> binding.iconImageView.setImageResource(R.drawable.ic_book)
            TYPE_DISK -> binding.iconImageView.setImageResource(R.drawable.ic_disk)
            TYPE_NEWSPAPER -> binding.iconImageView.setImageResource(R.drawable.ic_newspaper)
        }

        when (itemType) {
            TYPE_BOOK -> {
                binding.pagesEditText.isVisible = true
                binding.authorEditText.isVisible = true
                binding.diskTypeEditText.isVisible = false
                binding.issueNumberEditText.isVisible = false
                binding.monthEditText.isVisible = false

                binding.pagesEditText.filters = arrayOf(NumberInputFilter())
            }
            TYPE_DISK -> {
                binding.diskTypeEditText.isVisible = true
                binding.pagesEditText.isVisible = false
                binding.authorEditText.isVisible = false
                binding.issueNumberEditText.isVisible = false
                binding.monthEditText.isVisible = false
            }
            TYPE_NEWSPAPER -> {
                binding.issueNumberEditText.isVisible = true
                binding.monthEditText.isVisible = true
                binding.pagesEditText.isVisible = false
                binding.authorEditText.isVisible = false
                binding.diskTypeEditText.isVisible = false

                binding.issueNumberEditText.filters = arrayOf(NumberInputFilter())
            }
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.rootLayout)
        when (itemType) {
            TYPE_BOOK -> {
                constraintSet.connect(
                    binding.availableEditText.id, ConstraintSet.TOP,
                    binding.authorEditText.id, ConstraintSet.BOTTOM,
                    requireContext().dpToPx(4)
                )
                constraintSet.connect(
                    binding.saveButton.id, ConstraintSet.TOP,
                    binding.availableEditText.id, ConstraintSet.BOTTOM,
                    requireContext().dpToPx(24)
                )
            }
            TYPE_DISK -> {
                constraintSet.connect(
                    binding.availableEditText.id, ConstraintSet.TOP,
                    binding.diskTypeEditText.id, ConstraintSet.BOTTOM,
                    requireContext().dpToPx(4)
                )
                constraintSet.connect(
                    binding.saveButton.id, ConstraintSet.TOP,
                    binding.availableEditText.id, ConstraintSet.BOTTOM,
                    requireContext().dpToPx(24)
                )
            }
            TYPE_NEWSPAPER -> {
                constraintSet.connect(
                    binding.availableEditText.id, ConstraintSet.TOP,
                    binding.monthEditText.id, ConstraintSet.BOTTOM,
                    requireContext().dpToPx(4)
                )
                constraintSet.connect(
                    binding.saveButton.id, ConstraintSet.TOP,
                    binding.availableEditText.id, ConstraintSet.BOTTOM,
                    requireContext().dpToPx(24)
                )
            }
        }
        constraintSet.applyTo(binding.rootLayout)

        if (!editable) {
            listOf(
                binding.nameEditText,
                binding.pagesEditText,
                binding.authorEditText,
                binding.diskTypeEditText,
                binding.issueNumberEditText,
                binding.monthEditText,
                binding.availableEditText
            ).forEach { it.isEnabled = false }
            binding.saveButton.isVisible = false
        }
    }

    private fun setupSaveButton() {
        if (editable) {
            binding.saveButton.setOnClickListener {
                val name = binding.nameEditText.text.toString()
                val availableStr = binding.availableEditText.text.toString().trim()

                if (name.isEmpty()) {
                    binding.nameEditText.error = "Введите название"
                    return@setOnClickListener
                }

                if (!availableStr.equals("Да", ignoreCase = true) &&
                    !availableStr.equals("Нет", ignoreCase = true)
                ) {
                    binding.availableEditText.error = "Введите Да или Нет"
                    return@setOnClickListener
                }
                val available = availableStr.equals("Да", ignoreCase = true)

                when (itemType) {
                    TYPE_BOOK -> {
                        val pagesStr = binding.pagesEditText.text.toString()
                        val pagesLong = pagesStr.toLongOrNull()
                        if (pagesLong == null || pagesLong > Int.MAX_VALUE) {
                            binding.pagesEditText.error = "Неверное значение количества страниц"
                            return@setOnClickListener
                        }
                        val pages = pagesLong.toInt()
                        val author = binding.authorEditText.text.toString()
                        if (author.isEmpty()) {
                            binding.authorEditText.error = "Введите автора"
                            return@setOnClickListener
                        }
                        val newItem = Book(
                            id = UniqueIdGenerator.getUniqueId(),
                            available = available,
                            name = name,
                            pages = pages,
                            author = author
                        )
                        viewModel.onItemCreated(newItem)
                        sendResultFragment(newItem)
                    }
                    TYPE_NEWSPAPER -> {
                        val issueNumberStr = binding.issueNumberEditText.text.toString()
                        val issueNumberLong = issueNumberStr.toLongOrNull()
                        if (issueNumberLong == null || issueNumberLong > Int.MAX_VALUE) {
                            binding.issueNumberEditText.error = "Неверное значение номера выпуска"
                            return@setOnClickListener
                        }
                        val issueNumber = issueNumberLong.toInt()
                        val monthInput = binding.monthEditText.text.toString().trim()
                        val selectedMonth = Month.entries.firstOrNull {
                            it.displayName.equals(monthInput, ignoreCase = true)
                        }
                        if (selectedMonth == null) {
                            binding.monthEditText.error = "Неверное значение месяца"
                            return@setOnClickListener
                        }
                        val newItem = Newspaper(
                            id = UniqueIdGenerator.getUniqueId(),
                            available = available,
                            name = name,
                            issueNumber = issueNumber,
                            month = selectedMonth
                        )
                        viewModel.onItemCreated(newItem)
                        sendResultFragment(newItem)
                    }
                    TYPE_DISK -> {
                        val diskType = binding.diskTypeEditText.text.toString()
                        if (diskType.isEmpty()) {
                            binding.diskTypeEditText.error = "Введите тип диска"
                            return@setOnClickListener
                        }
                        val newItem = Disk(
                            id = UniqueIdGenerator.getUniqueId(),
                            available = available,
                            name = name,
                            diskType = diskType
                        )
                        viewModel.onItemCreated(newItem)
                        sendResultFragment(newItem)
                    }
                }
            }
        }
    }

    private fun sendResultFragment(item: LibraryItem) {
        val bundle = Bundle().apply { putParcelable("item", item) }
        parentFragmentManager.setFragmentResult("new_item", bundle)
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

    private class NumberInputFilter : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? = null
    }