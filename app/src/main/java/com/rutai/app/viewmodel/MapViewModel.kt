package com.rutai.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.rutai.app.BaseViewModel
import com.rutai.app.models.*
import com.rutai.app.repository.RutasRepository
import com.rutai.app.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class MapViewModel(
    context: Context,
    private val rutasRepository: RutasRepository
) : BaseViewModel(context, SessionManager.getInstance(context)) {

    private val _route: MutableState<DirectionsResponse?> = mutableStateOf(null)
    val route: State<DirectionsResponse?> = _route

    var currentMode: String = "walking"
    var currentMLType: String? = null

    private val _rutaIdActiva = mutableStateOf<Int?>(null)
    val rutaIdActiva: State<Int?> = _rutaIdActiva

    private val _mostrarOpcionesFinalizar = mutableStateOf(false)
    val mostrarOpcionesFinalizar: State<Boolean> = _mostrarOpcionesFinalizar

    var rutaActualUbicacionId: Int? = null
    var rutaActualDistancia: Double? = null
    var rutaActualDuracion: Double? = null

    private val _puntosGPSReales = mutableListOf<PuntoGPS>()

    private val _mostrarAlertaDesobediencia = mutableStateOf(false)
    val mostrarAlertaDesobediencia: State<Boolean> = _mostrarAlertaDesobediencia

    private val _mensajeAlertaDesobediencia = mutableStateOf<String?>(null)
    val mensajeAlertaDesobediencia: State<String?> = _mensajeAlertaDesobediencia

    private val _alternativeRoutes = mutableStateOf<List<RouteAlternative>>(emptyList())
    val alternativeRoutes: State<List<RouteAlternative>> = _alternativeRoutes

    private val _showRouteSelector = mutableStateOf(false)
    val showRouteSelector: State<Boolean> = _showRouteSelector

    private val _validacionSeguridad = mutableStateOf<ValidarRutasResponse?>(null)
    val validacionSeguridad: State<ValidarRutasResponse?> = _validacionSeguridad

    private val _mostrarAdvertenciaSeguridad = mutableStateOf(false)
    val mostrarAdvertenciaSeguridad: State<Boolean> = _mostrarAdvertenciaSeguridad

    private val _rutaSeleccionadaPendiente = mutableStateOf<RouteAlternative?>(null)

    private val _isRegeneratingRoutes = mutableStateOf(false)
    val isRegeneratingRoutes: State<Boolean> = _isRegeneratingRoutes

    private val _rutasGeneradasEvitandoZonas = mutableStateOf(false)
    val rutasGeneradasEvitandoZonas: State<Boolean> = _rutasGeneradasEvitandoZonas

    private val _zonasPeligrosas = mutableStateOf<List<ZonaPeligrosaResponse>>(emptyList())
    val zonasPeligrosas: State<List<ZonaPeligrosaResponse>> = _zonasPeligrosas

    private val _mostrarZonasPeligrosas = mutableStateOf(true)
    val mostrarZonasPeligrosas: State<Boolean> = _mostrarZonasPeligrosas

    private val _cargandoZonas = mutableStateOf(false)
    val cargandoZonas: State<Boolean> = _cargandoZonas

    fun cargarZonasPeligrosas() {
        _cargandoZonas.value = true
        safeApiCall(
            call = { token -> rutasRepository.obtenerMisZonasPeligrosas(token) },
            onSuccess = { zonas ->
                _zonasPeligrosas.value = zonas
                _cargandoZonas.value = false
            },
            onError = {
                _cargandoZonas.value = false
                Log.e("MapViewModel", "Error cargando zonas: $it")
            }
        )
    }

    fun toggleMostrarZonas() {
        _mostrarZonasPeligrosas.value = !_mostrarZonasPeligrosas.value
    }

    fun getColorForDangerLevel(level: Int, isPublic: Boolean = false): Int {
        if (isPublic) return 0x669C27B0.toInt() // Púrpura para públicas
        return when (level) {
            1 -> 0x66FFEB3B.toInt() // Amarillo
            2 -> 0x66FF9800.toInt() // Naranja
            3 -> 0x66F44336.toInt() // Rojo
            else -> 0x669E9E9E.toInt() // Gris
        }
    }

    fun fetchAllRouteAlternatives(
        origin: Pair<Double, Double>,
        destination: Pair<Double, Double>,
        mode: String,
        ubicacionId: Int,
        mlType: String
    ) {
        currentMode = mode
        rutaActualUbicacionId = ubicacionId
        currentMLType = mlType

        _showRouteSelector.value = true
        _alternativeRoutes.value = emptyList()

        safeApiCall(
            call = { token ->
                rutasRepository.validarRutas(token, ValidarRutasRequest(
                    rutas = emptyList(), // FIXME: In a real app, you'd convert alternatives here
                    ubicacionId = ubicacionId
                ))
            },
            onSuccess = { response ->
                _validacionSeguridad.value = response
                // Mapping validated routes back to alternatives if necessary
                // For now, keeping the list as is or using the validated info
            },
            onError = {
                Log.e("MapViewModel", "Error validando rutas: $it")
            }
        )
    }

    fun selectRouteAlternative(alternative: RouteAlternative, mode: String, ubicacionId: Int, mlType: String) {
        if (alternative.esSegura == false) {
            _rutaSeleccionadaPendiente.value = alternative
            _mostrarAdvertenciaSeguridad.value = true
        } else {
            confirmarSeleccionRuta(alternative, mode, ubicacionId, mlType)
        }
    }

    fun confirmarSeleccionRuta(alternative: RouteAlternative, mode: String, ubicacionId: Int, mlType: String) {
        _route.value = alternative.response
        _showRouteSelector.value = false
        _mostrarAdvertenciaSeguridad.value = false
        _rutaSeleccionadaPendiente.value = null

        guardarRutaEnBackend(alternative.response, mode, ubicacionId, mlType)
    }

    fun aceptarRiesgoRutaInsegura(mode: String, ubicacionId: Int, mlType: String) {
        _rutaSeleccionadaPendiente.value?.let {
            confirmarSeleccionRuta(it, mode, ubicacionId, mlType)
        }
    }

    fun rechazarRutaInsegura() {
        _mostrarAdvertenciaSeguridad.value = false
        _rutaSeleccionadaPendiente.value = null
    }

    fun hideRouteSelector() {
        _showRouteSelector.value = false
    }

    fun agregarPuntoGPSReal(lat: Double, lon: Double) {
        _puntosGPSReales.add(PuntoGPS(lat, lon, System.currentTimeMillis()))
        if (_puntosGPSReales.size % 5 == 0) {
            val (desobedece, similitud) = calcularSimilitudRuta()
            if (desobedece) {
                _mensajeAlertaDesobediencia.value = "Te has desviado de la ruta segura (${(similitud * 100).toInt()}% de similitud)"
                _mostrarAlertaDesobediencia.value = true
            }
        }
    }

    fun setMode(mode: String) {
        currentMode = mode
    }

    fun guardarRutaEnBackend(directions: DirectionsResponse, mode: String, ubicacionId: Int, mlType: String) {
        val ruta = directions.toRutaUsuarioJson(ubicacionId, mode, mlType)

        safeApiCall(
            call = { token -> rutasRepository.guardarRuta(token, ruta) },
            onSuccess = { guardada ->
                _rutaIdActiva.value = guardada.id
                rutaActualDistancia = guardada.distancia_total
                rutaActualDuracion = guardada.duracion_total
            }
        )
    }

    fun clearRoute() {
        _route.value = null
        _rutaIdActiva.value = null
        _puntosGPSReales.clear()
        _mostrarOpcionesFinalizar.value = false
        _alternativeRoutes.value = emptyList()
        _showRouteSelector.value = false
        _rutasGeneradasEvitandoZonas.value = false
    }

    fun calcularSimilitudRuta(): Pair<Boolean, Double> {
        // Implementación simplificada para el ejemplo
        return Pair(false, 1.0)
    }

    fun finalizarRutaBackend() {
        val id = _rutaIdActiva.value ?: return
        val (_, similitud) = calcularSimilitudRuta()
        val fechaFin = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        safeApiCall(
            call = { _ ->
                rutasRepository.finalizarRuta(
                    rutaId = id,
                    fechaFin = fechaFin,
                    puntosGPS = _puntosGPSReales.toList(),
                    siguioRutaRecomendada = similitud > 0.7,
                    porcentajeSimilitud = similitud
                )
            },
            onSuccess = {
                clearRoute()
            }
        )
    }

    fun cancelarRutaBackend() {
        val id = _rutaIdActiva.value ?: return
        val fechaFin = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        safeApiCall(
            call = { _ -> rutasRepository.cancelarRuta(id, fechaFin) },
            onSuccess = {
                clearRoute()
            }
        )
    }

    fun regenerarRutasEvitandoZonasPeligrosas(
        origin: Pair<Double, Double>,
        destination: Pair<Double, Double>,
        mode: String,
        ubicacionId: Int,
        mlType: String
    ) {
        _isRegeneratingRoutes.value = true
        // Simular regeneración usando el mismo endpoint pero con lógica de backend que ya conoce las zonas
        fetchAllRouteAlternatives(origin, destination, mode, ubicacionId, mlType)
        _rutasGeneradasEvitandoZonas.value = true
        _isRegeneratingRoutes.value = false
    }

    fun adoptarZonaPublica(zonaId: Int, onSuccess: () -> Unit) {
        safeApiCall(
            call = { token -> rutasRepository.adoptarZonaSugerida(token, zonaId) },
            onSuccess = {
                cargarZonasPeligrosas()
                onSuccess()
            }
        )
    }

    fun resetRegeneracionZonas() {
        _rutasGeneradasEvitandoZonas.value = false
    }

    fun cerrarAlertaDesobediencia() {
        _mostrarAlertaDesobediencia.value = false
        _mensajeAlertaDesobediencia.value = null
    }

    fun ocultarOpcionesFinalizar() {
        _mostrarOpcionesFinalizar.value = false
    }

    fun revalidarRutasActuales(mode: String, ubicacionId: Int) {
        // Implementación para revalidar
    }

    override fun onCleared() {
        super.onCleared()
    }
}
