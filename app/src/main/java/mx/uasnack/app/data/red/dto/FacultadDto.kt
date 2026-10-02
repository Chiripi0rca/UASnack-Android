package mx.uasnack.app.data.red.dto

import kotlinx.serialization.Serializable

// FacultadResource del backend
@Serializable
data class FacultadDto(
    val id: Int,
    val nombre: String,
    val clave: String
)

// Las listas de la API llegan envueltas: { "datos": [...], "meta": {...} }.
// "meta" se ignora por ahora (ignoreUnknownKeys en el Json de ModuloRed).
@Serializable
data class ListaDto<T>(
    val datos: List<T>
)
