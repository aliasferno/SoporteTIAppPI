package com.aliasferno.soportetiapp.repository

import android.util.Log
import com.aliasferno.soportetiapp.model.Comment
import com.aliasferno.soportetiapp.model.Ticket
import com.aliasferno.soportetiapp.model.TicketPriority
import com.aliasferno.soportetiapp.model.TicketStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

private const val TAG = "TicketRepository"

class TicketRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ticketsCollection = db.collection("tickets")
    private val auth = FirebaseAuth.getInstance()

    suspend fun getAllTickets(): List<Ticket> {
        return try {
            Log.d(TAG, "Iniciando getAllTickets")
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario autenticado")
                return emptyList()
            }
            
            Log.d(TAG, "Usuario actual: ${currentUser.uid}")
            
            val snapshot = ticketsCollection
                .whereEqualTo("createdBy", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d(TAG, "Documentos encontrados: ${snapshot.size()}")
            
            val tickets = snapshot.documents.mapNotNull { doc ->
                try {
                    val ticket = Ticket.fromMap(doc.data ?: emptyMap(), doc.id)
                    Log.d(TAG, "Ticket encontrado: ${ticket.title} con ID: ${ticket.id}, Estado: ${ticket.status}")
                    ticket
                } catch (e: Exception) {
                    Log.e(TAG, "Error al convertir documento a Ticket: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "Total de tickets procesados: ${tickets.size}")
            tickets
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener tickets: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTicketById(ticketId: String): Ticket? {
        return try {
            Log.d(TAG, "Obteniendo ticket con ID: $ticketId")
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario autenticado")
                return null
            }

            val doc = db.collection("tickets").document(ticketId).get().await()
            if (!doc.exists()) {
                Log.w(TAG, "No se encontró el ticket con ID: $ticketId")
                return null
            }

            val ticket = Ticket.fromMap(doc.data ?: emptyMap(), doc.id)
            if (ticket.id.isEmpty()) {
                Log.e(TAG, "Error al convertir documento a Ticket")
                return null
            }

            // Verificar permisos
            if (ticket.createdBy != currentUser.uid && ticket.assignedTo != currentUser.uid) {
                Log.w(TAG, "Usuario ${currentUser.uid} no tiene permiso para ver el ticket")
                return null
            }

            Log.d(TAG, "Ticket obtenido exitosamente: ${ticket.title}, Estado: ${ticket.status}")
            ticket
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ticket: ${e.message}", e)
            null
        }
    }

    suspend fun createTicket(
        title: String,
        description: String,
        category: String,
        priority: TicketPriority
    ): Boolean {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario autenticado")
                return false
            }

            val ticketData = mapOf(
                "title" to title,
                "description" to description,
                "category" to category,
                "priority" to priority.name,
                "status" to "ABIERTO",
                "createdBy" to currentUser.uid,
                "assignedTo" to "",
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "comments" to emptyList<Map<String, Any>>()
            )

            val docRef = ticketsCollection.add(ticketData).await()
            Log.d(TAG, "Ticket creado con ID: ${docRef.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear ticket: ${e.message}", e)
            false
        }
    }

    suspend fun updateTicket(ticket: Ticket): Boolean {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return false
            }

            val ticketData = hashMapOf(
                "title" to ticket.title,
                "description" to ticket.description,
                "category" to ticket.category,
                "priority" to ticket.priority.name,
                "status" to ticket.status.name,
                "createdBy" to ticket.createdBy,
                "assignedTo" to ticket.assignedTo,
                "comments" to ticket.comments
            )

            db.collection("tickets").document(ticket.id).update(ticketData.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateTicketStatus(ticketId: String, status: TicketStatus): Boolean {
        val currentUser = auth.currentUser ?: throw IllegalStateException("No user logged in")
        
        val ticket = getTicketById(ticketId) ?: return false
        if (ticket.createdBy != currentUser.uid && ticket.assignedTo != currentUser.uid) {
            throw SecurityException("No tienes permiso para actualizar este ticket")
        }

        return try {
            ticketsCollection.document(ticketId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addComment(ticketId: String, commentText: String): Boolean {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return false
            }

            val comment = hashMapOf(
                "text" to commentText,
                "createdBy" to currentUser.email,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            db.collection("tickets").document(ticketId)
                .update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteTicket(ticketId: String): Boolean {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return false
            }

            val ticket = getTicketById(ticketId) ?: return false
            if (ticket.createdBy != currentUser.uid) {
                throw SecurityException("No tienes permiso para eliminar este ticket")
            }

            ticketsCollection.document(ticketId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCriticalTickets(): List<Ticket> {
        return try {
            Log.d(TAG, "Iniciando getCriticalTickets")
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario autenticado")
                return emptyList()
            }
            
            Log.d(TAG, "Usuario actual: ${currentUser.uid}")
            
            val snapshot = ticketsCollection
                .whereEqualTo("createdBy", currentUser.uid)
                .whereEqualTo("priority", TicketPriority.CRITICAL.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d(TAG, "Documentos encontrados: ${snapshot.size()}")
            
            val tickets = snapshot.documents.mapNotNull { doc ->
                try {
                    val ticket = Ticket.fromMap(doc.data ?: emptyMap(), doc.id)
                    Log.d(TAG, "Ticket crítico encontrado: ${ticket.title} con ID: ${ticket.id}, Estado: ${ticket.status}")
                    ticket
                } catch (e: Exception) {
                    Log.e(TAG, "Error al convertir documento a Ticket: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "Total de tickets críticos procesados: ${tickets.size}")
            tickets
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener tickets críticos: ${e.message}", e)
            emptyList()
        }
    }
} 