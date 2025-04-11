package com.aliasferno.soportetiapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliasferno.soportetiapp.adapters.TicketAdapter
import com.aliasferno.soportetiapp.databinding.FragmentHomeBinding
import com.aliasferno.soportetiapp.repository.TicketRepository
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var ticketAdapter: TicketAdapter
    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadTickets()
        
        binding.fabNewTicket.setOnClickListener {
            // TODO: Implementar navegación a crear ticket
            Toast.makeText(requireContext(), "Crear nuevo ticket", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter { ticketId ->
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToOpenTicketFragment(ticketId)
            )
        }
        
        binding.recyclerViewTickets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ticketAdapter
        }
    }
    
    private fun loadTickets() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val tickets = ticketRepository.getAllTickets()
                ticketAdapter.submitList(tickets)
                binding.progressBar.visibility = View.GONE
                
                if (tickets.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.emptyView.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error cargando tickets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}