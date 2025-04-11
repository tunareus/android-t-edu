package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemLibraryItemBinding

class LibraryItemAdapter : ListAdapter<LibraryItem, LibraryItemAdapter.ItemViewHolder>(
    LibraryItemDiffCallback()
) {

    var itemClickListener: ((LibraryItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ItemViewHolder(
            ItemLibraryItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(private val binding: ItemLibraryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    itemClickListener?.invoke(getItem(pos))
                }
            }
        }

        fun bind(item: LibraryItem) = with(binding) {
            val ctx = root.context
            val iconResId = when (item) {
                is Book -> R.drawable.ic_book
                is Newspaper -> R.drawable.ic_newspaper
                is Disk -> R.drawable.ic_disk
                else -> R.drawable.ic_item
            }
            iconImageView.setImageResource(iconResId)
            nameTextView.text = item.name
            idTextView.text = ctx.getString(R.string.item_id, item.id)

            val alpha = if (item.available) 1f else 0.3f
            nameTextView.alpha = alpha
            idTextView.alpha = alpha
            iconImageView.alpha = alpha

            cardView.elevation = if (item.available)
                ctx.dpToPx(10).toFloat()
            else
                ctx.dpToPx(1).toFloat()
        }
    }

    class LibraryItemDiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
        override fun areItemsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean =
            oldItem == newItem

        override fun getChangePayload(oldItem: LibraryItem, newItem: LibraryItem): Any? =
            if (oldItem.available != newItem.available) PAYLOAD_AVAILABILITY else null
    }
}