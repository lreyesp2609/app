package com.example.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.app.services.PassiveTrackingService
import com.example.app.ui.theme.AppTheme
import com.example.app.utils.NotificationHelper
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.AuthViewModel
import com.example.app.viewmodel.MapViewModel
import com.example.app.viewmodel.MapViewModelFactory
import com.example.app.websocket.testWebSocketPing
import kotlinx.coroutines.delay
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null
    private var pendingNavigation: PendingNavigation? = null

    data class PendingNavigation(
        val ubicacionDestinoId: Int
    )

    companion object {
        private const val TAG = "MainActivity"
    }

    // 🆕 Launcher para permisos
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        } else {
            true
        }

        Log.d(TAG, "📍 Permisos de ubicación:")
        Log.d(TAG, "   FINE_LOCATION: $fineGranted")
        Log.d(TAG, "   COARSE_LOCATION: $coarseGranted")
        Log.d(TAG, "   BACKGROUND_LOCATION: $backgroundGranted")

        // Si tiene al menos un permiso de ubicación, iniciar servicio
        if (fineGranted || coarseGranted) {
            Log.d(TAG, "✅ Permisos de ubicación concedidos")
            iniciarTrackingPasivo()
        } else {
            Log.e(TAG, "❌ Permisos de ubicación denegados")
            Toast.makeText(
                this,
                "Los permisos de ubicación son necesarios para el tracking",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🏗️ ════════════════════════════════════════")
        Log.d(TAG, "🏗️ onCreate LLAMADO")
        Log.d(TAG, "🏗️ ════════════════════════════════════════")

        NotificationHelper.createNotificationChannel(this)
        testWebSocketPing()

        val authViewModel: AuthViewModel = ViewModelProvider(
            this,
            AuthViewModel.AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.Black.toArgb()
        window.statusBarColor = Color.Black.toArgb()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // 🔥 Verificar Y SOLICITAR permisos si el usuario está autenticado
        if (usuarioAutenticado()) {
            verificarYSolicitarPermisos()
        }

        procesarIntentParaNavegacion(intent)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mapViewModel: MapViewModel = viewModel(
                        factory = MapViewModelFactory()
                    )

                    val localNavController = rememberNavController()
                    navController = localNavController

                    LaunchedEffect(localNavController, pendingNavigation) {
                        delay(1000)

                        pendingNavigation?.let { pending ->
                            Log.d(TAG, "🎯 EJECUTANDO NAVEGACIÓN PENDIENTE")
                            try {
                                localNavController.navigate("rutas_screen/${pending.ubicacionDestinoId}") {
                                    popUpTo(localNavController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                                pendingNavigation = null
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ ERROR en navegación: ${e.message}")
                            }
                        }
                    }

                    AppNavigation(
                        authViewModel = authViewModel,
                        mapViewModel = mapViewModel,
                        navController = localNavController
                    )
                }
            }
        }
    }

    // 🔥 SIMPLIFICADO: Verificar y solicitar todos los permisos de una vez
    private fun verificarYSolicitarPermisos() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        Log.d(TAG, "🔍 Estado actual de permisos:")
        Log.d(TAG, "   FINE_LOCATION: $hasFineLocation")
        Log.d(TAG, "   COARSE_LOCATION: $hasCoarseLocation")
        Log.d(TAG, "   BACKGROUND_LOCATION: $hasBackgroundLocation")

        // Si ya tiene todos los permisos, iniciar servicio
        if ((hasFineLocation || hasCoarseLocation) && hasBackgroundLocation) {
            Log.d(TAG, "✅ Todos los permisos ya concedidos")
            iniciarTrackingPasivo()
            return
        }

        // Si falta algún permiso, solicitarlos todos juntos
        Log.d(TAG, "📱 Solicitando permisos de ubicación...")
        solicitarTodosLosPermisos()
    }

    // 🔥 NUEVO: Solicitar TODOS los permisos en una sola llamada
    private fun solicitarTodosLosPermisos() {
        val permisos = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Agregar permiso de background si es Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permisos.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        Log.d(TAG, "📋 Permisos a solicitar: ${permisos.joinToString()}")
        locationPermissionLauncher.launch(permisos.toTypedArray())
    }

    private fun procesarIntentParaNavegacion(intent: Intent?) {
        Log.d(TAG, "🔍 PROCESANDO INTENT PARA NAVEGACIÓN")

        if (intent == null) {
            Log.d(TAG, "⚠️ Intent es NULL")
            return
        }

        val navigateToRoutes = intent.getBooleanExtra("NAVIGATE_TO_ROUTES", false)
        val ubicacionDestinoId = intent.getIntExtra("UBICACION_DESTINO_ID", -1)
        val fromNotification = intent.getBooleanExtra("FROM_NOTIFICATION", false)

        if (navigateToRoutes && ubicacionDestinoId != -1 && fromNotification) {
            Log.d(TAG, "✅ Guardando navegación pendiente")
            pendingNavigation = PendingNavigation(ubicacionDestinoId)

            intent.removeExtra("NAVIGATE_TO_ROUTES")
            intent.removeExtra("UBICACION_DESTINO_ID")
            intent.removeExtra("FROM_NOTIFICATION")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "🔔 onNewIntent LLAMADO")
        setIntent(intent)
        procesarIntentParaNavegacion(intent)

        pendingNavigation?.let { pending ->
            navController?.let { controller ->
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        controller.navigate("rutas_screen/${pending.ubicacionDestinoId}") {
                            popUpTo(controller.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                        pendingNavigation = null
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ ERROR navegando: ${e.message}")
                    }
                }, 300)
            }
        }
    }

    private fun usuarioAutenticado(): Boolean {
        val sessionManager = SessionManager.getInstance(this)
        val isAuthenticated = !sessionManager.getAccessToken().isNullOrEmpty()
        Log.d(TAG, "🔐 Usuario autenticado: $isAuthenticated")
        return isAuthenticated
    }

    private fun iniciarTrackingPasivo() {
        // 🔥 VERIFICACIÓN FINAL antes de iniciar
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.e(TAG, "❌ No se puede iniciar servicio sin permisos de ubicación")
            return
        }

        try {
            val intent = Intent(this, PassiveTrackingService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            Log.d(TAG, "✅ Servicio de tracking pasivo iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando servicio: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun detenerTrackingPasivo() {
        val intent = Intent(this, PassiveTrackingService::class.java)
        intent.action = PassiveTrackingService.ACTION_STOP_TRACKING
        startService(intent)
        Log.d(TAG, "🛑 Servicio de tracking pasivo detenido")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 onDestroy LLAMADO")
        navController = null
        pendingNavigation = null
    }
}