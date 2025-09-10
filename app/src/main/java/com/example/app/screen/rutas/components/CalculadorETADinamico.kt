package com.example.app.screen.rutas.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import calcularDistancia
import calcularDistanciaSobreRuta
import org.osmdroid.util.GeoPoint

// 1. NUEVA DATA CLASS para manejar el estado de la ruta
data class RutaEstado(
    val rutaOriginal: List<GeoPoint> = emptyList(),
    val distanciaOriginalMetros: Double = 0.0,
    val duracionOriginalSegundos: Double = 0.0,
    val tiempoInicioRuta: Long = 0L,
    val velocidadPromedio: Double = 0.0, // m/s
    val velocidadActual: Double = 0.0,
    val ultimasPosiciones: List<Pair<Double, Double>> = emptyList(), // Para calcular velocidad
    val ultimosTiempos: List<Long> = emptyList(),
    val activa: Boolean = false
)

// 2. NUEVO COMPOSABLE para calcular ETA dinámico
@Composable
fun CalculadorETADinamico(
    userLat: Double,
    userLon: Double,
    rutaEstado: RutaEstado,
    transportMode: String,
    onETACalculado: (distancia: String, duracion: String) -> Unit
) {
    LaunchedEffect(userLat, userLon, rutaEstado.activa) {
        if (!rutaEstado.activa || rutaEstado.rutaOriginal.isEmpty()) return@LaunchedEffect

        // Calcular distancia restante más precisa
        val distanciaRestanteMetros = calcularDistanciaSobreRuta(
            userLat, userLon, rutaEstado.rutaOriginal
        )

        val tiempoActual = System.currentTimeMillis() / 1000
        val tiempoTranscurrido = tiempoActual - rutaEstado.tiempoInicioRuta

        // Calcular velocidad actual basada en las últimas posiciones
        val velocidadActualCalculada = calcularVelocidadActual(
            rutaEstado.ultimasPosiciones,
            rutaEstado.ultimosTiempos
        )

        // Obtener velocidades típicas según el modo de transporte
        val velocidadesModales = obtenerVelocidadesModales(transportMode)
        val velocidadMinima = velocidadesModales.first
        val velocidadMaxima = velocidadesModales.second
        val velocidadPromedio = velocidadesModales.third

        // Calcular ETA usando algoritmo similar a Google Maps
        val duracionEstimada = when {
            // Primeros 30 segundos: usar estimación original
            tiempoTranscurrido < 30 -> {
                val proporcionDistancia = distanciaRestanteMetros / rutaEstado.distanciaOriginalMetros
                rutaEstado.duracionOriginalSegundos * proporcionDistancia
            }

            // Si tenemos velocidad actual válida
            velocidadActualCalculada > velocidadMinima -> {
                // Usar velocidad actual pero con suavizado
                val velocidadSuavizada = suavizarVelocidad(
                    velocidadActualCalculada,
                    velocidadPromedio,
                    velocidadMinima,
                    velocidadMaxima
                )
                distanciaRestanteMetros / velocidadSuavizada
            }

            // Si la velocidad es muy baja o cero (parado, tráfico)
            velocidadActualCalculada <= velocidadMinima -> {
                // Aumentar tiempo estimado gradualmente
                val factorRetraso = when {
                    tiempoTranscurrido < 60 -> 1.1 // +10% en primer minuto
                    tiempoTranscurrido < 300 -> 1.3 // +30% en primeros 5 min
                    else -> 1.5 // +50% después de 5 min parado
                }
                (distanciaRestanteMetros / velocidadPromedio) * factorRetraso
            }

            // Fallback: usar velocidad promedio del modo de transporte
            else -> distanciaRestanteMetros / velocidadPromedio
        }

        // Formatear resultados
        val distanciaKm = distanciaRestanteMetros / 1000.0
        val duracionMinutos = (duracionEstimada / 60.0).toInt()

        val distanciaTexto = when {
            distanciaRestanteMetros < 50 -> "Llegando..."
            distanciaKm < 1.0 -> String.format("%.0f m", distanciaRestanteMetros)
            else -> String.format("%.2f km", distanciaKm)
        }

        val duracionTexto = when {
            distanciaRestanteMetros < 50 -> "Llegando..."
            duracionMinutos < 1 -> "< 1 min"
            duracionMinutos < 60 -> "${duracionMinutos} min"
            else -> {
                val horas = duracionMinutos / 60
                val minutos = duracionMinutos % 60
                "${horas}h ${minutos}min"
            }
        }

        onETACalculado(distanciaTexto, duracionTexto)
    }
}

// 3. FUNCIÓN para calcular velocidad actual basada en posiciones recientes
fun calcularVelocidadActual(
    ultimasPosiciones: List<Pair<Double, Double>>,
    ultimosTiempos: List<Long>
): Double {
    if (ultimasPosiciones.size < 2 || ultimosTiempos.size < 2) return 0.0

    // Usar las últimas 5 posiciones para suavizar la velocidad
    val posicionesRecientes = ultimasPosiciones.takeLast(5)
    val tiemposRecientes = ultimosTiempos.takeLast(5)

    if (posicionesRecientes.size < 2) return 0.0

    val distanciaTotal = calcularDistanciaTotal(posicionesRecientes)
    val tiempoTotal = (tiemposRecientes.last() - tiemposRecientes.first()).toDouble()

    return if (tiempoTotal > 0) distanciaTotal / tiempoTotal else 0.0
}

// 4. FUNCIÓN para calcular distancia total entre puntos
fun calcularDistanciaTotal(posiciones: List<Pair<Double, Double>>): Double {
    if (posiciones.size < 2) return 0.0

    var distanciaTotal = 0.0
    for (i in 1 until posiciones.size) {
        val pos1 = posiciones[i - 1]
        val pos2 = posiciones[i]
        distanciaTotal += calcularDistancia(pos1.first, pos1.second, pos2.first, pos2.second)
    }
    return distanciaTotal
}

// 5. FUNCIÓN para obtener velocidades típicas por modo de transporte
fun obtenerVelocidadesModales(transportMode: String): Triple<Double, Double, Double> {
    // Retorna: (velocidadMinima, velocidadMaxima, velocidadPromedio) en m/s
    return when (transportMode) {
        "foot-walking" -> Triple(0.5, 2.5, 1.4) // 1.8-9 km/h, promedio 5 km/h
        "cycling-regular" -> Triple(2.0, 8.0, 4.2) // 7-29 km/h, promedio 15 km/h
        "driving-car" -> Triple(5.0, 25.0, 13.9) // 18-90 km/h, promedio 50 km/h
        else -> Triple(0.5, 2.5, 1.4) // Default walking
    }
}

// 6. FUNCIÓN para suavizar velocidad y evitar cambios bruscos
fun suavizarVelocidad(
    velocidadActual: Double,
    velocidadPromedio: Double,
    velocidadMinima: Double,
    velocidadMaxima: Double
): Double {
    // Aplicar límites
    val velocidadLimitada = velocidadActual.coerceIn(velocidadMinima, velocidadMaxima)

    // Suavizar con promedio ponderado (70% actual, 30% promedio modal)
    return (velocidadLimitada * 0.7) + (velocidadPromedio * 0.3)
}

// 7. FUNCIÓN para actualizar historial de posiciones
fun actualizarHistorialPosiciones(
    latitud: Double,
    longitud: Double,
    rutaEstado: RutaEstado
): RutaEstado {
    val tiempoActual = System.currentTimeMillis() / 1000
    val nuevasPosiciones = (rutaEstado.ultimasPosiciones + Pair(latitud, longitud)).takeLast(10)
    val nuevosTiempos = (rutaEstado.ultimosTiempos + tiempoActual).takeLast(10)

    return rutaEstado.copy(
        ultimasPosiciones = nuevasPosiciones,
        ultimosTiempos = nuevosTiempos
    )
}

// 8. NUEVA FUNCIÓN para inicializar estado de ruta
fun inicializarRutaEstado(
    rutaOriginal: List<GeoPoint>,
    distanciaMetros: Double,
    duracionSegundos: Double
): RutaEstado {
    return RutaEstado(
        rutaOriginal = rutaOriginal,
        distanciaOriginalMetros = distanciaMetros,
        duracionOriginalSegundos = duracionSegundos,
        tiempoInicioRuta = System.currentTimeMillis() / 1000,
        activa = true,
        ultimasPosiciones = emptyList(),
        ultimosTiempos = emptyList()
    )
}