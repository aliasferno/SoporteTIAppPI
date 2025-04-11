package com.aliasferno.soportetiapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliasferno.soportetiapp.databinding.ItemStatBinding
import com.aliasferno.soportetiapp.model.Stat

class StatAdapter : ListAdapter<Stat, StatAdapter.StatViewHolder>(StatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = ItemStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StatViewHolder(private val binding: ItemStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: Stat) {
            binding.tvStatLabel.text = stat.label
            binding.tvStatCount.text = stat.count.toString()
        }
    }

    private class StatDiffCallback : DiffUtil.ItemCallback<Stat>() {
        override fun areItemsTheSame(oldItem: Stat, newItem: Stat): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: Stat, newItem: Stat): Boolean {
            return oldItem == newItem
        }
    }
} 