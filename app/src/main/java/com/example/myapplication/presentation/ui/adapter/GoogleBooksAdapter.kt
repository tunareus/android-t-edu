package com.example.myapplication.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemLibraryItemBinding
import com.example.myapplication.domain.model.GoogleBookDetails

class GoogleBooksAdapter(
    private val onLongClickListener: (GoogleBookDetails) -> Unit
) : ListAdapter<GoogleBookDetails, GoogleBooksAdapter.ViewHolder>(GoogleBookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLibraryItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onLongClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemLibraryItemBinding,
        private val onLongClickListener: (GoogleBookDetails) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: GoogleBookDetails? = null

        init {
            binding.cardView.setOnLongClickListener {
                currentItem?.let { item ->
                    onLongClickListener(item)
                    true
                } ?: false
            }
        }

        fun bind(item: GoogleBookDetails) = with(binding) {
            currentItem = item
            val ctx = root.context

            iconImageView.setImageResource(R.drawable.ic_book)
            nameTextView.text = item.title

            val details = mutableListOf<String>()
            if (item.authorsFormatted.isNotBlank() && item.authorsFormatted != "Неизвестный автор") {
                details.add(item.authorsFormatted)
            }
            if (item.pageCount > 0) {
                details.add("${item.pageCount} стр.")
            }
            item.isbn?.let { details.add("ISBN: $it") }

            idTextView.text = details.joinToString(" | ")

            nameTextView.alpha = 1f
            idTextView.alpha = 1f
            iconImageView.alpha = 1f
            cardView.elevation = itemView.resources.getDimension(R.dimen.library_item_elevation_available)
        }
    }

    class GoogleBookDiffCallback : DiffUtil.ItemCallback<GoogleBookDetails>() {
        override fun areItemsTheSame(oldItem: GoogleBookDetails, newItem: GoogleBookDetails): Boolean {
            return oldItem.googleVolumeId == newItem.googleVolumeId
        }

        override fun areContentsTheSame(oldItem: GoogleBookDetails, newItem: GoogleBookDetails): Boolean {
            return oldItem == newItem
        }
    }
}