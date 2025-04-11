package com.aliasferno.soportetiapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aliasferno.soportetiapp.adapters.StatAdapter
import com.aliasferno.soportetiapp.databinding.FragmentStatsBinding
import com.aliasferno.soportetiapp.model.Stat
import com.aliasferno.soportetiapp.model.TicketStatus
import com.aliasferno.soportetiapp.repository.TicketRepository
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var statAdapter: StatAdapter
    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadStats()
    }

    private fun setupRecyclerView() {
        statAdapter = StatAdapter()
        binding.rvStats.adapter = statAdapter
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val tickets = ticketRepository.getAllTickets()
            val stats = mutableListOf<Stat>()

            // Total de tickets
            stats.add(Stat("Total de tickets", tickets.size))

            // Tickets por estado
            val ticketsByStatus = tickets.groupBy { it.status }
            TicketStatus.values().forEach { status ->
                val count = ticketsByStatus[status]?.size ?: 0
                stats.add(Stat("Tickets ${status.name}", count))
            }

            // Tickets por categoría
            val ticketsByCategory = tickets.groupBy { it.category }
            ticketsByCategory.forEach { (category, ticketsInCategory) ->
                stats.add(Stat("Categoría: $category", ticketsInCategory.size))
            }

            statAdapter.submitList(stats)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 