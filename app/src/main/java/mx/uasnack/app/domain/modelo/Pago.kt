package mx.uasnack.app.domain.modelo

// ESQUELETO (Sprint 04). Pedidos y pagos tal como los usa la interfaz, sin anotaciones de red.
// Los llenan PedidoRepository.crear() y PagoRepository.

// Lo que regresa crear el pedido. Pedido confirmado (#196) usa el folio, el total y la hora.
data class PedidoCreado(
    val codigoPedido: String,
    val estado: String,
    val total: String,
    // "11:30" y "11:40"; null en "voy saliendo"
    val franjaInicio: String?,
    val franjaFin: String?,
    // Para PaymentSheet: confirma el cobro en la cuenta de la tienda
    val clientSecret: String,
    val stripeAccount: String
)

// Un renglón del historial de pagos
data class Pago(
    val id: Int,
    val codigoPedido: String,
    val monto: String,
    // "pendiente", "pagado", "reembolsado" o "fallido"
    val estado: String,
    val creadoEn: String
)

// Una tarjeta guardada. La tarjeta completa solo la conoce Stripe.
data class MetodoPago(
    val id: String,
    val marca: String,
    val ultimos4: String,
    val vence: String
)
