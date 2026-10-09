package mx.uasnack.app.data.repositorio

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.uasnack.app.data.local.CarritoAlmacen
import mx.uasnack.app.domain.modelo.OpcionProducto
import mx.uasnack.app.domain.modelo.Producto
import mx.uasnack.app.domain.modelo.ResultadoAgregar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal

// Prueba CarritoRepository (#231) con un DataStore real en un archivo temporal.
class CarritoRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var archivo: File
    private var alcance: CoroutineScope? = null
    private lateinit var carrito: CarritoRepository

    @Before
    fun preparar() {
        archivo = File(carpeta.root, "carrito.preferences_pb")
        abrirApp()
    }

    @After
    fun terminar() {
        cerrarApp()
    }

    // Arma el repositorio igual que UasnackApp. Llamarla otra vez simula cerrar y volver a abrir la app.
    private fun abrirApp() {
        cerrarApp()
        val nuevoAlcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        alcance = nuevoAlcance
        val dataStore = PreferenceDataStoreFactory.create(scope = nuevoAlcance, produceFile = { archivo })
        carrito = CarritoRepository(CarritoAlmacen(dataStore))
    }

    // DataStore no deja abrir dos instancias sobre el mismo archivo: se cierra la anterior primero
    private fun cerrarApp() = runBlocking {
        alcance?.coroutineContext?.get(Job)?.cancelAndJoin()
        alcance = null
    }

    @Test
    fun `agregar el mismo producto dos veces suma la cantidad`() = runBlocking {
        carrito.agregar(TORTA, cantidad = 1, extras = listOf(QUESO))
        carrito.agregar(TORTA, cantidad = 2, extras = listOf(QUESO))

        val lineas = carrito.carrito().first().lineas
        assertEquals(1, lineas.size)
        assertEquals(3, lineas[0].cantidad)
    }

    @Test
    fun `los mismos extras en distinto orden son la misma linea`() = runBlocking {
        carrito.agregar(TORTA, extras = listOf(QUESO, AGUACATE))
        carrito.agregar(TORTA, extras = listOf(AGUACATE, QUESO))

        val lineas = carrito.carrito().first().lineas
        assertEquals(1, lineas.size)
        assertEquals(2, lineas[0].cantidad)
    }

    @Test
    fun `extras distintos son lineas distintas`() = runBlocking {
        carrito.agregar(TORTA, extras = listOf(QUESO))
        carrito.agregar(TORTA, extras = listOf(AGUACATE))

        assertEquals(2, carrito.carrito().first().lineas.size)
    }

    @Test
    fun `el total local suma producto mas extras por la cantidad`() = runBlocking {
        // (45.50 + 5.00 + 8.00) × 2 = 117.00, más un agua de 20.00 = 137.00
        carrito.agregar(TORTA, cantidad = 2, extras = listOf(QUESO, AGUACATE), quitables = listOf(CEBOLLA))
        carrito.agregar(AGUA)

        val total = carrito.carrito().first().totalLocal
        assertEquals(0, BigDecimal("137.00").compareTo(total))
        assertEquals(3, carrito.carrito().first().totalArticulos)
    }

    @Test
    fun `un producto de otra tienda no se mezcla y avisa`() = runBlocking {
        carrito.agregar(TORTA)

        val resultado = carrito.agregar(CAFE_DERECHO)

        assertEquals(ResultadoAgregar.OtraTienda(idTiendaDelCarrito = 1), resultado)
        assertEquals(listOf(TORTA.id), carrito.carrito().first().lineas.map { it.idProducto })
    }

    @Test
    fun `vaciar y agregar empieza un carrito con la otra tienda`() = runBlocking {
        carrito.agregar(TORTA)

        val resultado = carrito.vaciarYAgregar(CAFE_DERECHO)

        assertEquals(ResultadoAgregar.Agregado, resultado)
        assertEquals(5, carrito.carrito().first().idTienda)
        assertEquals(1, carrito.carrito().first().lineas.size)
    }

    @Test
    fun `no deja pedir mas que las existencias`() = runBlocking {
        // Hay 3 tortas: 2 con queso y 1 sin nada ya agotan el stock, aunque sean líneas distintas
        carrito.agregar(TORTA, cantidad = 2, extras = listOf(QUESO))
        carrito.agregar(TORTA, cantidad = 1)

        val resultado = carrito.agregar(TORTA, cantidad = 1)

        assertEquals(ResultadoAgregar.SinExistencias(disponibles = 0), resultado)
        assertEquals(3, carrito.carrito().first().totalArticulos)
    }

    @Test
    fun `un producto agotado no se agrega`() = runBlocking {
        val resultado = carrito.agregar(AGUA.copy(stockHoy = 0, agotado = true))

        assertEquals(ResultadoAgregar.SinExistencias(disponibles = 0), resultado)
        assertTrue(carrito.carrito().first().vacio)
    }

    @Test
    fun `cambiar la cantidad a cero quita la linea`() = runBlocking {
        carrito.agregar(TORTA)
        carrito.agregar(AGUA)
        val claveTorta = carrito.carrito().first().lineas.first { it.idProducto == TORTA.id }.clave

        carrito.cambiarCantidad(claveTorta, 0)

        assertEquals(listOf(AGUA.id), carrito.carrito().first().lineas.map { it.idProducto })
    }

    @Test
    fun `el carrito sobrevive a cerrar y volver a abrir la app`() = runBlocking {
        carrito.agregar(TORTA, cantidad = 2, extras = listOf(QUESO))

        abrirApp()

        val linea = carrito.carrito().first().lineas.single()
        assertEquals(2, linea.cantidad)
        assertEquals(listOf("queso"), linea.extras.map { it.id })
        assertEquals(0, BigDecimal("45.50").compareTo(linea.precioUnitario))
    }

    @Test
    fun `vaciar deja el carrito vacio`() = runBlocking {
        carrito.agregar(TORTA)
        carrito.agregar(AGUA)

        carrito.vaciar()

        assertTrue(carrito.carrito().first().vacio)
    }

    private companion object {
        val QUESO = OpcionProducto("queso", "Queso extra", BigDecimal("5.00"))
        val AGUACATE = OpcionProducto("aguacate", "Aguacate", BigDecimal("8.00"))
        val CEBOLLA = OpcionProducto("cebolla", "Cebolla", null)

        val TORTA = producto(id = 10, idTienda = 1, nombre = "Torta de jamón", precio = "45.50", stock = 3)
        val AGUA = producto(id = 11, idTienda = 1, nombre = "Agua de jamaica", precio = "20.00", stock = 10)
        val CAFE_DERECHO = producto(id = 50, idTienda = 5, nombre = "Café", precio = "15.00", stock = 10)

        fun producto(id: Int, idTienda: Int, nombre: String, precio: String, stock: Int) = Producto(
            id = id,
            idTienda = idTienda,
            idCategoria = 1,
            nombre = nombre,
            descripcion = null,
            precio = BigDecimal(precio),
            stockHoy = stock,
            tiempoPrepMin = 5,
            tiempoPrepMax = 8,
            fotoUrl = null,
            agotado = false,
            extras = listOf(QUESO, AGUACATE),
            quitables = listOf(CEBOLLA)
        )
    }
}
