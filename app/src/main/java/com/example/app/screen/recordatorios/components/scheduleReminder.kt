package com.example.app.screen.recordatorios.components

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.app.models.ReminderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

fun scheduleReminder(context: Context, reminder: ReminderEntity) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", reminder.title)
        putExtra("description", reminder.description)
        putExtra("sound", reminder.sound)
        putExtra("vibration", reminder.vibration)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Convertir fecha/hora a millis
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateTime = "${reminder.date} ${reminder.time}"
    val triggerAtMillis = formatter.parse(dateTime)?.time

    if (triggerAtMillis == null) {
        Log.e("ScheduleReminder", "Error parseando fecha: $dateTime")
        return
    }

    Log.d("ScheduleReminder", "Programando recordatorio para: ${Date(triggerAtMillis)}")

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
    )
}