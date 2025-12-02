package com.example.app.screen.grupos.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun formatearFechaHeader(fechaIso: String): String {
    return try {
        // Parsear la fecha del mensaje en UTC
        val formatoIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val fechaMensaje = formatoIso.parse(fechaIso) ?: return ""

        // Convertir a hora local del dispositivo para mostrar
        val calMensajeLocal = Calendar.getInstance().apply {
            time = fechaMensaje
        }

        // Obtener "hoy" y "ayer" en UTC (no en hora local)
        val hoyUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val ayerUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        // Convertir el mensaje también a UTC para comparar
        val calMensajeUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = fechaMensaje
        }

        val mismoDia = hoyUTC.get(Calendar.YEAR) == calMensajeUTC.get(Calendar.YEAR) &&
                hoyUTC.get(Calendar.DAY_OF_YEAR) == calMensajeUTC.get(Calendar.DAY_OF_YEAR)
        val diaAnterior = ayerUTC.get(Calendar.YEAR) == calMensajeUTC.get(Calendar.YEAR) &&
                ayerUTC.get(Calendar.DAY_OF_YEAR) == calMensajeUTC.get(Calendar.DAY_OF_YEAR)

        when {
            mismoDia -> "Hoy"
            diaAnterior -> "Ayer"
            // Mostrar en formato local
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calMensajeLocal.time)
        }
    } catch (e: Exception) {
        ""
    }
}