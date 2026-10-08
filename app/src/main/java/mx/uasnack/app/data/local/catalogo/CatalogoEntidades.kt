package mx.uasnack.app.data.local.catalogo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tablas de la caché local del catálogo (#228). Guardan lo último que se vio, para mostrarlo sin red.
// Room NUNCA es la fuente de verdad: cada vez que hay red se reemplaza con lo que manda el servidor.

@Entity(tableName = "tiendas")
data class TiendaEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val ubicacion: String?,
    val horarioApertura: String?,
    val horarioCierre: String?,
    val abierta: Boolean,
    val esMiFacultad: Boolean,
    // Posición en la que llegó del servidor (las de la facultad del alumno primero).
    // Se guarda porque una tabla no conserva el orden por sí sola.
    val orden: Int
)

@Entity(tableName = "productos", indices = [Index("idTienda")])
data class ProductoEntity(
    @PrimaryKey val id: Int,
    val idTienda: Int,
    val idCategoria: Int?,
    val nombre: String,
    val descripcion: String?,
    // Texto ("25.00") para no perder centavos al guardar
    val precio: String,
    val stockHoy: Int,
    val tiempoPrepMin: Int,
    val tiempoPrepMax: Int,
    val fotoUrl: String?,
    val agotado: Boolean,
    // Extras y quitables como JSON: solo se leen junto con el producto, no se buscan por separado
    val extrasJson: String,
    val quitablesJson: String,
    val orden: Int
)
