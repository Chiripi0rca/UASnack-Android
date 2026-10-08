package mx.uasnack.app.data.local.catalogo

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Consultas a la caché del catálogo (#228). Room genera la implementación al compilar (ksp).
//
// Las de lectura regresan Flow: emiten lo que hay guardado y vuelven a emitir cada vez que cambia,
// así la pantalla se actualiza sola cuando llega lo nuevo del servidor.
//
// Es una clase abstracta (y no una interfaz) para poder tener métodos @Transaction con cuerpo.
// Los métodos "reemplazar…" son open para que las pruebas puedan usar una versión falsa sin Room.
@Dao
abstract class CatalogoDao {

    // --- Tiendas ---

    @Query("SELECT * FROM tiendas ORDER BY orden")
    abstract fun observarTiendas(): Flow<List<TiendaEntity>>

    @Upsert
    abstract suspend fun guardarTiendas(tiendas: List<TiendaEntity>)

    @Query("DELETE FROM tiendas")
    abstract suspend fun borrarTiendas()

    // Deja en la caché exactamente lo que mandó el servidor: si una tienda se dio de baja, desaparece.
    // En una transacción para que la pantalla nunca vea la lista a medias (vacía un instante).
    @Transaction
    open suspend fun reemplazarTiendas(tiendas: List<TiendaEntity>) {
        borrarTiendas()
        guardarTiendas(tiendas)
    }

    // --- Productos ---

    @Query("SELECT * FROM productos WHERE idTienda = :idTienda ORDER BY orden")
    abstract fun observarProductos(idTienda: Int): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id")
    abstract fun observarProducto(id: Int): Flow<ProductoEntity?>

    @Upsert
    abstract suspend fun guardarProductos(productos: List<ProductoEntity>)

    @Query("DELETE FROM productos WHERE idTienda = :idTienda")
    abstract suspend fun borrarProductosDe(idTienda: Int)

    // Igual que con las tiendas, pero solo los productos de UNA tienda: las demás conservan su caché.
    @Transaction
    open suspend fun reemplazarProductosDe(idTienda: Int, productos: List<ProductoEntity>) {
        borrarProductosDe(idTienda)
        guardarProductos(productos)
    }
}
