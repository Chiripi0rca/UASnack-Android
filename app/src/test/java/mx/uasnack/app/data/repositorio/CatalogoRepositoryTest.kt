package mx.uasnack.app.data.repositorio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mx.uasnack.app.data.local.catalogo.CatalogoDao
import mx.uasnack.app.data.local.catalogo.ProductoEntity
import mx.uasnack.app.data.local.catalogo.TiendaEntity
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.ModuloRed
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

// Prueba CatalogoRepository (#228): API falsa (MockWebServer) y una caché falsa en memoria que se
// comporta como el CatalogoDao de Room (Room real necesita Android para correr).
class CatalogoRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var cache: CacheFalsa
    private lateinit var repositorio: CatalogoRepository

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        cache = CacheFalsa()
        val api = ModuloRed.crearApiService(
            tokenActual = { "1|abc" },
            alVencerSesion = {},
            urlBase = servidor.url("/api/v1/").toString()
        )
        repositorio = CatalogoRepository(api, cache)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun `las tiendas se guardan en el orden del servidor, con las de mi facultad primero`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(TIENDAS))

        assertTrue(repositorio.refrescarTiendas().isSuccess)

        val tiendas = repositorio.tiendas().first()
        assertEquals(listOf("Cafetería de la FIC", "Cafetería de Derecho"), tiendas.map { it.nombre })
        assertTrue(tiendas[0].esMiFacultad)
        assertFalse(tiendas[1].esMiFacultad)
        assertEquals("/api/v1/tiendas", servidor.takeRequest().path)
    }

    @Test
    fun `sin red el catalogo sigue mostrando lo ultimo que se vio`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(TIENDAS))
        repositorio.refrescarTiendas()

        // Se apaga el servidor: la siguiente petición no tiene a quién llegar
        servidor.shutdown()
        val resultado = repositorio.refrescarTiendas()

        assertTrue(resultado.isFailure)
        assertEquals(ErrorApi.SIN_CONEXION, (resultado.exceptionOrNull() as ErrorApi).codigo)
        assertEquals(2, repositorio.tiendas().first().size)
    }

    @Test
    fun `una tienda dada de baja desaparece de la cache al refrescar`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(TIENDAS))
        servidor.enqueue(MockResponse().setBody("""{"datos":[$TIENDA_FIC],"meta":{"total":1}}"""))

        repositorio.refrescarTiendas()
        repositorio.refrescarTiendas()

        assertEquals(listOf(1), repositorio.tiendas().first().map { it.id })
    }

    @Test
    fun `el catalogo trae precios, extras y quitables, y los agotados no se ocultan`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(PRODUCTOS))

        assertTrue(repositorio.refrescarCatalogo(idTienda = 1).isSuccess)

        val productos = repositorio.catalogo(idTienda = 1).first()
        assertEquals(2, productos.size)

        val torta = productos[0]
        assertEquals(BigDecimal("45.50"), torta.precio)
        assertFalse(torta.agotado)
        assertEquals(listOf("queso", "aguacate"), torta.extras.map { it.id })
        assertEquals(BigDecimal("5.00"), torta.extras[0].precio)
        assertEquals(listOf("cebolla"), torta.quitables.map { it.id })
        assertNull(torta.quitables[0].precio)

        // stock_hoy = 0 → agotado, pero sigue en la lista
        assertTrue(productos[1].agotado)
        assertEquals("/api/v1/tiendas/1/productos", servidor.takeRequest().path)
    }

    @Test
    fun `el filtro por categoria funciona sobre la cache`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(PRODUCTOS))
        repositorio.refrescarCatalogo(idTienda = 1)

        val bebidas = repositorio.catalogo(idTienda = 1, idCategoria = 2).first()

        assertEquals(listOf("Agua de jamaica"), bebidas.map { it.nombre })
    }

    @Test
    fun `refrescar una tienda no borra la cache de las demas`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(PRODUCTOS))
        servidor.enqueue(MockResponse().setBody("""{"datos":[],"meta":{"total":0}}"""))

        repositorio.refrescarCatalogo(idTienda = 1)
        repositorio.refrescarCatalogo(idTienda = 2)

        assertEquals(2, repositorio.catalogo(idTienda = 1).first().size)
    }

    @Test
    fun `el detalle de un producto llega envuelto en datos y se guarda en la cache`() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"datos":$TORTA}"""))

        assertNull(repositorio.producto(10).first())
        assertTrue(repositorio.refrescarProducto(10).isSuccess)

        assertEquals("Torta de jamón", repositorio.producto(10).first()?.nombre)
        assertEquals("/api/v1/productos/10", servidor.takeRequest().path)
    }

    @Test
    fun `el precio se entiende aunque llegue como numero y no como texto`() = runBlocking {
        val conNumero = TORTA.replace("\"precio\":\"45.50\"", "\"precio\":45.5")
        servidor.enqueue(MockResponse().setBody("""{"datos":$conNumero}"""))

        repositorio.refrescarProducto(10)

        assertEquals(0, BigDecimal("45.5").compareTo(repositorio.producto(10).first()!!.precio))
    }

    @Test
    fun `un 404 regresa el error del servidor`() = runBlocking {
        servidor.enqueue(
            MockResponse().setResponseCode(404)
                .setBody("""{"error":{"codigo":"NO_ENCONTRADO","mensaje":"No encontramos lo que buscas."}}""")
        )

        val error = repositorio.refrescarProducto(99).exceptionOrNull() as ErrorApi

        assertEquals("NO_ENCONTRADO", error.codigo)
    }

    // Caché en memoria con el mismo comportamiento que el CatalogoDao de Room:
    // los Flow vuelven a emitir cada vez que cambian los datos.
    private class CacheFalsa : CatalogoDao() {
        private val tiendas = MutableStateFlow<List<TiendaEntity>>(emptyList())
        private val productos = MutableStateFlow<List<ProductoEntity>>(emptyList())

        override fun observarTiendas(): Flow<List<TiendaEntity>> = tiendas.map { it.sortedBy(TiendaEntity::orden) }

        override suspend fun guardarTiendas(tiendas: List<TiendaEntity>) {
            val nuevos = tiendas.associateBy { it.id }
            this.tiendas.value = this.tiendas.value.filterNot { it.id in nuevos } + tiendas
        }

        override suspend fun borrarTiendas() {
            tiendas.value = emptyList()
        }

        override fun observarProductos(idTienda: Int): Flow<List<ProductoEntity>> =
            productos.map { lista -> lista.filter { it.idTienda == idTienda }.sortedBy(ProductoEntity::orden) }

        override fun observarProducto(id: Int): Flow<ProductoEntity?> =
            productos.map { lista -> lista.firstOrNull { it.id == id } }

        override suspend fun guardarProductos(productos: List<ProductoEntity>) {
            val nuevos = productos.associateBy { it.id }
            this.productos.value = this.productos.value.filterNot { it.id in nuevos } + productos
        }

        override suspend fun borrarProductosDe(idTienda: Int) {
            productos.value = productos.value.filterNot { it.idTienda == idTienda }
        }
    }

    private companion object {
        const val TIENDA_FIC = """{"id":1,"nombre":"Cafetería de la FIC","ubicacion":"Módulo A",
            "horario_apertura":"07:00","horario_cierre":"19:00","abierta":true,"es_mi_facultad":true}"""

        const val TIENDAS = """{"datos":[
            $TIENDA_FIC,
            {"id":5,"nombre":"Cafetería de Derecho","abierta":true,"es_mi_facultad":false}
        ],"meta":{"total":2}}"""

        const val TORTA = """{"id":10,"id_tienda":1,"id_categoria":1,"nombre":"Torta de jamón",
            "precio":"45.50","stock_hoy":8,"tiempo_prep_min":5,"tiempo_prep_max":8,
            "estado_producto":"disponible",
            "personalizaciones":[
                {"tipo":"extras","opciones":[
                    {"id":"queso","nombre":"Queso extra","precio":"5.00"},
                    {"id":"aguacate","nombre":"Aguacate","precio":"8.00"}]},
                {"tipo":"quitables","opciones":[{"id":"cebolla","nombre":"Cebolla"}]}
            ]}"""

        const val PRODUCTOS = """{"datos":[
            $TORTA,
            {"id":11,"id_tienda":1,"id_categoria":2,"nombre":"Agua de jamaica","precio":"20.00",
             "stock_hoy":0,"tiempo_prep_min":1,"tiempo_prep_max":2}
        ],"meta":{"total":2}}"""
    }
}
