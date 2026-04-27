package com.rutai.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rutai.app.services.UnifiedLocationService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "📱 Dispositivo reiniciado")

            // ✅ VERIFICAR PERMISOS
            if (PermissionUtils.hasLocationPermissions(context)) {
                UnifiedLocationService.start(context)
                Log.d("BootReceiver", "✅ Servicio reiniciado")
            } else {
                Log.w("BootReceiver", "⚠️ Sin permisos - servicio NO iniciado")
            }
        }
    }
}