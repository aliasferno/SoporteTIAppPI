package com.aliasferno.soportetiapp.model

enum class TicketStatus {
    ABIERTO,
    EN_PROCESO,
    RESUELTO,
    CERRADO;

    companion object {
        fun fromString(value: String): TicketStatus {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                ABIERTO
            }
        }
    }
} 