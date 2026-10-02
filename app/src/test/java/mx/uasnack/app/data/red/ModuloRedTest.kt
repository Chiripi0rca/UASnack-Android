package mx.uasnack.app.data.red

import kotlinx.coroutines.runBlocking
import mx.uasnack.app.data.red.dto.LoginRequest
import mx.uasnack.app.data.red.dto.aDominio
import mx.uasnack.app.domain.modelo.Rol
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

// Prueba la capa de red contra un servidor falso (MockWebServer) que responde lo mismo que el backend.
class ModuloRedTest {

    private lateinit var servidor: MockWebServer
    private var token: String? = null
    private var sesionVencida = false

    private lateinit var api: ApiService

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        token = null
        sesionVencida = false
        api = ModuloRed.crearApiService(
            tokenActual = { token },
            alVencerSesion = { sesionVencida = true },
            urlBase = servidor.url("/api/v1/").toString()
        )
    }

    @After
    fun terminar() {
        servidor.shutdown()
    }

    // Ejecuta la llamada y regresa el ErrorApi que debe lanzar; falla la prueba si no lanza nada
    private fun errorDe(llamada: suspend () -> Unit): ErrorApi = runBlocking {
        try {
            llamada()
            fail("Se esperaba ErrorApi")
            throw IllegalStateException()
        } catch (e: ErrorApi) {
            e
        }
    }

    private fun responder(codigo: Int, cuerpo: String) {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    @Test
    fun `login exitoso lee la sesion y traduce snake_case`() = runBlocking {
        responder(
            200,
            """{"token":"1|abc","usuario":{"id":7,"nombre":"Ana","apellido":"López",
               "correo_institucional":"ana@uas.edu.mx","id_facultad":1,"rol":"usuario",
               "reputacion":100,"correo_verificado_en":"2026-10-01T10:00:00.000000Z","extra":"ignorado"}}"""
        )

        val sesion = api.login(LoginRequest("ana@uas.edu.mx", "secreto123"))
        val usuario = sesion.usuario.aDominio()

        assertEquals("1|abc", sesion.token)
        assertEquals("ana@uas.edu.mx", usuario.correoInstitucional)
        assertEquals(Rol.USUARIO, usuario.rol)
        assertTrue(usuario.correoVerificado)

        // El cuerpo enviado va en snake_case y la petición pide JSON
        val peticion = servidor.takeRequest()
        assertEquals("/api/v1/auth/login", peticion.path)
        assertEquals("application/json", peticion.getHeader("Accept"))
        assertTrue(peticion.body.readUtf8().contains("\"correo_institucional\":\"ana@uas.edu.mx\""))
    }

    @Test
    fun `sin sesion no manda Authorization y con sesion manda Bearer`() = runBlocking {
        responder(200, """{"datos":[],"meta":{"total":0}}""")
        api.obtenerFacultades()
        assertNull(servidor.takeRequest().getHeader("Authorization"))

        token = "1|abc"
        servidor.enqueue(MockResponse().setResponseCode(204))
        api.logout()
        assertEquals("Bearer 1|abc", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `lista de facultades ignora meta`() = runBlocking {
        responder(200, """{"datos":[{"id":1,"nombre":"Facultad de Informática Culiacán","clave":"FIC"}],"meta":{"total":1}}""")

        val facultades = api.obtenerFacultades().datos.map { it.aDominio() }

        assertEquals(1, facultades.size)
        assertEquals("Facultad de Informática Culiacán", facultades[0].nombre)
    }

    @Test
    fun `401 del login es credenciales invalidas y no cierra la sesion`() {
        responder(401, """{"error":{"codigo":"CREDENCIALES_INVALIDAS","mensaje":"Correo o contraseña incorrectos."}}""")

        val error = errorDe { api.login(LoginRequest("ana@uas.edu.mx", "mala")) }

        assertEquals(401, error.status)
        assertEquals(ErrorApi.CREDENCIALES_INVALIDAS, error.codigo)
        assertEquals("Correo o contraseña incorrectos.", error.mensaje)
        assertFalse(sesionVencida)
    }

    @Test
    fun `401 con token avisa que la sesion vencio`() {
        token = "1|vencido"
        responder(401, """{"error":{"codigo":"NO_AUTENTICADO","mensaje":"Tu sesión expiró. Inicia sesión de nuevo."}}""")

        val error = errorDe { api.logout() }

        assertEquals(ErrorApi.NO_AUTENTICADO, error.codigo)
        assertTrue(sesionVencida)
    }

    @Test
    fun `422 trae el primer error de cada campo`() {
        responder(
            422,
            """{"error":{"codigo":"VALIDACION","mensaje":"Revisa los datos marcados.",
               "campos":{"correo_institucional":["El correo ya está registrado.","Otro"],"password":["Mínimo 8."]}}}"""
        )

        val error = errorDe { api.login(LoginRequest("", "")) }

        assertEquals(ErrorApi.VALIDACION, error.codigo)
        assertEquals("El correo ya está registrado.", error.primerErrorPorCampo["correo_institucional"])
        assertEquals("Mínimo 8.", error.primerErrorPorCampo["password"])
    }

    @Test
    fun `429 lee los segundos de Retry-After`() {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "43")
                .setBody("""{"error":{"codigo":"DEMASIADOS_INTENTOS","mensaje":"Demasiados intentos."}}""")
        )

        val error = errorDe { api.login(LoginRequest("ana@uas.edu.mx", "x")) }

        assertEquals(ErrorApi.DEMASIADOS_INTENTOS, error.codigo)
        assertEquals(43, error.reintentarEn)
    }

    @Test
    fun `error sin formato del proyecto usa el mensaje generico`() {
        responder(500, "<html>Server Error</html>")

        val error = errorDe { api.obtenerFacultades() }

        assertEquals(500, error.status)
        assertEquals(ErrorApi.ERROR_DESCONOCIDO, error.codigo)
        assertEquals(ErrorApi.MENSAJE_DESCONOCIDO, error.mensaje)
    }

    @Test
    fun `servidor apagado es sin conexion`() {
        servidor.shutdown()

        val error = errorDe { api.obtenerFacultades() }

        assertEquals(0, error.status)
        assertEquals(ErrorApi.SIN_CONEXION, error.codigo)
        assertEquals(ErrorApi.MENSAJE_SIN_CONEXION, error.mensaje)
    }
}
