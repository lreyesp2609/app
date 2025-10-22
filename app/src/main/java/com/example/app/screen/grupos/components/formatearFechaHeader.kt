package com.example.app.screen.grupos.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatearFechaHeader(fechaIso: String): String {
    return try {
        val formatoIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val fecha = formatoIso.parse(fechaIso) ?: return ""

        val hoy = Calendar.getInstance()
        val ayer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val calMensaje = Calendar.getInstance().apply { time = fecha }

        val mismoDia = hoy.get(Calendar.YEAR) == calMensaje.get(Calendar.YEAR) &&
                hoy.get(Calendar.DAY_OF_YEAR) == calMensaje.get(Calendar.DAY_OF_YEAR)
        val diaAnterior = ayer.get(Calendar.YEAR) == calMensaje.get(Calendar.YEAR) &&
                ayer.get(Calendar.DAY_OF_YEAR) == calMensaje.get(Calendar.DAY_OF_YEAR)

        when {
            mismoDia -> "Hoy"
            diaAnterior -> "Ayer"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)
        }
    } catch (e: Exception) {
        ""
    }
}

fun obtenerFechaActualISO(): String {
    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    return formato.format(Date())
}