package com.example.myapplication.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemLibraryItemBinding
import com.example.myapplication.domain.model.Book
import com.example.myapplication.domain.model.Disk
import com.example.myapplication.domain.model.LibraryItem
import com.example.myapplication.domain.model.Newspaper

private const val VIEW_TYPE_ITEM = 0
private const val VIEW_TYPE_LOADING_TOP = 1
private const val VIEW_TYPE_LOADING_BOTTOM = 2

private const val PAYLOAD_AVAILABILITY = "payload_availability"

sealed class AdapterItem {
    data class Data(val libraryItem: LibraryItem) : AdapterItem()
    data object LoadingTop : AdapterItem()
    data object LoadingBottom : AdapterItem()
}

class LibraryItemAdapter : ListAdapter<AdapterItem, RecyclerView.ViewHolder>(
    AdapterItemDiffCallback()
) {

    var itemClickListener: ((LibraryItem) -> Unit)? = null

    class ItemViewHolder(
        private val binding: ItemLibraryItemBinding,
        private val listener: ((LibraryItem) -> Unit)?,
        private val adapter: LibraryItemAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        private val elevationAvailablePx = itemView.resources.getDimension(R.dimen.library_item_elevation_available)
        private val elevationUnavailablePx = itemView.resources.getDimension(R.dimen.library_item_elevation_unavailable)

        init {
            binding.cardView.setOnClickListener {
                val adapterPos = bindingAdapterPosition
                if (adapterPos != RecyclerView.NO_POSITION) {
                    val adapterItem = adapter.getItem(adapterPos)
                    if (adapterItem is AdapterItem.Data) {
                        listener?.invoke(adapterItem.libraryItem)
                    }
                }
            }
        }

        fun bind(item: LibraryItem) = with(binding) {
            val ctx = root.context
            val iconResId = when (item) {
                is Book -> R.drawable.ic_book
                is Newspaper -> R.drawable.ic_newspaper
                is Disk -> R.drawable.ic_disk
            }
            iconImageView.setImageResource(iconResId)
            nameTextView.text = item.name
            idTextView.text = ctx.getString(R.string.item_id, item.id)

            updateAvailabilityVisuals(item.available)
        }

        fun updateAvailabilityVisuals(available: Boolean) = with(binding) {
            val alpha = if (available) 1f else 0.3f
            nameTextView.alpha = alpha
            idTextView.alpha = alpha
            iconImageView.alpha = alpha

            cardView.elevation = if (available) {
                elevationAvailablePx
            } else {
                elevationUnavailablePx
            }
        }
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AdapterItem.Data -> VIEW_TYPE_ITEM
            AdapterItem.LoadingTop -> VIEW_TYPE_LOADING_TOP
            AdapterItem.LoadingBottom -> VIEW_TYPE_LOADING_BOTTOM
            null -> throw IllegalStateException("Null item found in ListAdapter at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> ItemViewHolder(
                ItemLibraryItemBinding.inflate(inflater, parent, false),
                itemClickListener,
                this
            )
            VIEW_TYPE_LOADING_TOP, VIEW_TYPE_LOADING_BOTTOM -> {
                val view = inflater.inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val adapterItem = getItem(position)) {
            is AdapterItem.Data -> (holder as ItemViewHolder).bind(adapterItem.libraryItem)
            AdapterItem.LoadingTop, AdapterItem.LoadingBottom -> { }
            null -> { }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            if (holder is ItemViewHolder) {
                val item = (getItem(position) as? AdapterItem.Data)?.libraryItem
                if (item != null) {
                    payloads.forEach { payload ->
                        if (payload == PAYLOAD_AVAILABILITY) {
                            holder.updateAvailabilityVisuals(item.available)
                        }
                    }
                } else {
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
        }
    }

    class AdapterItemDiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
        override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return when {
                oldItem is AdapterItem.Data && newItem is AdapterItem.Data -> oldItem.libraryItem.id == newItem.libraryItem.id
                oldItem is AdapterItem.LoadingTop && newItem is AdapterItem.LoadingTop -> true
                oldItem is AdapterItem.LoadingBottom && newItem is AdapterItem.LoadingBottom -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return when {
                oldItem is AdapterItem.Data && newItem is AdapterItem.Data ->
                    oldItem.libraryItem == newItem.libraryItem && oldItem.libraryItem.available == newItem.libraryItem.available
                oldItem is AdapterItem.LoadingTop && newItem is AdapterItem.LoadingTop -> true
                oldItem is AdapterItem.LoadingBottom && newItem is AdapterItem.LoadingBottom -> true
                else -> false
            }
        }

        override fun getChangePayload(oldItem: AdapterItem, newItem: AdapterItem): Any? {
            return if (oldItem is AdapterItem.Data && newItem is AdapterItem.Data &&
                oldItem.libraryItem.id == newItem.libraryItem.id &&
                oldItem.libraryItem.available != newItem.libraryItem.available) {
                PAYLOAD_AVAILABILITY
            } else {
                null
            }
        }
    }
}