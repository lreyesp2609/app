package com.rutai.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.rutai.app.services.UnifiedLocationService
import com.rutai.app.ui.theme.AppTheme
import com.rutai.app.utils.NotificationHelper
import com.rutai.app.utils.SessionManager
import com.rutai.app.viewmodel.AuthViewModel
import com.rutai.app.viewmodel.MapViewModel
import com.rutai.app.viewmodel.MapViewModelFactory
import com.rutai.app.websocket.testWebSocketPing
import kotlinx.coroutines.delay
class MainActivity : AppCompatActivity() {

    private var navController: NavHostController? = null
    private var pendingNavigation: PendingNavigation? = null
    private lateinit var authViewModel: AuthViewModel  // 🔥 AGREGAR


    data class PendingNavigation(
        val ubicacionDestinoId: Int
    )

    companion object {
        private const val TAG = "MainActivity"
    }

    // 🆕 Launcher SOLO para permisos foreground (fine + coarse)
    private val foregroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        Log.d(TAG, "📍 Permisos foreground:")
        Log.d(TAG, "   FINE_LOCATION: $fineGranted")
        Log.d(TAG, "   COARSE_LOCATION: $coarseGranted")

        if (fineGranted || coarseGranted) {
            Log.d(TAG, "✅ Permisos foreground concedidos")
            iniciarTrackingPasivo()

            // 🔥 OPCIONAL: Si necesitas background, pedirlo DESPUÉS (descomenta si lo necesitas)
            // solicitarPermisoBackground()
        } else {
            Log.e(TAG, "❌ Permisos foreground denegados")
            Toast.makeText(
                this,
                "Los permisos de ubicación son necesarios",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // 🆕 Launcher separado para background location (solo si lo necesitas)
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "📍 Permiso BACKGROUND_LOCATION: $granted")
        if (!granted) {
            Log.w(TAG, "⚠️ Background location denegado (pero el servicio funcionará igual)")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProvider(  // 🔥 GUARDAR REFERENCIA
            this,
            AuthViewModel.AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        Log.d(TAG, "🏗️ ════════════════════════════════════════")
        Log.d(TAG, "🏗️ onCreate LLAMADO")
        Log.d(TAG, "🏗️ ════════════════════════════════════════")

        NotificationHelper.createNotificationChannel(this)
        testWebSocketPing()

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
                        factory = MapViewModelFactory(this@MainActivity)
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

    override fun onResume() {
        super.onResume()
        // 🔥 Solo refrescar si ya terminó de restaurar sesión
        if (::authViewModel.isInitialized &&
            !authViewModel.isRestoringSession &&
            authViewModel.isLoggedIn) {
            Log.d(TAG, "🔄 App volvió de background - verificando token")
            authViewModel.verificarYRefrescarToken()
        }
    }

    // 🔥 CORREGIDO: Solo verificar y pedir foreground location
    private fun verificarYSolicitarPermisos() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "🔍 Estado permisos foreground:")
        Log.d(TAG, "   FINE_LOCATION: $hasFineLocation")
        Log.d(TAG, "   COARSE_LOCATION: $hasCoarseLocation")

        // Si ya tiene al menos uno, iniciar servicio
        if (hasFineLocation || hasCoarseLocation) {
            Log.d(TAG, "✅ Permisos foreground ya concedidos")
            iniciarTrackingPasivo()
            return
        }

        // Si no tiene permisos, solicitarlos
        Log.d(TAG, "📱 Solicitando permisos foreground...")
        solicitarPermisosForeground()
    }

    // 🔥 NUEVO: Solo pedir foreground location
    private fun solicitarPermisosForeground() {
        foregroundLocationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // 🔥 OPCIONAL: Solo llamar esto si REALMENTE necesitas background
    // (Por ejemplo, si el usuario activa tracking 24/7)
    private fun solicitarPermisoBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                Log.d(TAG, "📱 Solicitando permiso BACKGROUND_LOCATION...")
                backgroundLocationLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
        }
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
            val intent = Intent(this, UnifiedLocationService::class.java)

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


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 onDestroy LLAMADO")
        navController = null
        pendingNavigation = null
    }
}