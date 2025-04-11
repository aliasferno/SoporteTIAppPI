package com.aliasferno.soportetiapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aliasferno.soportetiapp.databinding.FragmentCreateTicketBinding
import com.aliasferno.soportetiapp.model.TicketPriority
import com.aliasferno.soportetiapp.repository.TicketRepository
import kotlinx.coroutines.launch

class CreateTicketFragment : Fragment() {
    private var _binding: FragmentCreateTicketBinding? = null
    private val binding get() = _binding!!
    private val ticketRepository = TicketRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        // Configurar spinner de prioridad
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            TicketPriority.values().map { it.name }
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPriority.setAdapter(adapter)
            // Establecer un valor por defecto
            binding.spinnerPriority.setText(TicketPriority.MEDIUM.name, false)
        }
    }

    private fun setupListeners() {
        binding.btnCreateTicket.setOnClickListener {
            createTicket()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun createTicket() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val category = binding.etCategory.text.toString().trim()
        val priorityText = binding.spinnerPriority.text.toString()

        if (title.isEmpty()) {
            binding.tilTitle.error = "El título es requerido"
            return
        }

        if (description.isEmpty()) {
            binding.tilDescription.error = "La descripción es requerida"
            return
        }

        if (category.isEmpty()) {
            binding.tilCategory.error = "La categoría es requerida"
            return
        }

        if (priorityText.isEmpty()) {
            binding.tilPriority.error = "La prioridad es requerida"
            return
        }

        val priority = try {
            TicketPriority.valueOf(priorityText)
        } catch (e: IllegalArgumentException) {
            binding.tilPriority.error = "Prioridad inválida"
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnCreateTicket.isEnabled = false

                if (ticketRepository.createTicket(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority
                )) {
                    Toast.makeText(requireContext(), "Ticket creado exitosamente", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Error al crear el ticket", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnCreateTicket.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 