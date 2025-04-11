package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import com.example.myapplication.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ITEM = "library_item"
        const val EXTRA_EDITABLE = "editable"
        const val EXTRA_ITEM_TYPE = "item_type"
        const val TYPE_BOOK = "Book"
        const val TYPE_NEWSPAPER = "Newspaper"
        const val TYPE_DISK = "Disk"

        fun newIntent(
            context: Context,
            editable: Boolean,
            itemType: String,
            item: LibraryItem? = null
        ): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_EDITABLE, editable)
                putExtra(EXTRA_ITEM_TYPE, itemType)
                if (item != null) {
                    putExtra(EXTRA_ITEM, item)
                }
            }
        }
    }

    private lateinit var binding: ActivityDetailBinding
    private var editable: Boolean = false
    private var itemType: String = TYPE_BOOK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editable = intent.getBooleanExtra(EXTRA_EDITABLE, false)
        itemType = intent.getStringExtra(EXTRA_ITEM_TYPE) ?: TYPE_BOOK

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
                binding.pagesEditText.filters = arrayOf(object : InputFilter {
                    override fun filter(
                        source: CharSequence?, start: Int, end: Int,
                        dest: Spanned?, dstart: Int, dend: Int
                    ): CharSequence? = null
                })
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
                binding.issueNumberEditText.filters = arrayOf(object : InputFilter {
                    override fun filter(
                        source: CharSequence?, start: Int, end: Int,
                        dest: Spanned?, dstart: Int, dend: Int
                    ): CharSequence? = null
                })
                binding.monthEditText.filters = arrayOf(object : InputFilter {
                    override fun filter(
                        source: CharSequence?, start: Int, end: Int,
                        dest: Spanned?, dstart: Int, dend: Int
                    ): CharSequence? = null
                })
            }
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)
        when (itemType) {
            TYPE_BOOK -> {
                constraintSet.connect(
                    binding.availableEditText.id, ConstraintSet.TOP,
                    binding.authorEditText.id, ConstraintSet.BOTTOM, dpToPx(4)
                )
                constraintSet.connect(
                    binding.saveButton.id, ConstraintSet.TOP,
                    binding.availableEditText.id, ConstraintSet.BOTTOM, dpToPx(24)
                )
            }
            TYPE_DISK -> {
                constraintSet.connect(
                    binding.availableEditText.id, ConstraintSet.TOP,
                    binding.diskTypeEditText.id, ConstraintSet.BOTTOM, dpToPx(4)
                )
                constraintSet.connect(
                    binding.saveButton.id, ConstraintSet.TOP,
                    binding.availableEditText.id, ConstraintSet.BOTTOM, dpToPx(24)
                )
            }
            TYPE_NEWSPAPER -> {
                constraintSet.connect(
                    binding.availableEditText.id, ConstraintSet.TOP,
                    binding.monthEditText.id, ConstraintSet.BOTTOM, dpToPx(4)
                )
                constraintSet.connect(
                    binding.saveButton.id, ConstraintSet.TOP,
                    binding.availableEditText.id, ConstraintSet.BOTTOM, dpToPx(24)
                )
            }
        }
        constraintSet.applyTo(binding.root)

        if (!editable) {
            val currentItem = intent.getParcelableExtraCompat<LibraryItem>(EXTRA_ITEM)
            when (itemType) {
                TYPE_BOOK -> {
                    val book = currentItem as? Book
                    book?.let {
                        binding.nameEditText.setText(it.name)
                        binding.pagesEditText.setText(it.pages.toString())
                        binding.authorEditText.setText(it.author)
                        binding.availableEditText.setText(if (it.available) "Да" else "Нет")
                    }
                }
                TYPE_DISK -> {
                    val disk = currentItem as? Disk
                    disk?.let {
                        binding.nameEditText.setText(it.name)
                        binding.diskTypeEditText.setText(it.getDiskType())
                        binding.availableEditText.setText(if (it.available) "Да" else "Нет")
                    }
                }
                TYPE_NEWSPAPER -> {
                    val newspaper = currentItem as? Newspaper
                    newspaper?.let {
                        binding.nameEditText.setText(it.name)
                        binding.issueNumberEditText.setText(it.issueNumber.toString())
                        binding.monthEditText.setText(it.month.displayName)
                        binding.availableEditText.setText(if (it.available) "Да" else "Нет")
                    }
                }
            }
            listOf(binding.nameEditText, binding.pagesEditText, binding.authorEditText,
                binding.diskTypeEditText, binding.issueNumberEditText, binding.monthEditText,
                binding.availableEditText).forEach { it.isEnabled = false }
            binding.saveButton.isEnabled = false
        } else {
            binding.availableEditText.isEnabled = true
            binding.saveButton.setOnClickListener {
                val name = binding.nameEditText.text.toString()
                val availableStr = binding.availableEditText.text.toString().trim()

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
                        val newBook = Book(
                            id = UniqueIdGenerator.getUniqueId(),
                            available = available,
                            name = name,
                            pages = pages,
                            author = author
                        )
                        sendResult(newBook)
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
                        val newNewspaper = Newspaper(
                            id = UniqueIdGenerator.getUniqueId(),
                            available = available,
                            name = name,
                            issueNumber = issueNumber,
                            month = selectedMonth
                        )
                        sendResult(newNewspaper)
                    }
                    TYPE_DISK -> {
                        val diskType = binding.diskTypeEditText.text.toString()
                        val newDisk = Disk(
                            id = UniqueIdGenerator.getUniqueId(),
                            available = available,
                            name = name,
                            diskType = diskType
                        )
                        sendResult(newDisk)
                    }
                }
            }
        }
    }

    private fun sendResult(item: LibraryItem) {
        val resultIntent = intent
        resultIntent.putExtra(EXTRA_ITEM, item)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}