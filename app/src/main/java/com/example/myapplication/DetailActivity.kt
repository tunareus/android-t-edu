package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ITEM = "library_item"
        const val EXTRA_EDITABLE = "editable"
        const val EXTRA_ITEM_TYPE = "item_type"
        const val TYPE_BOOK = "Book"
        const val TYPE_NEWSPAPER = "Newspaper"
        const val TYPE_DISK = "Disk"
    }

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var iconImageView: ImageView
    private lateinit var nameField: EditText
    private lateinit var pagesField: EditText
    private lateinit var authorField: EditText
    private lateinit var diskTypeField: EditText
    private lateinit var issueNumberField: EditText
    private lateinit var monthField: EditText
    private lateinit var availableEditText: EditText
    private lateinit var saveButton: Button

    private var editable: Boolean = false
    private var itemType: String = TYPE_BOOK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        rootLayout = findViewById(R.id.rootLayout)
        iconImageView = findViewById(R.id.iconImageView)
        nameField = findViewById(R.id.nameEditText)
        pagesField = findViewById(R.id.pagesEditText)
        authorField = findViewById(R.id.authorEditText)
        diskTypeField = findViewById(R.id.diskTypeEditText)
        issueNumberField = findViewById(R.id.issueNumberEditText)
        monthField = findViewById(R.id.monthEditText)
        availableEditText = findViewById(R.id.availableEditText)
        saveButton = findViewById(R.id.saveButton)

        editable = intent.getBooleanExtra(EXTRA_EDITABLE, false)
        itemType = intent.getStringExtra(EXTRA_ITEM_TYPE) ?: TYPE_BOOK

        when (itemType) {
            TYPE_BOOK -> iconImageView.setImageResource(R.drawable.ic_book)
            TYPE_DISK -> iconImageView.setImageResource(R.drawable.ic_disk)
            TYPE_NEWSPAPER -> iconImageView.setImageResource(R.drawable.ic_newspaper)
        }

        when (itemType) {
            TYPE_BOOK -> {
                pagesField.visibility = View.VISIBLE
                authorField.visibility = View.VISIBLE
                diskTypeField.visibility = View.GONE
                issueNumberField.visibility = View.GONE
                monthField.visibility = View.GONE
                pagesField.filters = arrayOf(object : InputFilter {
                    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
                        for (i in start until end) {
                            if (!source[i].isDigit()) return ""
                        }
                        return null
                    }
                })
            }
            TYPE_DISK -> {
                diskTypeField.visibility = View.VISIBLE
                pagesField.visibility = View.GONE
                authorField.visibility = View.GONE
                issueNumberField.visibility = View.GONE
                monthField.visibility = View.GONE
            }
            TYPE_NEWSPAPER -> {
                issueNumberField.visibility = View.VISIBLE
                monthField.visibility = View.VISIBLE
                pagesField.visibility = View.GONE
                authorField.visibility = View.GONE
                diskTypeField.visibility = View.GONE
                issueNumberField.filters = arrayOf(object : InputFilter {
                    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
                        for (i in start until end) {
                            if (!source[i].isDigit()) return ""
                        }
                        return null
                    }
                })
                monthField.filters = arrayOf(object : InputFilter {
                    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
                        for (i in start until end) {
                            if (!source[i].isLetter()) return ""
                        }
                        return null
                    }
                })
            }
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)
        when (itemType) {
            TYPE_BOOK -> {
                constraintSet.connect(R.id.availableEditText, ConstraintSet.TOP, R.id.authorEditText, ConstraintSet.BOTTOM, 8)
                constraintSet.connect(R.id.saveButton, ConstraintSet.TOP, R.id.availableEditText, ConstraintSet.BOTTOM, 24)
            }
            TYPE_DISK -> {
                constraintSet.connect(R.id.availableEditText, ConstraintSet.TOP, R.id.diskTypeEditText, ConstraintSet.BOTTOM, 8)
                constraintSet.connect(R.id.saveButton, ConstraintSet.TOP, R.id.availableEditText, ConstraintSet.BOTTOM, 24)
            }
            TYPE_NEWSPAPER -> {
                constraintSet.connect(R.id.availableEditText, ConstraintSet.TOP, R.id.monthEditText, ConstraintSet.BOTTOM, 8)
                constraintSet.connect(R.id.saveButton, ConstraintSet.TOP, R.id.availableEditText, ConstraintSet.BOTTOM, 24)
            }
        }
        constraintSet.applyTo(rootLayout)

        if (!editable) {
            @Suppress("DEPRECATION")
            val currentItem = intent.getSerializableExtra(EXTRA_ITEM)
            when (itemType) {
                TYPE_BOOK -> {
                    val book = currentItem as? Book
                    book?.let {
                        nameField.setText(it.name)
                        pagesField.setText(it.pages.toString())
                        authorField.setText(it.author)
                        availableEditText.setText(if (it.available) "Да" else "Нет")
                    }
                }
                TYPE_DISK -> {
                    val disk = currentItem as? Disk
                    disk?.let {
                        nameField.setText(it.name)
                        diskTypeField.setText(it.getDiskType())
                        availableEditText.setText(if (it.available) "Да" else "Нет")
                    }
                }
                TYPE_NEWSPAPER -> {
                    val newspaper = currentItem as? Newspaper
                    newspaper?.let {
                        nameField.setText(it.name)
                        issueNumberField.setText(it.issueNumber.toString())
                        monthField.setText(it.month.displayName)
                        availableEditText.setText(if (it.available) "Да" else "Нет")
                    }
                }
            }
            listOf(nameField, pagesField, authorField, diskTypeField, issueNumberField, monthField, availableEditText)
                .forEach { it.isEnabled = false }
            saveButton.isEnabled = false
        } else {
            availableEditText.isEnabled = true
            saveButton.setOnClickListener {
                val name = nameField.text.toString()
                val availableStr = availableEditText.text.toString().trim()

                if (!availableStr.equals("Да", ignoreCase = true) && !availableStr.equals("Нет", ignoreCase = true)) {
                    availableEditText.error = "Введите Да или Нет"
                    return@setOnClickListener
                }
                val available = availableStr.equals("Да", ignoreCase = true)

                when (itemType) {
                    TYPE_BOOK -> {
                        val pagesStr = pagesField.text.toString()
                        val pagesLong = pagesStr.toLongOrNull()
                        if (pagesLong == null || pagesLong > Int.MAX_VALUE) {
                            pagesField.error = "Неверное значение количества страниц"
                            return@setOnClickListener
                        }
                        val pages = pagesLong.toInt()
                        val author = authorField.text.toString()
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
                        val issueNumberStr = issueNumberField.text.toString()
                        val issueNumberLong = issueNumberStr.toLongOrNull()
                        if (issueNumberLong == null || issueNumberLong > Int.MAX_VALUE) {
                            issueNumberField.error = "Неверное значение номера выпуска"
                            return@setOnClickListener
                        }
                        val issueNumber = issueNumberLong.toInt()
                        val monthInput = monthField.text.toString().trim()
                        val selectedMonth = Month.entries.firstOrNull {
                            it.displayName.equals(monthInput, ignoreCase = true)
                        }
                        if (selectedMonth == null) {
                            monthField.error = "Неверное значение месяца"
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
                        val diskType = diskTypeField.text.toString()
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