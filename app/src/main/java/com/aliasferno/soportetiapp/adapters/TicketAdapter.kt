package com.aliasferno.soportetiapp.adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aliasferno.soportetiapp.R
import com.aliasferno.soportetiapp.databinding.ItemTicketBinding
import com.aliasferno.soportetiapp.model.Ticket
import com.aliasferno.soportetiapp.model.TicketPriority
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "TicketAdapter"

class TicketAdapter(private val onTicketClick: (String) -> Unit) : ListAdapter<Ticket, TicketAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = getItem(position)
        holder.bind(ticket)
    }

    inner class TicketViewHolder(private val binding: ItemTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ticket: Ticket) {
            Log.d(TAG, "Vinculando ticket con ID: ${ticket.id}")
            binding.apply {
                tvTicketTitle.text = ticket.title
                tvTicketDescription.text = ticket.description
                tvTicketDate.text = formatDate(ticket.createdAt.toDate().time)
                tvTicketStatus.text = ticket.status.toString()
                tvTicketCategory.text = ticket.category

                // Configurar el indicador y el icono de prioridad
                val (color, icon) = getPriorityColorAndIcon(ticket.priority)
                priorityIndicator.setBackgroundColor(ContextCompat.getColor(root.context, color))
                ivPriority.setImageResource(icon)
                ivPriority.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(root.context, color))

                root.setOnClickListener {
                    Log.d(TAG, "Ticket clickeado con ID: ${ticket.id}")
                    onTicketClick(ticket.id)
                }
            }
        }

        private fun getPriorityColorAndIcon(priority: TicketPriority): Pair<Int, Int> {
            return when (priority) {
                TicketPriority.CRITICAL -> Pair(R.color.priority_critical, R.drawable.ic_priority_high)
                TicketPriority.HIGH -> Pair(R.color.priority_high, R.drawable.ic_priority_high)
                TicketPriority.MEDIUM -> Pair(R.color.priority_medium, R.drawable.ic_priority_medium)
                TicketPriority.LOW -> Pair(R.color.priority_low, R.drawable.ic_priority_low)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem == newItem
        }
    }
} 