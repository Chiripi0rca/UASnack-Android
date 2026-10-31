package mx.uasnack.app.data.red.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ESQUELETO (Sprint 03). DTOs de franjas y espera: la forma del JSON acordada en el documento de
// turnos del Sprint 03. No se cambian sin avisar: los usan #173 (Daniel) y #175 (Josef).

// Una franja de GET /tiendas/{id}/franjas (#53 del backend)
@Serializable
data class FranjaDto(
    // "2026-10-22"
    val fecha: String,
    // "11:00" y "11:10"
    val inicio: String,
    val fin: String,
    @SerialName("minutos_restantes") val minutosRestantes: Int,
    // false = llena para este pedido: se muestra deshabilitada
    val disponible: Boolean,
    // Porcentaje de descuento de la franja (10 = −10%), o null si no tiene
    val descuento: Int? = null
)

// Respuesta completa de GET /tiendas/{id}/franjas: la lista y su "meta" (#53 del backend).
@Serializable
data class FranjasDto(
    val datos: List<FranjaDto>,
    val meta: FranjasMetaDto = FranjasMetaDto()
)

// Con valores por defecto: si el backend no los manda, la pantalla se comporta como antes (sin aviso)
@Serializable
data class FranjasMetaDto(
    // false = el pedido no cabe en ninguna hora de hoy
    @SerialName("cabe_hoy") val cabeHoy: Boolean = true,
    // El pedido más grande que todavía cabe hoy, en minutos de cocina. 0 = ya no hay horas hoy.
    @SerialName("minutos_maximos_hoy") val minutosMaximosHoy: Int? = null
)

// GET /tiendas/{id}/espera (#57 del backend). Siempre un rango, nunca un tiempo fijo.
@Serializable
data class EstimadoDto(
    val min: Int,
    val max: Int,
    // true = la cocina está cargada: se advierte y se sugiere apartar franja
    val saturada: Boolean,
    @SerialName("pedidos_en_cocina") val pedidosEnCocina: Int,
    // false = la tienda ya no acepta más pedidos "voy saliendo" por ahora
    @SerialName("voy_saliendo_disponible") val voySaliendoDisponible: Boolean
)
