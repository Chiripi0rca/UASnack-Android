package mx.uasnack.app.domain.modelo

// ESQUELETO (Sprint 03). Franjas y espera tal como las usa la interfaz, sin anotaciones de red.
// Las llena PedidoRepository: franjas() (#173) y espera() (#175).

// Una franja de retiro de 10 minutos (o lo que dure en esa tienda)
data class Franja(
    // "2026-10-22"
    val fecha: String,
    // "11:00" y "11:10"
    val inicio: String,
    val fin: String,
    val minutosRestantes: Int,
    // false = llena para este pedido: se muestra deshabilitada, nunca se oculta
    val disponible: Boolean,
    // 10 = −10%; null si no tiene descuento
    val descuento: Int?
)

// Lo que regresa PedidoRepository.franjas(): las franjas y si el pedido cabe hoy
data class FranjasDelDia(
    val franjas: List<Franja>,
    // false = no cabe en ninguna hora de hoy: se avisa "Tu pedido es muy grande para hoy"
    val cabeHoy: Boolean,
    // El pedido más grande que todavía cabe hoy, en minutos de cocina. 0 = ya no hay horas hoy.
    val minutosMaximosHoy: Int?
)

// Tiempo estimado del modo "voy saliendo". Siempre un rango: nunca se muestra un tiempo fijo.
data class Estimado(
    val min: Int,
    val max: Int,
    // true = la cocina está cargada: se advierte y se sugiere apartar franja
    val saturada: Boolean,
    val pedidosEnCocina: Int,
    // false = la tienda ya no acepta más pedidos "voy saliendo" por ahora
    val voySaliendoDisponible: Boolean
)
