package mx.uasnack.app.data.red

import java.io.IOException

// Error de la API ya traducido, listo para que el repositorio o el ViewModel decidan qué mostrar.
//
// Hereda de IOException a propósito: OkHttp solo deja pasar IOException desde un interceptor.
// Cualquier otra excepción lanzada ahí tumba la app en el hilo de OkHttp en lugar de llegar a la corrutina.
class ErrorApi(
    // Código HTTP (401, 422, 429...), o 0 si no hubo respuesta (sin conexión)
    val status: Int,
    // Código de la API: CREDENCIALES_INVALIDAS, VALIDACION, DEMASIADOS_INTENTOS...
    val codigo: String,
    // Texto para mostrar tal cual al usuario
    val mensaje: String,
    // Solo en 422: mensajes por campo, por ejemplo {"password": ["..."]}
    val campos: Map<String, List<String>> = emptyMap(),
    // Segundos del header Retry-After (429 y reenvío de código), o null si no vino
    val reintentarEn: Int? = null,
    // Error original de OkHttp cuando no hubo respuesta (sin conexión). No se muestra al usuario,
    // pero queda en el log para saber qué falló de verdad (DNS, tiempo agotado, HTTP bloqueado...)
    causa: Throwable? = null
) : IOException(mensaje, causa) {

    // Primer mensaje de cada campo, que es lo que se muestra debajo de cada input
    val primerErrorPorCampo: Map<String, String>
        get() = campos.mapNotNull { (campo, mensajes) -> mensajes.firstOrNull()?.let { campo to it } }.toMap()

    companion object {
        // Códigos que manda el backend (bootstrap/app.php y AuthService)
        const val VALIDACION = "VALIDACION"
        const val CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS"
        const val CORREO_NO_VERIFICADO = "CORREO_NO_VERIFICADO"
        const val DEMASIADOS_INTENTOS = "DEMASIADOS_INTENTOS"
        const val NO_AUTENTICADO = "NO_AUTENTICADO"

        // Códigos que pone la app cuando no hay respuesta del servidor que leer
        const val SIN_CONEXION = "SIN_CONEXION"
        const val ERROR_DESCONOCIDO = "ERROR_DESCONOCIDO"

        // Los mismos textos que la PWA (shared/api/cliente.js)
        const val MENSAJE_SIN_CONEXION = "No hay conexión. Revisa tu internet e intenta de nuevo."
        const val MENSAJE_DESCONOCIDO = "Ocurrió un error inesperado."
    }
}

// Convierte cualquier error en ErrorApi. Los repositorios ya regresan ErrorApi, pero si algo raro
// se cuela (por ejemplo una respuesta que no se pudo leer), la pantalla muestra el mensaje genérico
// en lugar de no mostrar nada.
fun Throwable.aErrorApi(): ErrorApi =
    this as? ErrorApi ?: ErrorApi(
        status = 0,
        codigo = ErrorApi.ERROR_DESCONOCIDO,
        mensaje = ErrorApi.MENSAJE_DESCONOCIDO,
        causa = this
    )
