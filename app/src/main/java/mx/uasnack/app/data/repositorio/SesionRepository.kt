package mx.uasnack.app.data.repositorio

import kotlinx.coroutines.flow.Flow
import mx.uasnack.app.data.local.SesionAlmacen
import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.dto.LoginRequest
import mx.uasnack.app.data.red.dto.RegistroRequest
import mx.uasnack.app.data.red.dto.SesionDto
import mx.uasnack.app.data.red.dto.VerificarRequest
import mx.uasnack.app.data.red.dto.aDominio
import mx.uasnack.app.domain.modelo.Usuario

// Única puerta de la app para la sesión: login, registro, verificación y cerrar sesión (#161).
// Los ViewModels no llaman a la API ni a DataStore directamente: le piden a este repositorio.
// Los errores regresan como Result.failure con el ErrorApi que armó ErrorInterceptor (código, mensaje del servidor, campos del 422 y Retry-After del 429). El ViewModel decide qué mostrar.
class SesionRepository(
    private val api: ApiService,
    private val almacen: SesionAlmacen
) {

    // Token guardado o null. La navegación lo usa para decidir si la app abre en el login o en Tiendas.
    // Es un Flow: quien lo escuche se entera también cuando se inicia o se cierra sesión.
    fun tokenGuardado(): Flow<String?> = almacen.token

    // POST /auth/login. Si el correo y la contraseña son correctos, guarda el token y regresa el usuario.
    // Errores que puede regresar: CREDENCIALES_INVALIDAS (401), CORREO_NO_VERIFICADO (403),
    // VALIDACION (422), DEMASIADOS_INTENTOS (429) o SIN_CONEXION.
    suspend fun login(correo: String, password: String): Result<Usuario> = guardandoSesion {
        api.login(LoginRequest(correoInstitucional = correo, password = password))
    }

    // POST /auth/registro. Crea la cuenta pero NO inicia sesión: eso pasa hasta verificar el correo.
    // Regresa los segundos que hay que esperar para poder reenviar el código (reenviar_en_segundos).
    suspend fun registro(
        nombre: String,
        apellido: String,
        correo: String,
        idFacultad: Int?,
        password: String,
        telefono: String
    ): Result<Int> = try {
        val respuesta = api.registro(
            RegistroRequest(
                nombre = nombre,
                apellido = apellido,
                correoInstitucional = correo,
                idFacultad = idFacultad,
                password = password,
                telefono = telefono,
                // Siempre true: la pantalla solo deja crear la cuenta con la casilla marcada
                aceptoTerminos = true
            )
        )
        Result.success(respuesta.reenviarEnSegundos)
    } catch (e: ErrorApi) {
        // Por ejemplo VALIDACION (422) con el error de cada campo en e.campos
        Result.failure(e)
    }

    // POST /auth/verificar. Si el código es correcto, el backend marca el correo como verificado
    // y ya entrega la sesión: se guarda el token igual que en el login.
    suspend fun verificar(correo: String, codigo: String): Result<Usuario> = guardandoSesion {
        api.verificar(VerificarRequest(correoInstitucional = correo, codigo = codigo))
    }

    // POST /auth/logout. Revoca el token en el servidor y lo borra del teléfono.
    // Si el servidor no responde (sin señal, token ya vencido) se borra igual:
    // el usuario pidió salir. Igual que la PWA.
    suspend fun cerrarSesion() {
        try {
            api.logout()
        } catch (_: ErrorApi) {
            // El token se queda sin usar en el servidor y vence solo a los 7 días
        }
        almacen.borrar()
    }

    // Lo comparten login y verificar, que responden igual ({ token, usuario }):
    // llama a la API y, si responde bien, guarda el token antes de regresar el usuario.
    // Si la API falla no se guarda nada y se regresa el error.
    private suspend fun guardandoSesion(llamada: suspend () -> SesionDto): Result<Usuario> = try {
        val sesion = llamada()
        almacen.guardar(sesion.token)
        // aDominio() traduce el DTO (forma del JSON) al modelo Usuario que usan las pantallas
        Result.success(sesion.usuario.aDominio())
    } catch (e: ErrorApi) {
        Result.failure(e)
    }
}
