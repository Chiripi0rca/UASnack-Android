package mx.uasnack.app.domain.modelo

import java.math.BigDecimal

// Producto del catálogo tal como lo usa la interfaz (#228).
//
// Los precios son solo para mostrar: el total que se cobra lo calcula el servidor.
// Se usa BigDecimal y no Double porque Double redondea mal el dinero (0.1 + 0.2 = 0.30000000000000004).
data class Producto(
    val id: Int,
    val idTienda: Int,
    val idCategoria: Int?,
    val nombre: String,
    val descripcion: String?,
    val precio: BigDecimal,
    val stockHoy: Int,
    val tiempoPrepMin: Int,
    val tiempoPrepMax: Int,
    val fotoUrl: String?,
    // Un producto agotado se muestra atenuado, pero nunca se oculta (HU-03, CA-02)
    val agotado: Boolean,
    // Lo que el alumno puede agregar (queso extra) o quitar (sin cebolla). Viene del catálogo dinámico (Mongo).
    val extras: List<OpcionProducto>,
    val quitables: List<OpcionProducto>
)

// Una opción de personalización. Los quitables no cuestan, por eso precio puede ser null.
data class OpcionProducto(
    val id: String,
    val nombre: String,
    val precio: BigDecimal?
)
