package com.rutai.app.viewmodel

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
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
import com.rutai.app.BaseViewModel
import com.rutai.app.models.Reminder
import com.rutai.app.models.ReminderEntity
import com.rutai.app.models.User
import com.rutai.app.models.toReminder
import com.rutai.app.network.AppDatabase
import com.rutai.app.network.RetrofitClient
import com.rutai.app.repository.AuthRepository
import com.rutai.app.repository.LoginState
import com.rutai.app.repository.ReminderRepository
import com.rutai.app.screen.recordatorios.components.ReminderReceiver
import com.rutai.app.screen.recordatorios.components.scheduleReminder
import com.rutai.app.services.UnifiedLocationService
import com.rutai.app.utils.PermissionUtils
import com.rutai.app.utils.SessionManager
import com.rutai.app.utils.BackendErrorMapper
import com.google.firebase.messaging.FirebaseMessaging
import com.rutai.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class AuthViewModel(context: Context) : BaseViewModel(context, SessionManager.getInstance(context)) {
    private val repository = AuthRepository()

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.rutai.app.FORCE_LOGOUT") {
                Log.w(TAG, "📡 Broadcast de logout recibido en AuthViewModel")
                clearLocalSession()
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
        private const val PREF_PENDING_TRACKING = "pending_tracking_start"
    }

    var isRestoringSession by mutableStateOf(true)
        private set
    
    var user by mutableStateOf<User?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set
    var accessToken by mutableStateOf<String?>(null)
        private set

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    init {
        val filter = IntentFilter("com.rutai.app.FORCE_LOGOUT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(logoutReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(logoutReceiver, filter)
        }

        restoreStateFromPrefs()
        restoreSession()
    }

    private fun restoreStateFromPrefs() {
        isLoggedIn = sessionManager.isLoggedIn()
        accessToken = sessionManager.getAccessToken()
        user = sessionManager.getUser()
    }

    private fun restoreSession() {
        val savedRefresh = sessionManager.getRefreshToken()
        if (savedRefresh != null && sessionManager.hasValidSession()) {
            isRestoringSession = true
            safeApiCallNoToken(
                call = { repository.refreshToken(savedRefresh) },
                onSuccess = { response ->
                    accessToken = response.accessToken
                    isLoggedIn = true
                    sessionManager.saveTokens(response.accessToken, response.refreshToken)
                    sessionManager.saveLoginState(true)

                    getCurrentUser {
                        restoreUserReminders(context, response.accessToken)
                        iniciarTrackingPasivoDespuesDeLogin()
                        isRestoringSession = false
                    }
                },
                onError = { errorMsg ->
                    val isNetworkError = errorMsg.contains("NETWORK_ERROR") ||
                            errorMsg.contains("TIMEOUT") ||
                            errorMsg.contains("NO_INTERNET") ||
                            errorMsg.contains("500")

                    if (isNetworkError) {
                        Log.w(TAG, "⚠️ Error de red en restoreSession, manteniendo sesión local")
                        isLoggedIn = true
                        iniciarTrackingPasivoDespuesDeLogin()
                        isRestoringSession = false
                    } else {
                        Log.e(TAG, "🚨 Error de autenticación en restoreSession, cerrando sesión")
                        clearLocalSession()
                    }
                }
            )
        } else {
            clearLocalSession()
        }
    }

    fun actualizarPerfil(nombre: String, apellido: String) {
        safeApiCall(
            call = { token -> repository.actualizarPerfil(token, nombre, apellido) },
            onSuccess = { profileResponse ->
                user = user?.copy(
                    nombre = profileResponse.nombre,
                    apellido = profileResponse.apellido
                )
                user?.let { sessionManager.saveUser(it) }
                Log.d(TAG, "✅ Perfil actualizado")
            }
        )
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.login(email, password, Build.MODEL, getAppVersion(context), obtenerIp())
                .collect { state ->
                    _loginState.value = state
                    when (state) {
                        is LoginState.Loading, is LoginState.Retrying -> {
                            // isLoading se maneja internamente en el BaseViewModel para safeApiCall
                            // pero login usa un Flow, así que lo manejamos aquí manualmente si es necesario
                            // o dejamos que el estado del Flow controle la UI
                        }
                        is LoginState.Success -> {
                            accessToken = state.data.accessToken
                            isLoggedIn = true
                            sessionManager.saveTokens(state.data.accessToken, state.data.refreshToken)
                            sessionManager.saveLoginState(true)

                            getCurrentUser {
                                restoreUserReminders(context, state.data.accessToken)
                                enviarTokenFCMPendiente()
                                iniciarTrackingPasivoDespuesDeLogin()
                                onResult(true)
                            }
                        }
                        is LoginState.Error -> {
                            isLoggedIn = false
                            sessionManager.saveLoginState(false)
                            errorMessage = BackendErrorMapper.resolve(context, state.message)
                            onResult(false)
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun iniciarTrackingPasivoDespuesDeLogin() {
        if (hasLocationPermissions()) {
            startTrackingService()
        } else {
            val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_PENDING_TRACKING, true).apply()
        }
    }

    fun reiniciarTrackingSiPendiente() {
        val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(PREF_PENDING_TRACKING, false)

        if (pending && hasLocationPermissions()) {
            startTrackingService()
            prefs.edit().putBoolean(PREF_PENDING_TRACKING, false).apply()
        }
    }

    private fun startTrackingService() {
        try {
            val intent = Intent(context, UnifiedLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando servicio: ${e.message}")
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineLocation && coarseLocation
    }

    private fun restoreUserReminders(context: Context, token: String) {
        viewModelScope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = ReminderRepository(database.reminderDao())
                val response = RetrofitClient.reminderService.getReminders("Bearer $token")

                if (response.isSuccessful) {
                    val apiReminders = response.body() ?: emptyList()
                    apiReminders.forEach { reminderResponse ->
                        val reminder = reminderResponse.toReminder()
                        val reminderEntity = ReminderEntity(
                            id = reminderResponse.id,
                            title = reminderResponse.title,
                            description = reminderResponse.description,
                            reminder_type = reminderResponse.reminder_type,
                            trigger_type = reminderResponse.trigger_type,
                            sound_uri = reminderResponse.sound_uri,
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

                        if (reminderEntity.is_active && !reminderEntity.is_deleted) {
                            when (reminderEntity.reminder_type) {
                                "datetime" -> reprogramarAlarmasFechaHora(context, reminder, reminderEntity)
                                "location", "both" -> {
                                    if (PermissionUtils.hasLocationPermissions(context)) {
                                        UnifiedLocationService.start(context)
                                    }
                                }
                            }
                            if (reminderEntity.reminder_type == "both") {
                                reprogramarAlarmasFechaHora(context, reminder, reminderEntity)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error restaurando recordatorios: ${e.message}")
            }
        }
    }

    private fun reprogramarAlarmasFechaHora(context: Context, reminder: Reminder, reminderEntity: ReminderEntity) {
        if (reminder.days.isNullOrEmpty() || reminder.time.isNullOrEmpty()) return
        reminder.days.forEachIndexed { index, day ->
            val uniqueId = reminderEntity.id * 100 + index
            val singleDayReminder = reminderEntity.copy(id = uniqueId, days = day)
            scheduleReminder(context, singleDayReminder)
        }
    }

    fun getCurrentUser(onSuccess: () -> Unit = {}) {
        safeApiCall(
            call = { token -> repository.getCurrentUser("Bearer $token") },
            onSuccess = { currentUser ->
                user = currentUser
                sessionManager.saveUser(currentUser)
                errorMessage = null
                onSuccess()
            },
            onError = { error ->
                user = null
                isLoggedIn = false
                accessToken = null
                sessionManager.saveLoginState(false)
                isRestoringSession = false
                errorMessage = error
            }
        )
    }

    fun logout(context: Context, shouldRemoveFCMToken: Boolean = true, onComplete: (() -> Unit)? = null) {
        val savedRefresh = sessionManager.getRefreshToken()
        viewModelScope.launch {
            if (shouldRemoveFCMToken) {
                accessToken?.let { token ->
                    repository.eliminarTokenFCM("Bearer $token")
                }
            }
            try { cancelAllRemindersAndCleanup(context) } catch (e: Exception) {}

            if (savedRefresh != null) {
                repository.logout(savedRefresh).fold(
                    onSuccess = { clearLocalSession(); onComplete?.invoke() },
                    onFailure = { clearLocalSession(); onComplete?.invoke() }
                )
            } else {
                clearLocalSession(); onComplete?.invoke()
            }
        }
    }

    private suspend fun cancelAllRemindersAndCleanup(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val repository = ReminderRepository(database.reminderDao())
        val allReminders = repository.getLocalReminders()
        allReminders.forEach { reminder ->
            if (reminder.reminder_type == "datetime" || reminder.reminder_type == "both") {
                val days = reminder.days?.split(",") ?: emptyList()
                days.forEachIndexed { index, _ -> cancelAlarm(context, reminder.id * 100 + index) }
            }
        }
        UnifiedLocationService.stop(context)
        repository.clearAllReminders()
    }

    private fun cancelAlarm(context: Context, reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun clearLocalSession() {
        user = null
        accessToken = null
        isLoggedIn = false
        isRestoringSession = false
        errorMessage = null
        sessionManager.clear()
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(logoutReceiver) } catch (e: Exception) {}
    }

    fun obtenerIp(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress ?: "Desconocida"
        } catch (e: Exception) { "Desconocida" }
    }

    fun getAppVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Desconocida"
        } catch (e: Exception) { "Desconocida" }
    }

    fun clearError() { errorMessage = null }

    class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) return AuthViewModel(context) as T
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun registerUser(nombre: String, apellido: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        safeApiCall(
            call = { repository.register(nombre, apellido, email, password) },
            onSuccess = { onResult(true) },
            onError = { errorMessage = it; onResult(false) }
        )
    }

    private fun enviarTokenFCM(fcmToken: String) {
        safeApiCall(
            call = { token -> repository.enviarTokenFCM("Bearer $token", mapOf("token" to fcmToken, "dispositivo" to "android")) },
            onSuccess = { Log.d(TAG, "✅ TOKEN FCM REGISTRADO") },
            onError = { Log.e(TAG, "❌ ERROR FCM: $it") }
        )
    }

    private fun enviarTokenFCMPendiente() {
        val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)
        val pendingToken = prefs.getString("PENDING_FCM_TOKEN", null)
        if (pendingToken != null) {
            enviarTokenFCM(pendingToken)
            prefs.edit().remove("PENDING_FCM_TOKEN").apply()
        } else {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) enviarTokenFCM(task.result)
            }
        }
    }

    fun verificarYRefrescarToken() {
        val savedRefresh = sessionManager.getRefreshToken() ?: return
        if (!isLoggedIn) return
        viewModelScope.launch {
            repository.refreshToken(savedRefresh).fold(
                onSuccess = { response ->
                    accessToken = response.accessToken
                    sessionManager.saveTokens(response.accessToken, response.refreshToken)
                },
                onFailure = { error ->
                    if (error.message?.contains("401") == true) logout(context, shouldRemoveFCMToken = false)
                }
            )
        }
    }
}
