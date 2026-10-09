package mx.uasnack.app.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.uasnack.app.data.local.CarritoAlmacen
import mx.uasnack.app.data.local.CarritoAlmacen.LineaGuardada
import mx.uasnack.app.data.local.CarritoAlmacen.OpcionGuardada
import mx.uasnack.app.domain.modelo.Carrito
import mx.uasnack.app.domain.modelo.LineaCarrito
import mx.uasnack.app.domain.modelo.OpcionProducto
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.domain.modelo.ResultadoAgregar
import java.math.BigDecimal

// Única puerta de la app para el carrito (#231). Lo usan Detalle de producto (#51) y Carrito (#170).
//
// Reglas:
//   - Un carrito es de UNA tienda. Agregar de otra no cambia nada y regresa OtraTienda.
//   - El mismo producto con los mismos extras y quitables, en cualquier orden, es la misma línea.
//   - No se pueden pedir más unidades que las existencias del producto (HU-04, CA-03).
//   - Guarda ids y cantidades; los precios son solo para mostrar. El servidor calcula el cobro.
//   - Se vacía al crear el pedido (#148, Sprint 04).
class CarritoRepository(private val almacen: CarritoAlmacen) {

    // El carrito actual. Emite otra vez cada vez que cambia.
    fun carrito(): Flow<Carrito> =
        almacen.lineas.map { lineas -> Carrito(lineas.map { it.aDominio() }) }

    suspend fun agregar(
        producto: Producto,
        cantidad: Int = 1,
        extras: List<OpcionProducto> = emptyList(),
        quitables: List<OpcionProducto> = emptyList()
    ): ResultadoAgregar = almacen.modificar { lineas ->
        agregarA(lineas, producto, cantidad, extras, quitables)
    }

    // Para cuando el alumno acepta "vaciar el carrito" tras recibir OtraTienda
    suspend fun vaciarYAgregar(
        producto: Producto,
        cantidad: Int = 1,
        extras: List<OpcionProducto> = emptyList(),
        quitables: List<OpcionProducto> = emptyList()
    ): ResultadoAgregar = almacen.modificar { _ ->
        agregarA(emptyList(), producto, cantidad, extras, quitables)
    }

    // Cambia la cantidad de una línea (pantalla de Carrito). 0 o menos la quita.
    // No revisa existencias: las del catálogo pueden haber cambiado; las revisa el servidor al pedir.
    suspend fun cambiarCantidad(clave: String, cantidad: Int) = almacen.modificar { lineas ->
        val nuevas = if (cantidad <= 0) {
            lineas.filterNot { it.clave == clave }
        } else {
            lineas.map { if (it.clave == clave) it.copy(cantidad = cantidad) else it }
        }
        nuevas to Unit
    }

    suspend fun quitar(clave: String) = almacen.modificar { lineas ->
        lineas.filterNot { it.clave == clave } to Unit
    }

    suspend fun vaciar() = almacen.modificar { _ -> emptyList<LineaGuardada>() to Unit }

    private fun agregarA(
        lineas: List<LineaGuardada>,
        producto: Producto,
        cantidad: Int,
        extras: List<OpcionProducto>,
        quitables: List<OpcionProducto>
    ): Pair<List<LineaGuardada>, ResultadoAgregar> {
        val tiendaDelCarrito = lineas.firstOrNull()?.idTienda
        if (tiendaDelCarrito != null && tiendaDelCarrito != producto.idTienda) {
            return lineas to ResultadoAgregar.OtraTienda(tiendaDelCarrito)
        }

        // Existencias: cuenta TODAS las líneas de ese producto (con y sin extras son el mismo stock)
        val yaEnCarrito = lineas.filter { it.idProducto == producto.id }.sumOf { it.cantidad }
        val disponibles = (producto.stockHoy - yaEnCarrito).coerceAtLeast(0)
        if (producto.agotado || cantidad > disponibles) {
            return lineas to ResultadoAgregar.SinExistencias(disponibles)
        }

        val clave = claveDeLinea(producto.id, extras, quitables)
        val existente = lineas.firstOrNull { it.clave == clave }
        val nuevas = if (existente != null) {
            lineas.map { if (it.clave == clave) it.copy(cantidad = it.cantidad + cantidad) else it }
        } else {
            lineas + LineaGuardada(
                clave = clave,
                idProducto = producto.id,
                idTienda = producto.idTienda,
                nombre = producto.nombre,
                precioUnitario = producto.precio.toPlainString(),
                cantidad = cantidad,
                extras = extras.map { it.aGuardada() },
                quitables = quitables.map { it.aGuardada() }
            )
        }
        return nuevas to ResultadoAgregar.Agregado
    }

    companion object {
        // producto|extras|quitables, con los ids ORDENADOS como texto: así "queso, aguacate" y
        // "aguacate, queso" dan la misma clave. (sorted() ordena texto alfabéticamente; los ids
        // de las opciones son texto, como "queso", no números.)
        fun claveDeLinea(idProducto: Int, extras: List<OpcionProducto>, quitables: List<OpcionProducto>): String {
            fun ids(opciones: List<OpcionProducto>) = opciones.map { it.id }.sorted().joinToString(",")
            return "$idProducto|${ids(extras)}|${ids(quitables)}"
        }
    }
}

private fun OpcionProducto.aGuardada() = OpcionGuardada(id = id, nombre = nombre, precio = precio?.toPlainString())

private fun OpcionGuardada.aDominio() = OpcionProducto(id = id, nombre = nombre, precio = precio?.toBigDecimalOrNull())

private fun LineaGuardada.aDominio() = LineaCarrito(
    clave = clave,
    idProducto = idProducto,
    idTienda = idTienda,
    nombre = nombre,
    precioUnitario = precioUnitario.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    cantidad = cantidad,
    extras = extras.map { it.aDominio() },
    quitables = quitables.map { it.aDominio() }
)
