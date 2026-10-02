package mx.uasnack.app.data.repositorio

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.uasnack.app.data.local.SesionAlmacen
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.ModuloRed
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Prueba SesionRepository de punta a punta: API falsa (MockWebServer) y DataStore real en un archivo temporal.
class SesionRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var servidor: MockWebServer
    private lateinit var archivo: File
    private var alcance: CoroutineScope? = null
    private lateinit var almacen: SesionAlmacen
    private lateinit var repositorio: SesionRepository

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        archivo = File(carpeta.root, "sesion.preferences_pb")
        abrirApp()
    }

    @After
    fun terminar() {
        cerrarApp()
        servidor.shutdown()
    }

    // Arma las piezas igual que UasnackApp. Llamarla otra vez simula cerrar y volver a abrir la app.
    private fun abrirApp() {
        cerrarApp()
        val nuevoAlcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        alcance = nuevoAlcance
        val dataStore = PreferenceDataStoreFactory.create(scope = nuevoAlcance, produceFile = { archivo })
        almacen = SesionAlmacen(dataStore, nuevoAlcance)
        val api = ModuloRed.crearApiService(
            tokenActual = almacen::tokenActual,
            alVencerSesion = {},
            urlBase = servidor.url("/api/v1/").toString()
        )
        repositorio = SesionRepository(api, almacen)
    }

    // DataStore no permite dos instancias sobre el mismo archivo: se espera a que la anterior termine
    private fun cerrarApp() = runBlocking {
        alcance?.coroutineContext?.get(Job)?.cancelAndJoin()
    }

    private fun responder(codigo: Int, cuerpo: String) {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    // Al entrar bien, el token queda guardado en DataStore y en memoria, y se regresa el usuario.
    @Test
    fun `login exitoso guarda el token y regresa el usuario`() = runBlocking {
        responder(200, SESION)

        val usuario = repositorio.login("ana@info.uas.edu.mx", "secreta123").getOrThrow()

        assertEquals("Ana", usuario.nombre)
        assertEquals("1|abc", repositorio.tokenGuardado().first())
        assertEquals("1|abc", almacen.tokenActual())
    }

    // Si el backend rechaza el login, se regresa su código y su mensaje tal cual y no se guarda ningún token.
    @Test
    fun `login con credenciales invalidas regresa el error del servidor y no guarda nada`() = runBlocking {
        responder(401, """{"error":{"codigo":"CREDENCIALES_INVALIDAS","mensaje":"Correo o contraseña incorrectos."}}""")

        val error = repositorio.login("ana@info.uas.edu.mx", "mala").exceptionOrNull() as ErrorApi

        assertEquals(ErrorApi.CREDENCIALES_INVALIDAS, error.codigo)
        assertEquals("Correo o contraseña incorrectos.", error.mensaje)
        assertNull(repositorio.tokenGuardado().first())
    }

    // Lo principal de la tarea: después de cerrar y abrir la app, el token sigue guardado.
    @Test
    fun `la sesion sobrevive a cerrar y volver a abrir la app`() = runBlocking {
        responder(200, SESION)
        repositorio.login("ana@info.uas.edu.mx", "secreta123")

        abrirApp()

        assertEquals("1|abc", repositorio.tokenGuardado().first())
        assertEquals("1|abc", almacen.tokenActual())
    }

    // Con sesión iniciada, AuthInterceptor toma el token del almacén y lo pone en la siguiente petición.
    @Test
    fun `con sesion las peticiones salen con el token`() = runBlocking {
        responder(200, SESION)
        repositorio.login("ana@info.uas.edu.mx", "secreta123")
        servidor.takeRequest()

        responder(204, "")
        repositorio.cerrarSesion()

        assertEquals("Bearer 1|abc", servidor.takeRequest().getHeader("Authorization"))
    }

    // Aunque el servidor falle al cerrar sesión, el token se borra del teléfono.
    @Test
    fun `cerrar sesion borra el token aunque el servidor falle`() = runBlocking {
        responder(200, SESION)
        repositorio.login("ana@info.uas.edu.mx", "secreta123")

        responder(500, "<html>Server Error</html>")
        repositorio.cerrarSesion()

        assertNull(repositorio.tokenGuardado().first())
        assertNull(almacen.tokenActual())
    }

    // Verificar el correo también inicia sesión: guarda el token igual que el login.
    @Test
    fun `verificar guarda el token igual que el login`() = runBlocking {
        responder(200, SESION)

        repositorio.verificar("ana@info.uas.edu.mx", "123456").getOrThrow()

        assertEquals("1|abc", repositorio.tokenGuardado().first())
    }

    // Registrarse no inicia sesión; regresa los segundos para reenviar y manda acepto_terminos = true.
    @Test
    fun `registro regresa los segundos para reenviar y no inicia sesion`() = runBlocking {
        responder(201, """{"mensaje":"Usuario registrado exitosamente.","reenviar_en_segundos":60}""")

        val segundos = repositorio.registro(
            nombre = "Ana",
            apellido = "López",
            correo = "ana@info.uas.edu.mx",
            idFacultad = 1,
            password = "secreta123",
            telefono = "6671234567"
        ).getOrThrow()

        assertEquals(60, segundos)
        assertNull(repositorio.tokenGuardado().first())
        assertTrue(servidor.takeRequest().body.readUtf8().contains("\"acepto_terminos\":true"))
    }

    private companion object {
        // Respuesta de /auth/login y /auth/verificar, igual que la manda el backend
        const val SESION = """{"token":"1|abc","usuario":{"id":7,"nombre":"Ana","apellido":"López",
            "correo_institucional":"ana@info.uas.edu.mx","id_facultad":1,"rol":"usuario","reputacion":100,
            "correo_verificado_en":"2026-10-01T10:00:00.000000Z"}}"""
    }
}
