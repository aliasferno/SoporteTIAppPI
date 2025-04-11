package com.aliasferno.soportetiapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliasferno.soportetiapp.adapters.TicketAdapter
import com.aliasferno.soportetiapp.databinding.FragmentNotificationsBinding
import com.aliasferno.soportetiapp.model.Ticket
import com.aliasferno.soportetiapp.model.TicketStatus
import com.aliasferno.soportetiapp.repository.TicketRepository
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var ticketAdapter: TicketAdapter
    private val repository = TicketRepository()
    private var allCriticalTickets = listOf<Ticket>()
    private var currentFilter: TicketStatus? = null
    private var currentSortOrder = SortOrder.DATE_DESC
    private var searchQuery = ""
    
    companion object {
        private const val TAG = "NotificationsFragment"
    }

    enum class SortOrder {
        DATE_DESC, DATE_ASC, TITLE_ASC, TITLE_DESC
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_notifications, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadCriticalTickets()
                true
            }
            R.id.sort_by_date -> {
                currentSortOrder = SortOrder.DATE_DESC
                applySortAndFilter()
                true
            }
            R.id.sort_by_date_oldest -> {
                currentSortOrder = SortOrder.DATE_ASC
                applySortAndFilter()
                true
            }
            R.id.sort_by_title -> {
                currentSortOrder = SortOrder.TITLE_ASC
                applySortAndFilter()
                true
            }
            R.id.sort_by_title_desc -> {
                currentSortOrder = SortOrder.TITLE_DESC
                applySortAndFilter()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated iniciado")
        setupRecyclerView()
        setupFilters()
        setupSearch()
        loadCriticalTickets()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Configurando RecyclerView")
        ticketAdapter = TicketAdapter { ticketId ->
            Log.d(TAG, "Navegando al ticket con ID: $ticketId")
            findNavController().navigate(
                NotificationsFragmentDirections.actionNotificationsFragmentToOpenTicketFragment(ticketId)
            )
        }
        
        binding.rvCriticalTickets.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ticketAdapter
        }
    }

    private fun setupFilters() {
        Log.d(TAG, "Configurando filtros")
        binding.chipGroupFilters.setOnCheckedChangeListener { group, checkedId ->
            Log.d(TAG, "Chip seleccionado con ID: $checkedId")
            
            currentFilter = when (checkedId) {
                R.id.chipAll -> {
                    Log.d(TAG, "Filtro seleccionado: Todos")
                    null
                }
                R.id.chipOpen -> {
                    Log.d(TAG, "Filtro seleccionado: Abiertos")
                    TicketStatus.ABIERTO
                }
                R.id.chipInProgress -> {
                    Log.d(TAG, "Filtro seleccionado: En Proceso")
                    TicketStatus.EN_PROCESO
                }
                R.id.chipResolved -> {
                    Log.d(TAG, "Filtro seleccionado: Resueltos")
                    TicketStatus.RESUELTO
                }
                else -> {
                    Log.d(TAG, "Filtro no reconocido: $checkedId")
                    null
                }
            }
            
            Log.d(TAG, "Aplicando filtro: $currentFilter")
            applySortAndFilter()
        }
        
        // Seleccionar el chip "Todos" por defecto
        binding.chipGroupFilters.check(R.id.chipAll)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: android.text.Editable?) {
                searchQuery = s.toString().trim().lowercase()
                applySortAndFilter()
            }
        })
    }

    private fun applySortAndFilter() {
        Log.d(TAG, "Aplicando filtro: $currentFilter, búsqueda: '$searchQuery', orden: $currentSortOrder")
        
        // Primero filtrar por estado
        val filteredByStatus = if (currentFilter == null) {
            Log.d(TAG, "Sin filtro de estado, mostrando todos los tickets")
            allCriticalTickets
        } else {
            Log.d(TAG, "Filtrando por estado: $currentFilter")
            allCriticalTickets.filter { ticket -> 
                val matches = ticket.status == currentFilter
                Log.d(TAG, "Ticket ${ticket.id} - Estado: ${ticket.status}, Coincide: $matches")
                matches
            }
        }
        
        Log.d(TAG, "Tickets después de filtrar por estado: ${filteredByStatus.size}")
        
        // Luego filtrar por búsqueda
        val filteredBySearch = if (searchQuery.isEmpty()) {
            filteredByStatus
        } else {
            Log.d(TAG, "Filtrando por búsqueda: '$searchQuery'")
            filteredByStatus.filter { 
                it.title.lowercase().contains(searchQuery) || 
                it.description.lowercase().contains(searchQuery) ||
                it.category.lowercase().contains(searchQuery)
            }
        }
        
        Log.d(TAG, "Tickets después de filtrar por búsqueda: ${filteredBySearch.size}")
        
        // Finalmente ordenar
        val sortedTickets = when (currentSortOrder) {
            SortOrder.DATE_DESC -> filteredBySearch.sortedByDescending { it.createdAt }
            SortOrder.DATE_ASC -> filteredBySearch.sortedBy { it.createdAt }
            SortOrder.TITLE_ASC -> filteredBySearch.sortedBy { it.title }
            SortOrder.TITLE_DESC -> filteredBySearch.sortedByDescending { it.title }
        }
        
        Log.d(TAG, "Tickets después de ordenar: ${sortedTickets.size}")
        
        if (sortedTickets.isEmpty()) {
            Log.d(TAG, "No hay tickets que mostrar")
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCriticalTickets.visibility = View.GONE
        } else {
            Log.d(TAG, "Mostrando ${sortedTickets.size} tickets")
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCriticalTickets.visibility = View.VISIBLE
            ticketAdapter.submitList(sortedTickets)
        }
    }

    private fun loadCriticalTickets() {
        Log.d(TAG, "Iniciando carga de tickets críticos")
        binding.progressBar.visibility = View.VISIBLE
        binding.rvCriticalTickets.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                allCriticalTickets = repository.getCriticalTickets()
                Log.d(TAG, "Tickets críticos obtenidos: ${allCriticalTickets.size}")
                
                // Actualizar contadores
                updateCounters()
                
                if (allCriticalTickets.isEmpty()) {
                    Log.d(TAG, "No hay tickets críticos para mostrar")
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    Log.d(TAG, "Mostrando ${allCriticalTickets.size} tickets críticos")
                    applySortAndFilter()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar tickets críticos", e)
                Toast.makeText(requireContext(), "Error al cargar los tickets críticos: ${e.message}", Toast.LENGTH_LONG).show()
                binding.tvEmptyState.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateCounters() {
        // Actualizar contador total
        binding.tvTotalCount.text = "Total: ${allCriticalTickets.size}"
        
        // Contar tickets sin resolver (abiertos o en proceso)
        val unresolvedCount = allCriticalTickets.count { 
            it.status == TicketStatus.ABIERTO || it.status == TicketStatus.EN_PROCESO 
        }
        binding.tvUnresolvedCount.text = unresolvedCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 