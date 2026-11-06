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

// --- ESQUELETO (Sprint 04). La forma del JSON está en el documento de turnos del Sprint 04. ---

// Cuerpo de POST /pedidos (#148 del backend). El header Idempotency-Key lo pone #183.
@Serializable
data class CrearPedidoRequest(
    @SerialName("id_tienda") val idTienda: Int,
    // "franja" o "voy_saliendo"
    @SerialName("modo_retiro") val modoRetiro: String,
    // "2026-11-09 11:30"; null en "voy saliendo"
    val franja: String? = null,
    val lineas: List<LineaPedidoRequest>
)

@Serializable
data class LineaPedidoRequest(
    @SerialName("id_producto") val idProducto: Int,
    val cantidad: Int,
    val extras: List<Int> = emptyList(),
    val quitables: List<Int> = emptyList()
)

// Respuesta de POST /pedidos: 201, o 200 si la clave se repite
@Serializable
data class PedidoCreadoDto(
    @SerialName("codigo_pedido") val codigoPedido: String,
    val estado: String,
    // Llega como texto para no perder centavos: "85.00"
    val total: String,
    @SerialName("franja_inicio") val franjaInicio: String? = null,
    @SerialName("franja_fin") val franjaFin: String? = null,
    val pago: DatosPagoDto
)

// Lo que necesita PaymentSheet para confirmar el cobro en la cuenta de la tienda
@Serializable
data class DatosPagoDto(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("stripe_account") val stripeAccount: String
)

// Un renglón de GET /pagos (#69 del backend)
@Serializable
data class PagoDto(
    val id: Int,
    @SerialName("codigo_pedido") val codigoPedido: String,
    val monto: String,
    // "pendiente", "pagado", "reembolsado" o "fallido"
    val estado: String,
    @SerialName("creado_en") val creadoEn: String
)

// Una tarjeta de GET /pagos/metodos (#188 del backend)
@Serializable
data class MetodoPagoDto(
    val id: String,
    val marca: String,
    val ultimos4: String,
    val vence: String
)

// Respuesta de POST /pagos/metodos (#65 del backend). Con esto PaymentSheet guarda la tarjeta.
@Serializable
data class SetupIntentDto(
    @SerialName("client_secret") val clientSecret: String
)
