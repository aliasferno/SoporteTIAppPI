package com.aliasferno.soportetiapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliasferno.soportetiapp.databinding.ItemStatBinding

data class StatItem(
    val label: String,
    val count: Int
)

class StatItemAdapter : ListAdapter<StatItem, StatItemAdapter.StatViewHolder>(StatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = ItemStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StatViewHolder(
        private val binding: ItemStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: StatItem) {
            binding.tvStatLabel.text = item.label
            binding.tvStatCount.text = item.count.toString()
        }
    }

    private class StatDiffCallback : DiffUtil.ItemCallback<StatItem>() {
        override fun areItemsTheSame(oldItem: StatItem, newItem: StatItem): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: StatItem, newItem: StatItem): Boolean {
            return oldItem == newItem
        }
    }
} 