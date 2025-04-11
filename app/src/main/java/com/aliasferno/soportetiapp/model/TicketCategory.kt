package com.aliasferno.soportetiapp.model

enum class TicketCategory {
    HARDWARE,
    SOFTWARE,
    RED,
    IMPRESORA,
    EMAIL,
    ACCESOS,
    OTRO;

    companion object {
        fun fromString(value: String): TicketCategory {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                OTRO
            }
        }
    }
} 