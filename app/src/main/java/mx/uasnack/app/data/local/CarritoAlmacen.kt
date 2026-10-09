package mx.uasnack.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

// Archivo de DataStore donde vive el carrito (se guarda como "carrito.preferences_pb" en el teléfono).
// Se declara una sola vez: DataStore truena si se abren dos instancias sobre el mismo archivo.
val Context.dataStoreCarrito: DataStore<Preferences> by preferencesDataStore(name = "carrito")

// Guarda el carrito en el teléfono para que sobreviva a girar la pantalla y a cerrar la app (#231).
//
// Va en DataStore y no en Room a propósito: la base de Room es caché y se borra completa cuando
// cambia de versión. El carrito del alumno no se debe perder por eso.
class CarritoAlmacen(private val dataStore: DataStore<Preferences>) {

    // Las líneas guardadas. Emite otra vez cada que el carrito cambia.
    val lineas: Flow<List<LineaGuardada>> = dataStore.data
        // Archivo dañado o sin espacio: se trata como carrito vacío en lugar de tumbar la app
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { leer(it) }

    // Lee el carrito, deja que "cambio" calcule el nuevo y lo guarda, todo en un solo paso:
    // si se tocan dos botones muy rápido, el segundo cambio parte del resultado del primero.
    // Regresa lo que regrese "cambio" (por ejemplo, si se pudo agregar o no).
    suspend fun <T> modificar(cambio: (List<LineaGuardada>) -> Pair<List<LineaGuardada>, T>): T {
        var respuesta: T? = null
        dataStore.edit { preferencias ->
            val (nuevas, resultado) = cambio(leer(preferencias))
            preferencias[CLAVE_LINEAS] = json.encodeToString(serializador, nuevas)
            respuesta = resultado
        }
        @Suppress("UNCHECKED_CAST")
        return respuesta as T
    }

    private fun leer(preferencias: Preferences): List<LineaGuardada> =
        preferencias[CLAVE_LINEAS]
            ?.let { texto -> runCatching { json.decodeFromString(serializador, texto) }.getOrNull() }
            .orEmpty()

    // Forma en que se guarda cada línea. Precios como texto ("45.50") para no perder centavos.
    @Serializable
    data class LineaGuardada(
        val clave: String,
        val idProducto: Int,
        val idTienda: Int,
        val nombre: String,
        val precioUnitario: String,
        val cantidad: Int,
        val extras: List<OpcionGuardada>,
        val quitables: List<OpcionGuardada>
    )

    @Serializable
    data class OpcionGuardada(val id: String, val nombre: String, val precio: String? = null)

    private companion object {
        val CLAVE_LINEAS = stringPreferencesKey("lineas")
        val json = Json { ignoreUnknownKeys = true }
        val serializador = ListSerializer(LineaGuardada.serializer())
    }
}
