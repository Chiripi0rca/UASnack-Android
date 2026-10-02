package mx.uasnack.app.domain.modelo

// Usuario tal como lo usa la interfaz, sin anotaciones de red.
data class Usuario(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val correoInstitucional: String,
    val idFacultad: Int?,
    val rol: Rol,
    val reputacion: Int,
    val correoVerificado: Boolean
)

// Roles del backend (app/Enums/RolUsuario.php). La app Android solo es para el rol USUARIO:
// el encargado y el administrador usan el panel web.
enum class Rol {
    USUARIO,
    ENCARGADO_TIENDA,
    ADMINISTRADOR,
    // Por si el backend agrega un rol que esta versión de la app no conoce
    DESCONOCIDO
}
