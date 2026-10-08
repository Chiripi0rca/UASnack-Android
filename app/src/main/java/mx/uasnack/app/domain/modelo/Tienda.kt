package mx.uasnack.app.domain.modelo

// Tienda tal como la usa la interfaz (#228), sin anotaciones de red ni de base de datos.
//
// El alumno ve las tiendas de TODAS las facultades; las de la suya llegan primero.
// esMiFacultad sirve para separarlas en pantalla en "Tu facultad" y "Otras facultades".
data class Tienda(
    val id: Int,
    val nombre: String,
    val ubicacion: String?,
    val horarioApertura: String?,
    val horarioCierre: String?,
    // Cerrada = se muestra atenuada y no se puede tocar
    val abierta: Boolean,
    val esMiFacultad: Boolean
)
