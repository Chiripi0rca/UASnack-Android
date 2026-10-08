package mx.uasnack.app.data.local.catalogo

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mx.uasnack.app.data.red.dto.ProductoDto
import mx.uasnack.app.data.red.dto.TiendaDto
import mx.uasnack.app.domain.modelo.OpcionProducto
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.domain.modelo.Tienda
import java.math.BigDecimal

// Traducciones del catálogo (#228). El camino siempre es el mismo:
//   servidor (DTO) → caché (Entity) → pantallas (modelo de dominio)
// Las pantallas nunca leen el DTO: así lo que se ve con red y sin red sale del mismo lugar.

private const val EXTRAS = "extras"
private const val QUITABLES = "quitables"

private val json = Json { ignoreUnknownKeys = true }

// Forma en que se guardan extras y quitables dentro de la columna JSON
@Serializable
private data class OpcionGuardada(val id: String, val nombre: String, val precio: String? = null)

private val serializadorOpciones = ListSerializer(OpcionGuardada.serializer())

// --- Servidor → caché ---

// orden: la posición en la que llegó, para no perder "las de tu facultad primero"
fun TiendaDto.aEntidad(orden: Int) = TiendaEntity(
    id = id,
    nombre = nombre,
    ubicacion = ubicacion,
    horarioApertura = horarioApertura,
    horarioCierre = horarioCierre,
    abierta = abierta,
    esMiFacultad = esMiFacultad,
    orden = orden
)

fun ProductoDto.aEntidad(orden: Int): ProductoEntity {
    fun opcionesDe(tipo: String) = personalizaciones
        .filter { it.tipo == tipo }
        .flatMap { it.opciones }
        .map { OpcionGuardada(it.id, it.nombre, it.precio) }

    return ProductoEntity(
        id = id,
        idTienda = idTienda,
        idCategoria = idCategoria,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        stockHoy = stockHoy,
        tiempoPrepMin = tiempoPrepMin,
        tiempoPrepMax = tiempoPrepMax,
        fotoUrl = fotoUrl,
        // Agotado: lo que diga el backend; si no lo manda, sin existencias o marcado como agotado
        agotado = agotado ?: (stockHoy <= 0 || estadoProducto == "agotado"),
        extrasJson = json.encodeToString(serializadorOpciones, opcionesDe(EXTRAS)),
        quitablesJson = json.encodeToString(serializadorOpciones, opcionesDe(QUITABLES)),
        orden = orden
    )
}

// --- Caché → pantallas ---

fun TiendaEntity.aDominio() = Tienda(
    id = id,
    nombre = nombre,
    ubicacion = ubicacion,
    horarioApertura = horarioApertura,
    horarioCierre = horarioCierre,
    abierta = abierta,
    esMiFacultad = esMiFacultad
)

fun ProductoEntity.aDominio() = Producto(
    id = id,
    idTienda = idTienda,
    idCategoria = idCategoria,
    nombre = nombre,
    descripcion = descripcion,
    precio = precio.aDecimal(),
    stockHoy = stockHoy,
    tiempoPrepMin = tiempoPrepMin,
    tiempoPrepMax = tiempoPrepMax,
    fotoUrl = fotoUrl,
    agotado = agotado,
    extras = leerOpciones(extrasJson),
    quitables = leerOpciones(quitablesJson)
)

private fun leerOpciones(texto: String): List<OpcionProducto> =
    runCatching { json.decodeFromString(serializadorOpciones, texto) }
        .getOrDefault(emptyList())
        .map { OpcionProducto(id = it.id, nombre = it.nombre, precio = it.precio?.aDecimal()) }

// Si el texto no es un número válido se muestra 0 en lugar de tumbar la pantalla
private fun String.aDecimal(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO
