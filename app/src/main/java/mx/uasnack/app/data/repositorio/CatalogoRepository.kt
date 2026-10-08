package mx.uasnack.app.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.uasnack.app.data.local.catalogo.CatalogoDao
import mx.uasnack.app.data.local.catalogo.aDominio
import mx.uasnack.app.data.local.catalogo.aEntidad
import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.domain.modelo.Tienda

// Única puerta de la app para el catálogo (#228): tiendas, productos de una tienda y detalle de un producto.
//
// Funciona "offline-first":
//   - tiendas(), catalogo() y producto() regresan un Flow que lee de la caché (Room). Emite al instante
//     lo último que se vio, y vuelve a emitir solo cuando la caché cambia.
//   - refrescar…() pide los datos al servidor y los guarda en la caché. Al guardarlos, el Flow emite
//     de nuevo y la pantalla se actualiza sola.
//
// Uso típico en un ViewModel:
//   init {
//       viewModelScope.launch { catalogoRepository.tiendas().collect { lista -> /* pintar */ } }
//       viewModelScope.launch { catalogoRepository.refrescarTiendas().onFailure { /* aviso */ } }
//   }
//
// Si no hay red, refrescar…() regresa el ErrorApi (SIN_CONEXION) y la pantalla sigue mostrando
// lo que había en la caché: "sin red, el catálogo muestra lo último que se vio".
class CatalogoRepository(
    private val api: ApiService,
    private val dao: CatalogoDao
) {

    // --- Tiendas ---

    // Las de la facultad del alumno primero (así las manda el servidor y así se guardan)
    fun tiendas(): Flow<List<Tienda>> =
        dao.observarTiendas().map { lista -> lista.map { it.aDominio() } }

    // GET /tiendas. Deja en la caché exactamente lo que mandó el servidor.
    suspend fun refrescarTiendas(): Result<Unit> = intentar {
        val tiendas = api.obtenerTiendas().datos
        dao.reemplazarTiendas(tiendas.mapIndexed { posicion, tienda -> tienda.aEntidad(posicion) })
    }

    // --- Productos de una tienda ---

    // idCategoria = null: todas. El filtro se hace aquí y no en el servidor para que también funcione
    // sin red. Los agotados se incluyen siempre (HU-03, CA-02: se atenúan, nunca se ocultan).
    fun catalogo(idTienda: Int, idCategoria: Int? = null): Flow<List<Producto>> =
        dao.observarProductos(idTienda).map { lista ->
            lista
                .filter { idCategoria == null || it.idCategoria == idCategoria }
                .map { it.aDominio() }
        }

    // GET /tiendas/{id}/productos. Reemplaza solo los productos de esa tienda.
    suspend fun refrescarCatalogo(idTienda: Int): Result<Unit> = intentar {
        val productos = api.obtenerProductos(idTienda).datos
        dao.reemplazarProductosDe(idTienda, productos.mapIndexed { posicion, producto -> producto.aEntidad(posicion) })
    }

    // --- Detalle de un producto ---

    // null si nunca se ha visto ese producto y todavía no llega del servidor
    fun producto(idProducto: Int): Flow<Producto?> =
        dao.observarProducto(idProducto).map { it?.aDominio() }

    // GET /productos/{id}. Actualiza ese producto en la caché con sus extras y quitables al día.
    suspend fun refrescarProducto(idProducto: Int, posicion: Int = 0): Result<Unit> = intentar {
        val producto = api.obtenerProducto(idProducto).datos
        dao.guardarProductos(listOf(producto.aEntidad(posicion)))
    }

    // Convierte el resultado en Result: los ErrorApi (sin red, 401, 404...) regresan como failure
    // con su código y mensaje del servidor, igual que en SesionRepository.
    private suspend fun intentar(bloque: suspend () -> Unit): Result<Unit> = try {
        bloque()
        Result.success(Unit)
    } catch (e: ErrorApi) {
        Result.failure(e)
    }
}
