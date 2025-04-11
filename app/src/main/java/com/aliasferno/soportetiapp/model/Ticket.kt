package com.aliasferno.soportetiapp.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Ticket(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @get:PropertyName("createdBy")
    @set:PropertyName("createdBy")
    var createdBy: String = "",
    @get:PropertyName("assignedTo")
    @set:PropertyName("assignedTo")
    var assignedTo: String = "",
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: TicketStatus = TicketStatus.ABIERTO,
    @get:PropertyName("priority")
    @set:PropertyName("priority")
    var priority: TicketPriority = TicketPriority.MEDIUM,
    val category: String = "",
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now(),
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Timestamp = Timestamp.now(),
    @get:PropertyName("comments")
    @set:PropertyName("comments")
    var comments: List<Comment> = emptyList()
) {
    companion object {
        private const val TAG = "Ticket"
        
        fun fromMap(map: Map<String, Any>, id: String): Ticket {
            try {
                val statusStr = map["status"] as? String ?: "ABIERTO"
                val priorityStr = map["priority"] as? String ?: "MEDIUM"
                
                Log.d(TAG, "Convirtiendo ticket con ID: $id, Estado: $statusStr, Prioridad: $priorityStr")
                
                return Ticket(
                    id = id,
                    title = map["title"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    createdBy = map["createdBy"] as? String ?: "",
                    assignedTo = map["assignedTo"] as? String ?: "",
                    status = TicketStatus.fromString(statusStr),
                    priority = TicketPriority.fromString(priorityStr),
                    category = map["category"] as? String ?: "",
                    createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                    updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now(),
                    comments = (map["comments"] as? List<Map<String, Any>>)?.map { commentMap ->
                        Comment(
                            id = commentMap["id"] as? String ?: "",
                            content = commentMap["text"] as? String ?: "",
                            authorId = commentMap["createdBy"] as? String ?: "",
                            authorName = commentMap["authorName"] as? String ?: "Usuario",
                            createdAt = commentMap["createdAt"] as? Timestamp ?: Timestamp.now()
                        )
                    } ?: emptyList()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al convertir mapa a Ticket: ${e.message}", e)
                return Ticket(id = id)
            }
        }
    }
}

data class Comment(
    val id: String = "",
    val content: String = "",
    @get:PropertyName("authorId")
    @set:PropertyName("authorId")
    var authorId: String = "",
    @get:PropertyName("authorName")
    @set:PropertyName("authorName")
    var authorName: String = "",
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now()
)

enum class TicketPriority {
    LOW, MEDIUM, HIGH, CRITICAL;

    companion object {
        fun fromString(value: String): TicketPriority {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                MEDIUM
            }
        }
    }
} 