package com.example.app.viewmodel

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.models.Reminder
import com.example.app.models.ReminderEntity
import com.example.app.models.User
import com.example.app.models.toReminder
import com.example.app.network.AppDatabase
import com.example.app.network.RetrofitClient
import com.example.app.repository.AuthRepository
import com.example.app.repository.ReminderRepository
import com.example.app.screen.recordatorios.components.ReminderReceiver
import com.example.app.screen.recordatorios.components.scheduleReminder
import com.example.app.services.UnifiedLocationService
import com.example.app.utils.PermissionUtils
import com.example.app.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class AuthViewModel(private val context: Context) : ViewModel() {
    private val repository = AuthRepository()
    private val sessionManager = SessionManager.getInstance(context)

    companion object {
        private const val TAG = "AuthViewModel"
        private const val PREF_PENDING_TRACKING = "pending_tracking_start"  // 🔥 NUEVO
    }

    // Estados de la UI
    var isLoading by mutableStateOf(false)
        private set
    var user by mutableStateOf<User?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set
    var accessToken by mutableStateOf<String?>(null)
        private set

    init {
        // 🔹 Restaurar estado inmediatamente desde SharedPreferences
        restoreStateFromPrefs()
        restoreSession()
        startAutoRefresh()
    }

    // 🔹 Restaurar estado desde SharedPreferences
    private fun restoreStateFromPrefs() {
        isLoggedIn = sessionManager.isLoggedIn()
        accessToken = sessionManager.getAccessToken()
        user = sessionManager.getUser()
    }

    // 🔹 Restaurar sesión automáticamente al iniciar la app
    private fun restoreSession() {
        val savedRefresh = sessionManager.getRefreshToken()
        if (savedRefresh != null && sessionManager.hasValidSession()) {
            viewModelScope.launch {
                isLoading = true
                repository.refreshToken(savedRefresh).fold(
                    onSuccess = { response ->
                        accessToken = response.accessToken
                        isLoggedIn = true
                        sessionManager.saveTokens(response.accessToken, response.refreshToken)
                        sessionManager.saveLoginState(true)

                        if (user == null) {
                            getCurrentUser {
                                restoreUserReminders(context, response.accessToken)

                                // 🔥 NUEVO: Iniciar tracking al restaurar sesión
                                iniciarTrackingPasivoDespuesDeLogin()

                                isLoading = false
                            }
                        } else {
                            restoreUserReminders(context, response.accessToken)

                            // 🔥 NUEVO: Iniciar tracking al restaurar sesión
                            iniciarTrackingPasivoDespuesDeLogin()

                            isLoading = false
                        }
                    },
                    onFailure = {
                        clearLocalSession()
                        isLoading = false
                    }
                )
            }
        } else if (!sessionManager.hasValidSession()) {
            clearLocalSession()
            isLoading = false
        } else {
            isLoading = false
        }
    }

    fun actualizarPerfil(nombre: String, apellido: String) {
        val token = accessToken ?: return
        viewModelScope.launch {
            val result = repository.actualizarPerfil(token, nombre, apellido)
            result.onSuccess { profileResponse ->
                // user es mutableStateOf, no MutableStateFlow
                user = user?.copy(
                    nombre = profileResponse.nombre,
                    apellido = profileResponse.apellido
                )
                // Actualizar también en SharedPreferences
                user?.let { sessionManager.saveUser(it) }
                Log.d(TAG, "✅ Perfil actualizado: ${profileResponse.nombre}")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error actualizando perfil: ${error.message}")
            }
        }
    }

    // 🔹 Función de login (actualizada)
    // Dentro de la función login(), después de guardar tokens:

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            repository.login(email, password, Build.MODEL, getAppVersion(context), obtenerIp())
                .fold(
                    onSuccess = { loginResponse ->
                        accessToken = loginResponse.accessToken
                        isLoggedIn = true

                        sessionManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                        sessionManager.saveLoginState(true)

                        getCurrentUser {
                            restoreUserReminders(context, loginResponse.accessToken)
                            enviarTokenFCMPendiente()

                            // 🔥 NUEVO: Iniciar tracking pasivo después del login
                            iniciarTrackingPasivoDespuesDeLogin()

                            onResult(true)
                        }
                    },
                    onFailure = { exception ->
                        isLoggedIn = false
                        sessionManager.saveLoginState(false)
                        isLoading = false
                        errorMessage = when {
                            exception.message?.contains("INVALID_CREDENTIALS") == true -> {
                                "Correo o contraseña incorrectos"
                            }
                            exception.message?.contains("USER_NOT_FOUND") == true -> {
                                "No existe una cuenta con este correo"
                            }
                            exception.message?.contains("ACCOUNT_LOCKED") == true -> {
                                "Cuenta bloqueada. Contacta soporte"
                            }
                            exception.message?.contains("NETWORK_ERROR") == true -> {
                                "Error de conexión. Verifica tu internet"
                            }
                            exception.message?.contains("401") == true -> {
                                "Credenciales inválidas"
                            }
                            exception.message?.contains("500") == true -> {
                                "Error del servidor. Inténtalo más tarde"
                            }
                            else -> {
                                "Error al iniciar sesión. Inténtalo nuevamente"
                            }
                        }
                        onResult(false)
                    }
                )
        }
    }

    // 🆕 NUEVA FUNCIÓN: Iniciar tracking después del login
    private fun iniciarTrackingPasivoDespuesDeLogin() {
        Log.d(TAG, "🚀 ════════════════════════════════════════")
        Log.d(TAG, "🚀 INICIANDO TRACKING PASIVO POST-LOGIN")
        Log.d(TAG, "🚀 ════════════════════════════════════════")

        if (hasLocationPermissions()) {
            startTrackingService()
            Log.d(TAG, "✅ Servicio iniciado correctamente")
        } else {
            Log.w(TAG, "⚠️ No hay permisos de ubicación")
            Log.w(TAG, "⚠️ Marcando para iniciar después de conceder permisos")

            // 🔥 Marcar como pendiente
            val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_PENDING_TRACKING, true).apply()
        }
    }

    /**
     * 🔥 NUEVA FUNCIÓN: Reinicia el tracking si estaba pendiente
     * Llamar desde HomeScreen cuando se concedan permisos
     */
    fun reiniciarTrackingSiPendiente() {
        val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(PREF_PENDING_TRACKING, false)

        if (pending && hasLocationPermissions()) {
            Log.d(TAG, "🔄 ════════════════════════════════════════")
            Log.d(TAG, "🔄 REINICIANDO TRACKING DESPUÉS DE PERMISOS")
            Log.d(TAG, "🔄 ════════════════════════════════════════")

            startTrackingService()

            // Limpiar flag
            prefs.edit().putBoolean(PREF_PENDING_TRACKING, false).apply()

            Log.d(TAG, "✅ Tracking reiniciado exitosamente")
        } else if (!pending) {
            Log.d(TAG, "ℹ️ No hay tracking pendiente")
        } else if (!hasLocationPermissions()) {
            Log.w(TAG, "⚠️ Aún faltan permisos de ubicación")
        }
    }

    /**
     * 🔥 NUEVA FUNCIÓN: Inicia el servicio de tracking
     */
    private fun startTrackingService() {
        try {
            val intent = Intent(context, UnifiedLocationService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(TAG, "📍 UnifiedLocationService iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando servicio: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 🔥 NUEVA FUNCIÓN: Verifica permisos
     */
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation && coarseLocation
    }

    private fun restoreUserReminders(context: Context, token: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Restaurando recordatorios del usuario...")

                val database = AppDatabase.getDatabase(context)
                val repository = ReminderRepository(database.reminderDao())

                val response = RetrofitClient.reminderService.getReminders("Bearer $token")

                if (response.isSuccessful) {
                    val apiReminders = response.body() ?: emptyList()
                    Log.d(TAG, "📥 ${apiReminders.size} recordatorios obtenidos de la API")

                    apiReminders.forEach { reminderResponse ->
                        val reminder = reminderResponse.toReminder()

                        val reminderEntity = ReminderEntity(
                            id = reminderResponse.id,
                            title = reminderResponse.title,
                            description = reminderResponse.description,
                            reminder_type = reminderResponse.reminder_type,
                            trigger_type = reminderResponse.trigger_type,
                            sound_uri = reminderResponse.sound_uri,  // ← CAMBIO: sound_type → sound_uri
                            vibration = reminderResponse.vibration,
                            sound = reminderResponse.sound,
                            days = reminderResponse.days,
                            time = reminderResponse.time,
                            location = reminderResponse.location,
                            latitude = reminderResponse.latitude,
                            longitude = reminderResponse.longitude,
                            radius = reminderResponse.radius?.toFloat(),
                            user_id = reminderResponse.user_id,
                            is_active = reminderResponse.is_active,
                            is_deleted = reminderResponse.is_deleted
                        )

                        repository.saveReminder(reminderEntity)

                        // 🔥 SOLO REPROGRAMAR SI ESTÁ ACTIVO
                        if (reminderEntity.is_active && !reminderEntity.is_deleted) {
                            when (reminderEntity.reminder_type) {
                                "datetime" -> {
                                    reprogramarAlarmasFechaHora(context, reminder, reminderEntity)
                                }
                                "location", "both" -> {
                                    // ✅ VERIFICAR PERMISOS ANTES DE INICIAR
                                    if (PermissionUtils.hasLocationPermissions(context)) {
                                        UnifiedLocationService.start(context)
                                        Log.d(TAG, "✅ Servicio de ubicación iniciado")
                                    } else {
                                        Log.w(TAG, "⚠️ No se puede iniciar servicio: sin permisos de ubicación")
                                    }
                                }
                            }

                            // 🔥 PROGRAMAR ALARMAS SI ES "both"
                            if (reminderEntity.reminder_type == "both") {
                                reprogramarAlarmasFechaHora(context, reminder, reminderEntity)
                            }
                        }
                    }

                    Log.d(TAG, "✅ Recordatorios restaurados y alarmas reprogramadas")
                } else {
                    Log.e(TAG, "❌ Error al obtener recordatorios: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error restaurando recordatorios: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun reprogramarAlarmasFechaHora(
        context: Context,
        reminder: Reminder,
        reminderEntity: ReminderEntity
    ) {
        if (reminder.days.isNullOrEmpty() || reminder.time.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "⏰ Reprogramando alarmas para: ${reminder.title}")

        reminder.days.forEachIndexed { index, day ->
            val uniqueId = reminderEntity.id * 100 + index

            val singleDayReminder = reminderEntity.copy(
                id = uniqueId,
                days = day
            )

            scheduleReminder(context, singleDayReminder)
        }
    }
    // 🔹 Función para obtener usuario (actualizada)
    fun getCurrentUser(onSuccess: () -> Unit = {}) {
        accessToken?.let { token ->
            viewModelScope.launch {
                isLoading = true
                repository.getCurrentUser("Bearer $token").fold(
                    onSuccess = { currentUser ->
                        user = currentUser
                        // 🔹 Guardar usuario en SharedPreferences
                        sessionManager.saveUser(currentUser)
                        isLoading = false
                        errorMessage = null
                        onSuccess()
                    },
                    onFailure = {
                        user = null
                        isLoggedIn = false
                        accessToken = null
                        sessionManager.saveLoginState(false)
                        isLoading = false
                        errorMessage = it.message
                    }
                )
            }
        } ?: run {
            user = null
            isLoggedIn = false
            sessionManager.saveLoginState(false)
            errorMessage = "No hay token de acceso"
            isLoading = false
        }
    }

    // Dentro de AuthViewModel
    private lateinit var reminderViewModel: ReminderViewModel // O inyecta el repository

    fun logout(
        context: Context,
        shouldRemoveFCMToken: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        Log.d(TAG, "🔓 ════════════════════════════════════════")
        Log.d(TAG, "🔓 LOGOUT INICIADO")
        Log.d(TAG, "🔓 Eliminar FCM: $shouldRemoveFCMToken")
        Log.d(TAG, "🔓 Llamado desde:")
        Thread.currentThread().stackTrace.take(6).forEach {
            Log.d(TAG, "   $it")
        }
        Log.d(TAG, "🔓 ════════════════════════════════════════")

        val savedRefresh = sessionManager.getRefreshToken()

        viewModelScope.launch {
            if (shouldRemoveFCMToken) {
                try {
                    accessToken?.let { token ->
                        repository.eliminarTokenFCM("Bearer $token")
                        Log.d(TAG, "✅ Token FCM eliminado del backend")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error eliminando token FCM: ${e.message}")
                }
            } else {
                Log.d(TAG, "ℹ️ Token FCM NO eliminado (logout automático)")
            }

            try {
                cancelAllRemindersAndCleanup(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error limpiando recordatorios: ${e.message}")
            }

            if (savedRefresh != null) {
                repository.logout(savedRefresh).fold(
                    onSuccess = {
                        clearLocalSession()
                        onComplete?.invoke()
                    },
                    onFailure = { exception ->
                        clearLocalSession()
                        onComplete?.invoke()
                    }
                )
            } else {
                clearLocalSession()
                onComplete?.invoke()
            }
        }
    }

    private suspend fun cancelAllRemindersAndCleanup(context: Context) {
        // Necesitas acceso al ReminderRepository aquí
        val database = AppDatabase.getDatabase(context)
        val repository = ReminderRepository(database.reminderDao())

        Log.d(TAG, "🧹 Limpiando recordatorios del usuario anterior...")

        // Obtener todos los recordatorios
        val allReminders = repository.getLocalReminders()

        // Cancelar alarmas
        allReminders.forEach { reminder ->
            if (reminder.reminder_type == "datetime" || reminder.reminder_type == "both") {
                val days = reminder.days?.split(",") ?: emptyList()
                days.forEachIndexed { index, _ ->
                    val uniqueId = reminder.id * 100 + index
                    cancelAlarm(context, uniqueId)
                }
            }
        }

        // Detener servicio de ubicación
        UnifiedLocationService.stop(context)

        // Limpiar base de datos
        repository.clearAllReminders()

        Log.d(TAG, "✅ Limpieza completada")
    }

    private fun cancelAlarm(context: Context, reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // 🔹 Helper para limpiar sesión local
    private fun clearLocalSession() {
        user = null
        accessToken = null
        isLoggedIn = false
        isLoading = false
        errorMessage = null
        sessionManager.clear()
    }

    // 🔹 Auto refresh actualizado
    // 🔹 Auto refresh mejorado con reintentos
    private fun startAutoRefresh() {
        viewModelScope.launch {
            var consecutiveFailures = 0
            val MAX_FAILURES = 3  // Permitir 3 fallos seguidos antes de logout

            while (isActive) {
                delay(5 * 60 * 1000) // 5 minutos
                val savedRefresh = sessionManager.getRefreshToken()

                if (savedRefresh != null && isLoggedIn) {
                    Log.d(TAG, "🔄 Iniciando auto-refresh del token...")

                    repository.refreshToken(savedRefresh).fold(
                        onSuccess = { response ->
                            Log.d(TAG, "✅ Token renovado exitosamente")
                            consecutiveFailures = 0  // ✅ Resetear contador

                            accessToken = response.accessToken
                            sessionManager.saveTokens(response.accessToken, response.refreshToken)

                            Log.d(TAG, "📢 Token guardado, listeners notificados")
                        },
                        onFailure = { error ->
                            consecutiveFailures++

                            Log.e(TAG, "❌ Error en auto-refresh: ${error.message}")
                            Log.w(TAG, "⚠️ Fallo ${consecutiveFailures}/$MAX_FAILURES")

                            // Solo hacer logout si es un error de autenticación O muchos fallos seguidos
                            val isAuthError = error.message?.contains("401") == true ||
                                    error.message?.contains("REFRESH_INVALIDO") == true ||
                                    error.message?.contains("REFRESH_EXPIRADO") == true

                            if (isAuthError) {
                                Log.e(TAG, "🚨 Error de autenticación - Logout inmediato")
                                logout(context, shouldRemoveFCMToken = false)
                            } else if (consecutiveFailures >= MAX_FAILURES) {
                                Log.e(TAG, "🚨 Demasiados fallos consecutivos - Logout")
                                logout(context, shouldRemoveFCMToken = false)
                            } else {
                                Log.w(TAG, "⏳ Error de red - Reintentando en el próximo ciclo")
                            }
                        }
                    )
                } else {
                    Log.w(TAG, "⚠️ No hay refresh token o usuario no logueado")
                }
            }
        }
    }

    fun obtenerIp(): String {
        return try {
            val en = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in en) {
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            "Desconocida"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Desconocida"
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    fun clearError() {
        errorMessage = null
    }

    class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun registerUser(nombre: String, apellido: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null // Limpiar errores anteriores

            repository.register(nombre, apellido, email, password).fold(
                onSuccess = {
                    isLoading = false
                    onResult(true)
                },
                onFailure = { exception ->
                    isLoading = false

                    // Manejar diferentes tipos de errores del backend
                    errorMessage = when {
                        exception.message?.contains("USER_ALREADY_EXISTS") == true -> {
                            "Ya existe una cuenta con este correo electrónico"
                        }
                        exception.message?.contains("INVALID_EMAIL") == true -> {
                            "El formato del correo electrónico no es válido"
                        }
                        exception.message?.contains("WEAK_PASSWORD") == true -> {
                            "La contraseña debe ser más fuerte"
                        }
                        exception.message?.contains("NETWORK_ERROR") == true -> {
                            "Error de conexión. Verifica tu internet"
                        }
                        exception.message?.contains("SERVER_ERROR") == true -> {
                            "Error del servidor. Inténtalo más tarde"
                        }
                        exception.message?.contains("400") == true -> {
                            "Datos inválidos. Verifica la información ingresada"
                        }
                        exception.message?.contains("500") == true -> {
                            "Error interno del servidor. Inténtalo más tarde"
                        }
                        else -> {
                            // Mensaje genérico amigable
                            "Error al crear la cuenta. Inténtalo nuevamente"
                        }
                    }

                    onResult(false)
                }
            )
        }
    }


    /**
     * 🔥 Obtener y enviar token FCM al backend
     */
    private fun obtenerYEnviarTokenFCM() {
        Log.d(TAG, "🔥 Solicitando token FCM a Firebase...")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "❌ ========================================")
                Log.e(TAG, "❌ ERROR AL OBTENER TOKEN FCM")
                Log.e(TAG, "❌ ${task.exception?.message}")
                Log.e(TAG, "❌ ========================================")
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "✅ ========================================")
            Log.d(TAG, "✅ TOKEN FCM OBTENIDO DE FIREBASE")
            Log.d(TAG, "✅ Token: ${token.take(30)}...")
            Log.d(TAG, "✅ Token completo: $token")
            Log.d(TAG, "✅ ========================================")

            // Enviar al backend
            enviarTokenFCM(token)
        }
    }

    /**
     * 📤 Enviar token FCM al backend
     */
    private fun enviarTokenFCM(fcmToken: String) {
        viewModelScope.launch {
            try {
                val bearerToken = accessToken

                if (bearerToken.isNullOrEmpty()) {
                    Log.e(TAG, "❌ ========================================")
                    Log.e(TAG, "❌ NO HAY ACCESS TOKEN DISPONIBLE")
                    Log.e(TAG, "❌ No se puede enviar el token FCM")
                    Log.e(TAG, "❌ ========================================")
                    return@launch
                }

                Log.d(TAG, "🔥 ========================================")
                Log.d(TAG, "🔥 ENVIANDO TOKEN FCM AL BACKEND")
                Log.d(TAG, "🔥 ========================================")
                Log.d(TAG, "   Token FCM: ${fcmToken.take(30)}...")
                Log.d(TAG, "   Access Token: ${bearerToken.take(20)}...")

                val request = mapOf(
                    "token" to fcmToken,
                    "dispositivo" to "android"
                )

                val response = repository.enviarTokenFCM("Bearer $bearerToken", request)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ ========================================")
                    Log.d(TAG, "✅ TOKEN FCM REGISTRADO EN BACKEND")
                    Log.d(TAG, "✅ Código HTTP: ${response.code()}")
                    Log.d(TAG, "✅ Usuario ahora puede recibir notificaciones")
                    Log.d(TAG, "✅ ========================================")

                    // ✅ NUEVO: Verificar en la BD que se guardó
                    Log.d(TAG, "🔍 Verifica en la BD que el token esté guardado:")
                    Log.d(TAG, "   SELECT * FROM fcm_tokens WHERE token = '${fcmToken.take(30)}...'")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ ========================================")
                    Log.e(TAG, "❌ ERROR AL REGISTRAR TOKEN FCM")
                    Log.e(TAG, "❌ Código HTTP: ${response.code()}")
                    Log.e(TAG, "❌ Error: $errorBody")
                    Log.e(TAG, "❌ ========================================")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ ========================================")
                Log.e(TAG, "❌ EXCEPCIÓN AL ENVIAR TOKEN FCM")
                Log.e(TAG, "❌ ${e.message}")
                Log.e(TAG, "❌ ========================================")
                e.printStackTrace()
            }
        }
    }

    private fun enviarTokenFCMPendiente() {
        Log.d(TAG, "🔥 ========================================")
        Log.d(TAG, "🔥 INICIANDO PROCESO FCM POST-LOGIN")
        Log.d(TAG, "🔥 ========================================")

        val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        val pendingToken = prefs.getString("PENDING_FCM_TOKEN", null)

        if (pendingToken != null) {
            Log.d(TAG, "📤 Token FCM pendiente encontrado:")
            Log.d(TAG, "   Token: ${pendingToken.take(30)}...")
            Log.d(TAG, "📤 Enviando al backend...")

            // ✅ Enviar token pendiente
            enviarTokenFCM(pendingToken)

            // ✅ Limpiar token pendiente DESPUÉS de enviarlo
            prefs.edit().remove("PENDING_FCM_TOKEN").apply()
            Log.d(TAG, "🧹 Token pendiente eliminado de SharedPreferences")
        } else {
            Log.d(TAG, "ℹ️ No hay token FCM pendiente en SharedPreferences")
            Log.d(TAG, "🔍 Obteniendo nuevo token de Firebase...")

            // ✅ Si no hay token pendiente, obtener uno nuevo
            obtenerYEnviarTokenFCM()
        }

        Log.d(TAG, "🔥 ========================================")
    }

}