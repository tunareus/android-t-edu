package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemLibraryItemBinding

class LibraryItemAdapter : ListAdapter<LibraryItem, LibraryItemAdapter.ItemViewHolder>(
    LibraryItemDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemLibraryItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(private val binding: ItemLibraryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    item.available = !item.available
                    notifyItemChanged(pos, "payload_availability")
                    Toast.makeText(
                        binding.root.context,
                        binding.root.context.getString(R.string.item_click_text, item.id),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        fun bind(item: LibraryItem) {
            val context = binding.root.context
            val iconResId = when (item) {
                is Book -> R.drawable.ic_book
                is Newspaper -> R.drawable.ic_newspaper
                is Disk -> R.drawable.ic_disk
                else -> R.drawable.ic_item
            }
            binding.iconImageView.setImageResource(iconResId)
            binding.nameTextView.text = item.name
            binding.idTextView.text = context.getString(R.string.item_id, item.id)

            val alpha = if (item.available) 1.0f else 0.3f
            binding.nameTextView.alpha = alpha
            binding.idTextView.alpha = alpha
            binding.iconImageView.alpha = alpha

            val elevationDp = if (item.available) 10f else 1f
            binding.cardView.elevation =
                elevationDp * context.resources.displayMetrics.density
        }
    }

    class LibraryItemDiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
        override fun areItemsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
            return oldItem.id == newItem.id && oldItem::class == newItem::class
        }

        override fun areContentsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: LibraryItem, newItem: LibraryItem): Any? {
            return if (oldItem.available != newItem.available) "payload_availability" else null
        }
    }
}