package mx.uasnack.app.data.red.dto

import kotlinx.serialization.Serializable

// Forma de todos los errores de la API: { "error": { "codigo", "mensaje", "campos" } }
@Serializable
data class RespuestaErrorDto(
    val error: ErrorDto
)

@Serializable
data class ErrorDto(
    // Código para que el cliente decida qué hacer: CREDENCIALES_INVALIDAS, CORREO_NO_VERIFICADO...
    val codigo: String,
    // Texto para mostrar tal cual al usuario
    val mensaje: String,
    // Solo en 422 (VALIDACION): mensajes por campo, por ejemplo {"password": ["..."]}
    val campos: Map<String, List<String>> = emptyMap()
)
