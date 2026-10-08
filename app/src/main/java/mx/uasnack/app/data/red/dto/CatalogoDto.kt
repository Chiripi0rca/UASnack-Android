package mx.uasnack.app.data.red.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

// DTOs del catálogo (#228): la forma del JSON de GET /tiendas, /tiendas/{id}/productos y /productos/{id}.
// Esos endpoints los hace la #46 del backend. Si allá cambia un nombre, se ajusta aquí y en Mapeos.kt;
// las pantallas no se enteran.

// TiendaResource del backend
@Serializable
data class TiendaDto(
    val id: Int,
    val nombre: String,
    val ubicacion: String? = null,
    @SerialName("horario_apertura") val horarioApertura: String? = null,
    @SerialName("horario_cierre") val horarioCierre: String? = null,
    val abierta: Boolean = true,
    // true si la tienda es de la facultad del alumno: esas llegan primero en la lista
    @SerialName("es_mi_facultad") val esMiFacultad: Boolean = false
)

// ProductoResource del backend
@Serializable
data class ProductoDto(
    val id: Int,
    @SerialName("id_tienda") val idTienda: Int,
    @SerialName("id_categoria") val idCategoria: Int? = null,
    val nombre: String,
    val descripcion: String? = null,
    // Laravel manda los decimales como texto ("25.00"); DecimalComoTexto acepta también un número
    @Serializable(with = DecimalComoTexto::class) val precio: String,
    @SerialName("stock_hoy") val stockHoy: Int = 0,
    @SerialName("tiempo_prep_min") val tiempoPrepMin: Int = 0,
    @SerialName("tiempo_prep_max") val tiempoPrepMax: Int = 0,
    @SerialName("foto_url") val fotoUrl: String? = null,
    @SerialName("estado_producto") val estadoProducto: String? = null,
    // Si el backend lo manda ya calculado, se usa; si no, se deduce del stock y del estado
    val agotado: Boolean? = null,
    // Del catálogo dinámico (Mongo): [{ tipo: "extras", opciones: [...] }, { tipo: "quitables", opciones: [...] }]
    val personalizaciones: List<PersonalizacionDto> = emptyList()
)

@Serializable
data class PersonalizacionDto(
    // "extras" o "quitables"
    val tipo: String,
    val opciones: List<OpcionDto> = emptyList()
)

@Serializable
data class OpcionDto(
    val id: String,
    val nombre: String,
    @Serializable(with = DecimalComoTextoOpcional::class) val precio: String? = null
)

// Respuesta de un solo recurso: { "datos": { ... } }, igual que las listas usan { "datos": [...], "meta": {...} }
@Serializable
data class ObjetoDto<T>(
    val datos: T
)

// Lee un decimal venga como texto ("25.00") o como número (25.0) y lo deja como texto.
// Así un cambio de cast en Laravel no tumba la app.
object DecimalComoTexto : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("DecimalComoTexto", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String =
        (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive?.content ?: decoder.decodeString()

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

object DecimalComoTextoOpcional : KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor("DecimalComoTextoOpcional", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val elemento = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        return elemento.jsonPrimitive.takeUnless { it.content == "null" }?.content
    }

    override fun serialize(encoder: Encoder, value: String?) = encoder.encodeString(value.orEmpty())
}
