import com.example.app.models.UbicacionUsuarioResponse
import org.osmdroid.util.GeoPoint
import kotlin.math.*

fun calcularDistanciaSobreRuta(userLat: Double, userLon: Double, routePoints: List<GeoPoint>): Double {
    if (routePoints.isEmpty()) return 0.0
    if (routePoints.size == 1) {
        return calcularDistancia(userLat, userLon, routePoints[0].latitude, routePoints[0].longitude)
    }

    val userPoint = GeoPoint(userLat, userLon)

    // Encontrar el segmento de ruta más cercano al usuario
    var closestSegmentIndex = 0
    var minDistToSegment = Double.MAX_VALUE
    var closestPointOnSegment: GeoPoint? = null

    for (i in 0 until routePoints.size - 1) {
        val p1 = routePoints[i]
        val p2 = routePoints[i + 1]

        // Encontrar el punto más cercano en este segmento de línea
        val closestPoint = findClosestPointOnSegment(userPoint, p1, p2)
        val distToSegment = calcularDistancia(
            userLat, userLon,
            closestPoint.latitude, closestPoint.longitude
        )

        if (distToSegment < minDistToSegment) {
            minDistToSegment = distToSegment
            closestSegmentIndex = i
            closestPointOnSegment = closestPoint
        }
    }

    // Calcular distancia restante desde el punto más cercano en el segmento
    var distanciaRestante = 0.0

    closestPointOnSegment?.let { puntoMasCercano ->
        // Distancia desde el punto más cercano al final del segmento actual
        distanciaRestante += calcularDistancia(
            puntoMasCercano.latitude, puntoMasCercano.longitude,
            routePoints[closestSegmentIndex + 1].latitude, routePoints[closestSegmentIndex + 1].longitude
        )

        // Sumar distancia de todos los segmentos restantes
        for (i in (closestSegmentIndex + 1) until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]
            distanciaRestante += calcularDistancia(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }
    }

    return distanciaRestante
}

fun findClosestPointOnSegment(userPoint: GeoPoint, p1: GeoPoint, p2: GeoPoint): GeoPoint {
    val A = userPoint.latitude - p1.latitude
    val B = userPoint.longitude - p1.longitude
    val C = p2.latitude - p1.latitude
    val D = p2.longitude - p1.longitude

    val dot = A * C + B * D
    val lenSq = C * C + D * D

    val param = if (lenSq != 0.0) dot / lenSq else -1.0

    return when {
        param < 0 -> p1 // Más cercano al punto inicial
        param > 1 -> p2 // Más cercano al punto final
        else -> GeoPoint(
            p1.latitude + param * C,
            p1.longitude + param * D
        ) // Punto en el segmento
    }
}

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // Radio de la tierra en metros
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}