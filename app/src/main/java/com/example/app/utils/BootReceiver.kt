package com.example.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.app.services.LocationReminderService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "📱 Dispositivo reiniciado")

            // ✅ VERIFICAR PERMISOS
            if (PermissionUtils.hasLocationPermissions(context)) {
                LocationReminderService.start(context)
                Log.d("BootReceiver", "✅ Servicio reiniciado")
            } else {
                Log.w("BootReceiver", "⚠️ Sin permisos - servicio NO iniciado")
            }
        }
    }
}