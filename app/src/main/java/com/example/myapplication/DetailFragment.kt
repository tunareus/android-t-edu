package com.example.myapplication

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
import com.example.myapplication.databinding.ActivityDetailBinding
import kotlinx.coroutines.launch

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
            val item: LibraryItem? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_ITEM, LibraryItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_ITEM)
            }
            if (item != null) {
                setupItemData(item)
            }
        } ?: run {
            Toast.makeText(context, "Ошибка загрузки деталей", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupUI()
        setupSaveButton()
    }

    private fun setupItemData(item: LibraryItem) {
        binding.nameEditText.setText(item.name)
        binding.availableEditText.setText(if (item.available) "Да" else "Нет")

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
            val fallbackId = when {
                binding.nameEditText.isVisible -> binding.nameEditText.id
                else -> binding.iconImageView.id
            }
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

        binding.saveButton.setOnClickListener {
            var newItem: LibraryItem? = null
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
                binding.nameEditText.error = "Введите название"
                return@setOnClickListener
            }
            val available = when {
                availableStr.equals("Да", ignoreCase = true) -> true
                availableStr.equals("Нет", ignoreCase = true) -> false
                else -> {
                    binding.availableEditText.error = "Введите Да или Нет"
                    return@setOnClickListener
                }
            }

            try {
                when (itemType) {
                    TYPE_BOOK -> {
                        val pagesStr = binding.pagesEditText.text.toString()
                        val pages = pagesStr.toIntOrNull()
                        if (pages == null || pages <= 0) {
                            binding.pagesEditText.error = "Введите корректное число страниц (> 0)"
                            return@setOnClickListener
                        }
                        val author = binding.authorEditText.text.toString().trim()
                        if (author.isEmpty()) {
                            binding.authorEditText.error = "Введите автора"
                            return@setOnClickListener
                        }
                        newItem = Book(UniqueIdGenerator.getUniqueId(), available, name, pages, author)
                    }
                    TYPE_NEWSPAPER -> {
                        val issueNumberStr = binding.issueNumberEditText.text.toString()
                        val issueNumber = issueNumberStr.toIntOrNull()
                        if (issueNumber == null || issueNumber <= 0) {
                            binding.issueNumberEditText.error = "Введите корректный номер выпуска (> 0)"
                            return@setOnClickListener
                        }
                        val monthInput = binding.monthEditText.text.toString().trim()
                        val selectedMonth = Month.entries.firstOrNull { it.displayName.equals(monthInput, ignoreCase = true) }
                        if (selectedMonth == null) {
                            binding.monthEditText.error = "Неверный месяц (напр. 'Январь')"
                            return@setOnClickListener
                        }
                        newItem = Newspaper(UniqueIdGenerator.getUniqueId(), available, name, issueNumber, selectedMonth)
                    }
                    TYPE_DISK -> {
                        val diskType = binding.diskTypeEditText.text.toString().trim()
                        if (diskType.isEmpty()) {
                            binding.diskTypeEditText.error = "Введите тип диска"
                            return@setOnClickListener
                        }
                        newItem = Disk(UniqueIdGenerator.getUniqueId(), available, name, diskType)
                    }
                    else -> {
                        Toast.makeText(context, "Ошибка: Неизвестный тип элемента", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Ошибка ввода числовых данных", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } catch (e: Exception) {
                Toast.makeText(context, "Произошла ошибка при проверке данных", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            newItem?.let { itemToAdd ->
                binding.saveButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.addNewItem(itemToAdd)

                    viewModel.completeAddItem()

                    if (!isTwoPaneMode()) {
                        parentFragmentManager.popBackStack()
                    }
                }
            } ?: run {
                Toast.makeText(context, "Ошибка: Не удалось создать элемент", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
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