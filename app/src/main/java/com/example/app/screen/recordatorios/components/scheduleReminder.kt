package com.example.app.screen.recordatorios.components

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.app.models.ReminderEntity
import java.util.Calendar
import kotlin.jvm.java

fun scheduleReminder(context: Context, reminder: ReminderEntity) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ✅ IMPORTANTE: reminder.days ahora es UN SOLO DÍA (no múltiples)
    Log.d("ScheduleReminder", "🔹 Programando alarma para UN día:")
    Log.d("ScheduleReminder", "   Título: ${reminder.title}")
    Log.d("ScheduleReminder", "   Día: ${reminder.days}")
    Log.d("ScheduleReminder", "   Hora: ${reminder.time}")
    Log.d("ScheduleReminder", "   ID único: ${reminder.id}")

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("reminder_id", reminder.id)
        putExtra("title", reminder.title)
        putExtra("description", reminder.description)
        putExtra("sound", reminder.sound)
        putExtra("vibration", reminder.vibration)
        putExtra("day", reminder.days)  // Un solo día
        putExtra("time", reminder.time)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id,  // ID único para cada alarma
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 🔹 Calcular la próxima ocurrencia del día de la semana
    val triggerAtMillis = calculateNextOccurrence(reminder.days, reminder.time)

    if (triggerAtMillis == null) {
        Log.e("ScheduleReminder", "❌ Error calculando próxima ocurrencia")
        Log.e("ScheduleReminder", "   Día recibido: '${reminder.days}'")
        Log.e("ScheduleReminder", "   Hora recibida: '${reminder.time}'")
        return
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
    Log.d("ScheduleReminder", "📅 Próxima ejecución: ${calendar.time}")

    // ⏰ Programar alarma exacta
    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        Log.d("ScheduleReminder", "✅ Alarma programada exitosamente")
    } catch (e: Exception) {
        Log.e("ScheduleReminder", "❌ Error al programar alarma: ${e.message}")
    }
}

private fun calculateNextOccurrence(dayName: String?, time: String?): Long? {
    if (dayName == null || time == null) {
        Log.e("ScheduleReminder", "❌ Día o hora nulos")
        return null
    }

    // ✅ VALIDAR: dayName debe ser UN SOLO DÍA, no múltiples
    if (dayName.contains(",")) {
        Log.e("ScheduleReminder", "❌ ERROR: Se recibieron múltiples días: '$dayName'")
        Log.e("ScheduleReminder", "   Esta función solo acepta UN día a la vez")
        return null
    }

    // Mapeo de nombres de días a valores Calendar
    val dayMap = mapOf(
        "Lunes" to Calendar.MONDAY,
        "Martes" to Calendar.TUESDAY,
        "Miércoles" to Calendar.WEDNESDAY,
        "Jueves" to Calendar.THURSDAY,
        "Viernes" to Calendar.FRIDAY,
        "Sábado" to Calendar.SATURDAY,
        "Domingo" to Calendar.SUNDAY
    )

    val targetDayOfWeek = dayMap[dayName.trim()]
    if (targetDayOfWeek == null) {
        Log.e("ScheduleReminder", "❌ Día no reconocido: '$dayName'")
        Log.e("ScheduleReminder", "   Días válidos: ${dayMap.keys}")
        return null
    }

    // Parsear hora (formato: "HH:mm:ss")
    val timeParts = time.split(":")
    if (timeParts.size != 3) {
        Log.e("ScheduleReminder", "❌ Formato de hora inválido: '$time'")
        Log.e("ScheduleReminder", "   Formato esperado: HH:mm:ss")
        return null
    }

    val hour = timeParts[0].toIntOrNull()
    val minute = timeParts[1].toIntOrNull()
    val second = timeParts[2].toIntOrNull()

    if (hour == null || minute == null || second == null) {
        Log.e("ScheduleReminder", "❌ Error parseando hora: '$time'")
        return null
    }

    // Crear calendario para hoy
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }

    // Calcular días de diferencia
    val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    var daysUntilTarget = targetDayOfWeek - currentDayOfWeek

    Log.d("ScheduleReminder", "📊 Cálculo:")
    Log.d("ScheduleReminder", "   Día actual: $currentDayOfWeek")
    Log.d("ScheduleReminder", "   Día objetivo: $targetDayOfWeek")
    Log.d("ScheduleReminder", "   Diferencia inicial: $daysUntilTarget días")

    // Si el día es hoy, verificar si la hora ya pasó
    if (daysUntilTarget == 0) {
        if (target.timeInMillis <= now.timeInMillis) {
            // La hora ya pasó hoy, programar para la próxima semana
            daysUntilTarget = 7
            Log.d("ScheduleReminder", "   ⏰ Hora ya pasó, programando para próxima semana")
        } else {
            Log.d("ScheduleReminder", "   ⏰ Programando para HOY")
        }
    } else if (daysUntilTarget < 0) {
        // El día ya pasó esta semana, ajustar para la próxima
        daysUntilTarget += 7
        Log.d("ScheduleReminder", "   📅 Día ya pasó, ajustando: +7 días")
    }

    // Agregar los días necesarios
    target.add(Calendar.DAY_OF_YEAR, daysUntilTarget)

    Log.d("ScheduleReminder", "   ✅ Días hasta objetivo: $daysUntilTarget")
    Log.d("ScheduleReminder", "   ✅ Fecha calculada: ${target.time}")

    return target.timeInMillis
}