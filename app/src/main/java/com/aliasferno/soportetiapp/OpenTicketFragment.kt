package com.aliasferno.soportetiapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliasferno.soportetiapp.adapters.CommentAdapter
import com.aliasferno.soportetiapp.databinding.FragmentOpenTicketBinding
import com.aliasferno.soportetiapp.model.Comment
import com.aliasferno.soportetiapp.model.Ticket
import com.aliasferno.soportetiapp.model.TicketPriority
import com.aliasferno.soportetiapp.model.TicketStatus
import com.aliasferno.soportetiapp.repository.TicketRepository
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private const val TAG = "OpenTicketFragment"

class OpenTicketFragment : Fragment() {
    private var _binding: FragmentOpenTicketBinding? = null
    private val binding get() = _binding!!
    private val args: OpenTicketFragmentArgs by navArgs()
    private val ticketRepository = TicketRepository()
    private val commentAdapter = CommentAdapter()
    private var currentTicket: Ticket? = null
    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpenTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated - ticketId: ${args.ticketId}")
        
        if (args.ticketId.isBlank()) {
            Log.e(TAG, "El ticketId está vacío")
            Snackbar.make(binding.root, "Error: ID de ticket inválido", Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }
        
        setupCommentRecyclerView()
        setupSpinners()
        setupListeners()
        loadTicket()
    }

    private fun setupCommentRecyclerView() {
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSpinners() {
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            TicketStatus.values().map { it.toString() }
        )
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            TicketPriority.values().map { it.toString() }
        )

        binding.spinnerStatus.setAdapter(statusAdapter)
        binding.spinnerPriority.setAdapter(priorityAdapter)
    }

    private fun loadTicket() {
        Log.d(TAG, "Cargando ticket con ID: ${args.ticketId}")
        lifecycleScope.launch {
            try {
                val ticket = ticketRepository.getTicketById(args.ticketId)
                if (ticket != null) {
                    Log.d(TAG, "Ticket cargado exitosamente: ${ticket.title}")
                    currentTicket = ticket
                    updateUI(ticket)
                } else {
                    Log.e(TAG, "No se encontró el ticket con ID: ${args.ticketId}")
                    Snackbar.make(binding.root, "No se encontró el ticket", Snackbar.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar el ticket", e)
                Snackbar.make(binding.root, "Error al cargar el ticket: ${e.message}", Snackbar.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun updateUI(ticket: Ticket) {
        Log.d(TAG, "Actualizando UI con ticket: ${ticket.title}")
        binding.apply {
            etTitle.setText(ticket.title)
            etDescription.setText(ticket.description)
            etCategory.setText(ticket.category)
            spinnerStatus.setText(ticket.status.toString(), false)
            spinnerPriority.setText(ticket.priority.toString(), false)
            commentAdapter.submitList(ticket.comments)
            
            // Deshabilitar campos inicialmente
            setFieldsEnabled(false)
            
            // Mostrar el FAB de edición
            fabEdit.visibility = View.VISIBLE
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        binding.apply {
            etTitle.isEnabled = enabled
            etDescription.isEnabled = enabled
            etCategory.isEnabled = enabled
            spinnerStatus.isEnabled = enabled
            spinnerPriority.isEnabled = enabled
        }
    }

    private fun setupListeners() {
        binding.fabEdit.setOnClickListener {
            if (isEditing) {
                saveChanges()
            } else {
                isEditing = true
                setFieldsEnabled(true)
                binding.fabEdit.setImageResource(android.R.drawable.ic_menu_save)
            }
        }

        binding.btnAddComment.setOnClickListener {
            val commentText = binding.etNewComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            }
        }
    }

    private fun saveChanges() {
        val updatedTicket = currentTicket?.copy(
            title = binding.etTitle.text.toString(),
            description = binding.etDescription.text.toString(),
            category = binding.etCategory.text.toString(),
            status = TicketStatus.valueOf(binding.spinnerStatus.text.toString()),
            priority = TicketPriority.valueOf(binding.spinnerPriority.text.toString())
        )

        if (updatedTicket != null) {
            lifecycleScope.launch {
                try {
                    ticketRepository.updateTicket(updatedTicket)
                    isEditing = false
                    setFieldsEnabled(false)
                    binding.fabEdit.setImageResource(android.R.drawable.ic_menu_edit)
                    Snackbar.make(binding.root, "Ticket actualizado", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Error al actualizar: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addComment(commentText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Snackbar.make(binding.root, "Debes iniciar sesión para comentar", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                currentTicket?.let { ticket ->
                    val comment = Comment(
                        id = "",
                        content = commentText,
                        authorId = currentUser.uid,
                        authorName = currentUser.displayName ?: "Usuario"
                    )
                    ticketRepository.addComment(ticket.id, commentText)
                    binding.etNewComment.text?.clear()
                    loadTicket()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error al agregar comentario: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}