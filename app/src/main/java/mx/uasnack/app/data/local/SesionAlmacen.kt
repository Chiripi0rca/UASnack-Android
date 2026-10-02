package mx.uasnack.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

// Archivo de DataStore donde vive la sesión (se guarda como "sesion.preferences_pb" en el teléfono).
// Se declara una sola vez para toda la app: DataStore truena si se abren dos instancias sobre el mismo archivo. UasnackApp es quien lo usa.
val Context.dataStoreSesion: DataStore<Preferences> by preferencesDataStore(name = "sesion")

// Guarda el token de Sanctum en DataStore para que la sesión sobreviva al cerrar la app (#161).
// También tiene el token en memoria: AuthInterceptor lo pide en cada petición desde el hilo de OkHttp y no debe leer el disco cada vez.
class SesionAlmacen(
    private val dataStore: DataStore<Preferences>,
    alcance: CoroutineScope
) {

    // Copia en memoria del token guardado.
    // @Volatile: se escribe desde una corrutina y se lee desde el hilo de OkHttp;
    // así cada hilo ve siempre el valor más reciente.
    @Volatile
    private var tokenEnMemoria: String? = null

    // false hasta la primera lectura del disco al abrir la app
    @Volatile
    private var leido = false

    // Token guardado, o null si no hay sesión. Emite cada vez que cambia (al entrar o al salir).
    val token: Flow<String?> = dataStore.data
        // Si el archivo no se puede leer (dañado o sin espacio), se trata como si no hubiera sesión
        // en vez de tumbar la app: el usuario solo tendrá que iniciar sesión otra vez.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        // De todo lo guardado en DataStore solo interesa el token
        .map { it[CLAVE_TOKEN] }
        // No vuelve a emitir si el token es el mismo que el anterior
        .distinctUntilChanged()

    init {
        // Al crearse, empieza a escuchar el disco y copia cada cambio a memoria.
        // Sigue escuchando mientras la app esté abierta (vive en "alcance").
        alcance.launch {
            token.collect {
                tokenEnMemoria = it
                leido = true
            }
        }
    }

    // Token para la petición que va a salir, o null si no hay sesión. La usa AuthInterceptor.
    // Si la app apenas arrancó y todavía no se lee el disco, espera esa primera lectura
    // (pasa una sola vez y en el hilo de OkHttp, nunca en el de la pantalla).
    fun tokenActual(): String? {
        if (!leido) return runBlocking { token.first() }
        return tokenEnMemoria
    }

    // Guarda el token que regresó la API al iniciar sesión o al verificar el correo
    suspend fun guardar(token: String) {
        // Primero en memoria, para que la siguiente petición ya salga con el token
        tokenEnMemoria = token
        // Después en disco, para que siga ahí la próxima vez que se abra la app
        dataStore.edit { it[CLAVE_TOKEN] = token }
    }

    // Borra el token al cerrar sesión o cuando el backend dice que la sesión venció
    suspend fun borrar() {
        // Primero en memoria, para que ninguna petición vuelva a salir con el token viejo
        tokenEnMemoria = null
        dataStore.edit { it.remove(CLAVE_TOKEN) }
    }

    private companion object {
        // Nombre con el que se guarda el token dentro del archivo de DataStore
        val CLAVE_TOKEN = stringPreferencesKey("token")
    }
}
