package com.example.app.viewmodel

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED) {

                // Necesitas solicitarlo desde una Activity
                (context as? ComponentActivity)?.let { activity ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(permission),
                        100
                    )
                }
            }
        }
    }
}