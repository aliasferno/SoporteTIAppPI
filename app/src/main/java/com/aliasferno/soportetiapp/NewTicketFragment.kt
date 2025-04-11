package com.aliasferno.soportetiapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aliasferno.soportetiapp.databinding.FragmentNewTicketBinding
import com.aliasferno.soportetiapp.model.TicketCategory
import com.aliasferno.soportetiapp.model.TicketPriority
import com.aliasferno.soportetiapp.repository.TicketRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class NewTicketFragment : Fragment() {
    private var _binding: FragmentNewTicketBinding? = null
    private val binding get() = _binding!!
    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val priorities = TicketPriority.values().map { it.name }
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            priorities
        )
        (binding.spinnerPriority as? AutoCompleteTextView)?.let { autoComplete ->
            autoComplete.setAdapter(priorityAdapter)
            autoComplete.setText(priorities[0], false)
        }

        val categories = TicketCategory.values().map { it.name }
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        (binding.spinnerCategory as? AutoCompleteTextView)?.let { autoComplete ->
            autoComplete.setAdapter(categoryAdapter)
            autoComplete.setText(categories[0], false)
        }
    }

    private fun setupListeners() {
        binding.btnCreateTicket.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val category = binding.spinnerCategory.text.toString()
            val priority = binding.spinnerPriority.text.toString()

            if (title.isBlank() || description.isBlank()) {
                Toast.makeText(context, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(context, "Debe iniciar sesión para crear un ticket", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val success = ticketRepository.createTicket(
                        title = title,
                        description = description,
                        category = category,
                        priority = TicketPriority.valueOf(priority)
                    )
                    
                    if (success) {
                        findNavController().navigateUp()
                        Toast.makeText(context, "Ticket creado exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al crear el ticket", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al crear el ticket: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}