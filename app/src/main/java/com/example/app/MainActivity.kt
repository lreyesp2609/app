package com.example.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🏗️ ════════════════════════════════════════")
        Log.d(TAG, "🏗️ onCreate LLAMADO")
        Log.d(TAG, "🏗️ ════════════════════════════════════════")
        Log.d(TAG, "   savedInstanceState: ${if (savedInstanceState != null) "NO ES NULL" else "NULL"}")
        Log.d(TAG, "   Timestamp: ${System.currentTimeMillis()}")

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

        if (usuarioAutenticado()) {
            iniciarTrackingPasivo()
        }

        // 🔥 Procesar intent inicial
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

                    Log.d(TAG, "🎨 Compose recompuesto")
                    Log.d(TAG, "   NavController asignado: ${localNavController != null}")

                    // 🆕 Observar navegación pendiente
                    LaunchedEffect(localNavController, pendingNavigation) {
                        Log.d(TAG, "🔄 LaunchedEffect ejecutado")
                        Log.d(TAG, "   PendingNavigation: $pendingNavigation")

                        // Dar tiempo para que Compose esté listo
                        delay(1000)
                        Log.d(TAG, "⏰ Delay de 1 segundo completado")

                        pendingNavigation?.let { pending ->
                            Log.d(TAG, "🎯 ════════════════════════════════════════")
                            Log.d(TAG, "🎯 EJECUTANDO NAVEGACIÓN PENDIENTE")
                            Log.d(TAG, "🎯 ════════════════════════════════════════")
                            Log.d(TAG, "   Destino ID: ${pending.ubicacionDestinoId}")
                            Log.d(TAG, "   NavController: $localNavController")
                            Log.d(TAG, "   NavController.graph: ${localNavController.graph}")
                            Log.d(TAG, "   Ruta a navegar: rutas_screen/${pending.ubicacionDestinoId}")

                            try {
                                localNavController.navigate("rutas_screen/${pending.ubicacionDestinoId}") {
                                    popUpTo(localNavController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                                Log.d(TAG, "✅ Navegación pendiente COMPLETADA exitosamente")
                                pendingNavigation = null
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ ERROR en navegación pendiente")
                                Log.e(TAG, "   Exception: ${e.javaClass.simpleName}")
                                Log.e(TAG, "   Message: ${e.message}")
                                e.printStackTrace()
                            }
                        } ?: run {
                            Log.d(TAG, "ℹ️ No hay navegación pendiente")
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

        Log.d(TAG, "🏁 onCreate COMPLETADO")
    }

    private fun procesarIntentParaNavegacion(intent: Intent?) {
        Log.d(TAG, "🔍 ════════════════════════════════════════")
        Log.d(TAG, "🔍 PROCESANDO INTENT PARA NAVEGACIÓN")
        Log.d(TAG, "🔍 ════════════════════════════════════════")

        if (intent == null) {
            Log.d(TAG, "⚠️ Intent es NULL")
            return
        }

        // 🔥 Log COMPLETO del intent
        Log.d(TAG, "📱 DETALLES DEL INTENT:")
        Log.d(TAG, "   Action: ${intent.action}")
        Log.d(TAG, "   Data: ${intent.data}")
        Log.d(TAG, "   DataString: ${intent.dataString}")
        Log.d(TAG, "   Type: ${intent.type}")
        Log.d(TAG, "   Package: ${intent.`package`}")
        Log.d(TAG, "   Component: ${intent.component}")
        Log.d(TAG, "   Flags: ${Integer.toBinaryString(intent.flags)}")

        Log.d(TAG, "📦 EXTRAS DEL INTENT:")
        if (intent.extras != null) {
            intent.extras?.keySet()?.forEach { key ->
                val value = intent.extras?.get(key)
                Log.d(TAG, "   $key = $value (${value?.javaClass?.simpleName})")
            }
        } else {
            Log.d(TAG, "   (sin extras)")
        }

        val navigateToRoutes = intent.getBooleanExtra("NAVIGATE_TO_ROUTES", false)
        val ubicacionDestinoId = intent.getIntExtra("UBICACION_DESTINO_ID", -1)
        val fromNotification = intent.getBooleanExtra("FROM_NOTIFICATION", false)

        Log.d(TAG, "🎯 VALORES EXTRAÍDOS:")
        Log.d(TAG, "   NAVIGATE_TO_ROUTES: $navigateToRoutes")
        Log.d(TAG, "   UBICACION_DESTINO_ID: $ubicacionDestinoId")
        Log.d(TAG, "   FROM_NOTIFICATION: $fromNotification")

        // 🔥 Verificar condiciones
        Log.d(TAG, "✅ VERIFICACIÓN DE CONDICIONES:")
        Log.d(TAG, "   navigateToRoutes == true? ${navigateToRoutes == true}")
        Log.d(TAG, "   ubicacionDestinoId != -1? ${ubicacionDestinoId != -1}")
        Log.d(TAG, "   fromNotification == true? ${fromNotification == true}")
        Log.d(TAG, "   TODAS cumplidas? ${navigateToRoutes && ubicacionDestinoId != -1 && fromNotification}")

        if (navigateToRoutes && ubicacionDestinoId != -1 && fromNotification) {
            Log.d(TAG, "✅ ════════════════════════════════════════")
            Log.d(TAG, "✅ CONDICIONES CUMPLIDAS")
            Log.d(TAG, "✅ Guardando navegación pendiente")
            Log.d(TAG, "✅ ════════════════════════════════════════")

            pendingNavigation = PendingNavigation(ubicacionDestinoId)
            Log.d(TAG, "   PendingNavigation creado: $pendingNavigation")

            // 🧹 Limpiar extras para evitar reprocessing
            intent.removeExtra("NAVIGATE_TO_ROUTES")
            intent.removeExtra("UBICACION_DESTINO_ID")
            intent.removeExtra("FROM_NOTIFICATION")
            Log.d(TAG, "   Extras limpiados")
        } else {
            Log.d(TAG, "⚠️ ════════════════════════════════════════")
            Log.d(TAG, "⚠️ NO SE CUMPLEN LAS CONDICIONES")
            Log.d(TAG, "⚠️ No se navegará automáticamente")
            Log.d(TAG, "⚠️ ════════════════════════════════════════")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        Log.d(TAG, "🔔 ════════════════════════════════════════")
        Log.d(TAG, "🔔 onNewIntent LLAMADO")
        Log.d(TAG, "🔔 ════════════════════════════════════════")
        Log.d(TAG, "   Timestamp: ${System.currentTimeMillis()}")
        Log.d(TAG, "   App en foreground: ${lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)}")

        setIntent(intent)
        Log.d(TAG, "   Intent actualizado con setIntent()")

        procesarIntentParaNavegacion(intent)

        // Si el NavController ya existe, navegar inmediatamente
        Log.d(TAG, "🚀 Verificando navegación inmediata...")
        Log.d(TAG, "   NavController disponible: ${navController != null}")
        Log.d(TAG, "   PendingNavigation: $pendingNavigation")

        pendingNavigation?.let { pending ->
            navController?.let { controller ->
                Log.d(TAG, "🎯 NavController disponible, programando navegación...")

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        Log.d(TAG, "🚀 ════════════════════════════════════════")
                        Log.d(TAG, "🚀 NAVEGANDO DESDE onNewIntent")
                        Log.d(TAG, "🚀 ════════════════════════════════════════")
                        Log.d(TAG, "   Destino: rutas_screen/${pending.ubicacionDestinoId}")

                        controller.navigate("rutas_screen/${pending.ubicacionDestinoId}") {
                            popUpTo(controller.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }

                        pendingNavigation = null
                        Log.d(TAG, "✅ Navegación desde onNewIntent COMPLETADA")
                        Log.d(TAG, "   PendingNavigation limpiado")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ ERROR navegando desde onNewIntent")
                        Log.e(TAG, "   Exception: ${e.javaClass.simpleName}")
                        Log.e(TAG, "   Message: ${e.message}")
                        e.printStackTrace()
                    }
                }, 300)
            } ?: run {
                Log.d(TAG, "⚠️ NavController NO disponible aún")
                Log.d(TAG, "   La navegación se ejecutará en LaunchedEffect")
            }
        } ?: run {
            Log.d(TAG, "ℹ️ No hay navegación pendiente")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "▶️ onStart llamado")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "▶️ onResume llamado")
        Log.d(TAG, "   Intent actual: ${intent?.action}")
        Log.d(TAG, "   Intent data: ${intent?.data}")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ onPause llamado")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "⏹️ onStop llamado")
    }

    private fun usuarioAutenticado(): Boolean {
        val sessionManager = SessionManager.getInstance(this)
        val isAuthenticated = !sessionManager.getAccessToken().isNullOrEmpty()
        Log.d(TAG, "🔐 Usuario autenticado: $isAuthenticated")
        return isAuthenticated
    }

    private fun iniciarTrackingPasivo() {
        val intent = Intent(this, PassiveTrackingService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Log.d(TAG, "✅ Servicio de tracking pasivo iniciado")
    }

    private fun detenerTrackingPasivo() {
        val intent = Intent(this, PassiveTrackingService::class.java)
        intent.action = PassiveTrackingService.ACTION_STOP_TRACKING
        startService(intent)
        Log.d(TAG, "🛑 Servicio de tracking pasivo detenido")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 ════════════════════════════════════════")
        Log.d(TAG, "💀 onDestroy LLAMADO")
        Log.d(TAG, "💀 ════════════════════════════════════════")
        Log.d(TAG, "   Limpiando referencias...")

        navController = null
        pendingNavigation = null

        Log.d(TAG, "   Referencias limpiadas")
    }
}