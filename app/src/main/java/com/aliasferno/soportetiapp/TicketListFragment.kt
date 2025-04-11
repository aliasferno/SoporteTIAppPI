package com.aliasferno.soportetiapp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliasferno.soportetiapp.adapters.TicketAdapter
import com.aliasferno.soportetiapp.databinding.FragmentTicketListBinding
import com.aliasferno.soportetiapp.repository.TicketRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

private const val TAG = "TicketListFragment"

class TicketListFragment : Fragment() {
    private var _binding: FragmentTicketListBinding? = null
    private val binding get() = _binding!!
    private lateinit var ticketAdapter: TicketAdapter
    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated iniciado")

        setupRecyclerView()
        loadTickets()

        binding.fabAddTicket.setOnClickListener {
            findNavController().navigate(R.id.action_ticketListFragment_to_newTicketFragment)
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Configurando RecyclerView")
        ticketAdapter = TicketAdapter { ticketId ->
            findNavController().navigate(
                TicketListFragmentDirections.actionTicketListFragmentToOpenTicketFragment(ticketId)
            )
        }
        
        binding.rvTickets.apply {
            adapter = ticketAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.RED)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val ticket = ticketAdapter.currentList[position]
                
                lifecycleScope.launch {
                    try {
                        if (ticketRepository.deleteTicket(ticket.id)) {
                            val newList = ticketAdapter.currentList.toMutableList()
                            newList.removeAt(position)
                            ticketAdapter.submitList(newList)
                            
                            Snackbar.make(
                                binding.root,
                                "Ticket eliminado",
                                Snackbar.LENGTH_LONG
                            ).setAction("Deshacer") {
                                val undoList = ticketAdapter.currentList.toMutableList()
                                undoList.add(position, ticket)
                                ticketAdapter.submitList(undoList)
                                // Note: This doesn't actually restore the ticket in Firestore
                            }.show()
                        } else {
                            // Si falla la eliminación, recargamos la lista para restaurar el item
                            loadTickets()
                            Snackbar.make(
                                binding.root,
                                "No se pudo eliminar el ticket",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        loadTickets()
                        Snackbar.make(
                            binding.root,
                            "Error: ${e.message}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2

                // Draw background
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                // Draw delete icon
                deleteIcon?.let { icon ->
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + icon.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvTickets)
    }

    private fun loadTickets() {
        Log.d(TAG, "Iniciando carga de tickets")
        lifecycleScope.launch {
            try {
                val tickets = ticketRepository.getAllTickets()
                Log.d(TAG, "Tickets obtenidos: ${tickets.size}")
                ticketAdapter.submitList(tickets)
                
                // Mostrar mensaje si no hay tickets
                binding.tvEmptyState.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
                binding.rvTickets.visibility = if (tickets.isEmpty()) View.GONE else View.VISIBLE
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar tickets", e)
                Toast.makeText(requireContext(), "Error al cargar los tickets: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 