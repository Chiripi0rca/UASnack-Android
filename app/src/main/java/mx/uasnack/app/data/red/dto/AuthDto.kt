package mx.uasnack.app.data.red.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTOs de autenticación: la forma exacta del JSON de la API.
// La API viaja en snake_case y Kotlin usa camelCase: la traducción se hace aquí, una sola vez, con @SerialName.

// --- Lo que se envía ---

@Serializable
data class LoginRequest(
    @SerialName("correo_institucional") val correoInstitucional: String,
    val password: String
)

@Serializable
data class RegistroRequest(
    val nombre: String,
    val apellido: String,
    @SerialName("correo_institucional") val correoInstitucional: String,
    @SerialName("id_facultad") val idFacultad: Int?,
    val password: String,
    val telefono: String,
    // El backend exige que llegue en true (regla "accepted")
    @SerialName("acepto_terminos") val aceptoTerminos: Boolean
)

@Serializable
data class VerificarRequest(
    @SerialName("correo_institucional") val correoInstitucional: String,
    val codigo: String
)

@Serializable
data class ReenviarCodigoRequest(
    @SerialName("correo_institucional") val correoInstitucional: String
)

// --- Lo que se recibe ---

// Respuesta de login y de verificar: el token de Sanctum y el usuario
@Serializable
data class SesionDto(
    val token: String,
    val usuario: UsuarioDto
)

// UsuarioResource del backend
@Serializable
data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val apellido: String,
    @SerialName("correo_institucional") val correoInstitucional: String,
    @SerialName("id_facultad") val idFacultad: Int?,
    // "usuario", "encargado_tienda" o "administrador"
    val rol: String,
    val reputacion: Int,
    // Fecha ISO 8601, o null si todavía no verifica su correo
    @SerialName("correo_verificado_en") val correoVerificadoEn: String? = null
)

// Respuesta de registro (201) y de reenviar código: mensaje y cuántos segundos esperar para reenviar
@Serializable
data class CodigoEnviadoDto(
    val mensaje: String,
    @SerialName("reenviar_en_segundos") val reenviarEnSegundos: Int
)
