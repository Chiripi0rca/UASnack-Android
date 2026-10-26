package mx.uasnack.app.data.repositorio

import kotlinx.coroutines.runBlocking
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

// Prueba PedidoRepository.franjas() (#173) con una API falsa (MockWebServer).
class PedidoRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: PedidoRepository

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val api = ModuloRed.crearApiService(
            tokenActual = { "1|abc" },
            alVencerSesion = {},
            urlBase = servidor.url("/api/v1/").toString()
        )
        repositorio = PedidoRepository(api)
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    @Test
    fun `las franjas llegan del servidor con la forma acordada`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(FRANJAS))

        val franjas = repositorio.franjas(idTienda = 3, minutos = 18).getOrThrow()

        assertEquals(listOf("11:00", "11:10"), franjas.map { it.inicio })
        assertEquals("11:10", franjas[0].fin)
        assertEquals("2026-10-26", franjas[0].fecha)
        assertEquals(24, franjas[0].minutosRestantes)
        assertTrue(franjas[0].disponible)
        assertNull(franjas[0].descuento)
        // La llena no se oculta: llega marcada
        assertFalse(franjas[1].disponible)
        assertEquals(10, franjas[1].descuento)
    }

    @Test
    fun `pide las franjas de esa tienda con los minutos del carrito`() = runBlocking {
        servidor.enqueue(MockResponse().setBody(FRANJAS))

        repositorio.franjas(idTienda = 3, minutos = 18)

        assertEquals("/api/v1/tiendas/3/franjas?minutos=18&limite=8", servidor.takeRequest().path)
    }

    @Test
    fun `sin red regresa el error y no truena`() = runBlocking {
        servidor.shutdown()

        val resultado = repositorio.franjas(idTienda = 3, minutos = 18)

        assertTrue(resultado.isFailure)
        assertEquals("SIN_CONEXION", (resultado.exceptionOrNull() as ErrorApi).codigo)
    }

    @Test
    fun `un 404 regresa el error del servidor`() = runBlocking {
        servidor.enqueue(
            MockResponse().setResponseCode(404)
                .setBody("""{"error":{"codigo":"NO_ENCONTRADO","mensaje":"No encontramos lo que buscas."}}""")
        )

        val resultado = repositorio.franjas(idTienda = 99, minutos = 18)

        assertEquals("NO_ENCONTRADO", (resultado.exceptionOrNull() as ErrorApi).codigo)
    }

    private companion object {
        const val FRANJAS = """
            {"datos":[
              {"fecha":"2026-10-26","inicio":"11:00","fin":"11:10","minutos_restantes":24,"disponible":true,"descuento":null},
              {"fecha":"2026-10-26","inicio":"11:10","fin":"11:20","minutos_restantes":4,"disponible":false,"descuento":10}
            ],"meta":{"total":2}}
        """
    }
}
