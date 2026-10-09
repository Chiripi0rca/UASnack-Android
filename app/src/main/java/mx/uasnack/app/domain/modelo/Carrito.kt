package mx.uasnack.app.domain.modelo

import java.math.BigDecimal

// Carrito del alumno tal como lo usa la interfaz (#231).
//
// Todos los productos son de UNA sola tienda: un pedido es de una tienda.
// totalLocal es solo para mostrar: el total que se cobra lo calcula el servidor (#49).
data class Carrito(
    val lineas: List<LineaCarrito> = emptyList()
) {
    val vacio: Boolean get() = lineas.isEmpty()

    // La tienda de los productos del carrito, o null si está vacío
    val idTienda: Int? get() = lineas.firstOrNull()?.idTienda

    // Para el numerito del ícono del carrito
    val totalArticulos: Int get() = lineas.sumOf { it.cantidad }

    val totalLocal: BigDecimal get() = lineas.fold(BigDecimal.ZERO) { suma, linea -> suma + linea.subtotal }
}

// Una línea del carrito: un producto con sus extras y quitables, y cuántos.
// Dos veces el mismo producto con los mismos extras y quitables es la MISMA línea (se suma la cantidad).
data class LineaCarrito(
    // Identifica la línea: producto + extras + quitables, sin importar en qué orden se eligieron
    val clave: String,
    val idProducto: Int,
    val idTienda: Int,
    val nombre: String,
    // Precio del producto sin extras, tal como se vio en el catálogo
    val precioUnitario: BigDecimal,
    val cantidad: Int,
    val extras: List<OpcionProducto>,
    val quitables: List<OpcionProducto>
) {
    // (precio + extras) × cantidad. Los quitables no cuestan.
    val subtotal: BigDecimal
        get() {
            val precioExtras = extras.fold(BigDecimal.ZERO) { suma, extra -> suma + (extra.precio ?: BigDecimal.ZERO) }
            return (precioUnitario + precioExtras) * cantidad.toBigDecimal()
        }
}

// Lo que pasa al intentar agregar un producto al carrito
sealed interface ResultadoAgregar {
    data object Agregado : ResultadoAgregar

    // El carrito ya tiene productos de otra tienda. No se cambió nada: la pantalla le pregunta al
    // alumno si quiere vaciarlo y, si dice que sí, llama a vaciarYAgregar().
    data class OtraTienda(val idTiendaDelCarrito: Int) : ResultadoAgregar

    // No hay existencias suficientes (HU-04, CA-03). disponibles = cuántos más se pueden agregar.
    data class SinExistencias(val disponibles: Int) : ResultadoAgregar
}
