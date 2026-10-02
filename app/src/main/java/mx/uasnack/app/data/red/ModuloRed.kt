package mx.uasnack.app.data.red

import kotlinx.serialization.json.Json
import mx.uasnack.app.BuildConfig
import mx.uasnack.app.data.red.interceptores.AuthInterceptor
import mx.uasnack.app.data.red.interceptores.ErrorInterceptor
import mx.uasnack.app.data.red.interceptores.SesionInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Arma el cliente HTTP de la app: OkHttp con los interceptores, Retrofit y el convertidor de JSON.
// Se crea una sola vez (la crea quien arme los repositorios) y se reutiliza en toda la app.
object ModuloRed {

    // Configuración del JSON:
    // - ignoreUnknownKeys: si el backend agrega un campo nuevo, la app no truena.
    // - coerceInputValues: si llega null en un campo con valor por defecto, se usa el valor por defecto.
    // - explicitNulls = false: no manda los campos null en las peticiones.
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    // tokenActual: devuelve el token guardado o null si no hay sesión (lo da SesionAlmacen, #161).
    // alVencerSesion: se llama cuando el backend rechaza el token (401); debe borrar la sesión.
    fun crearApiService(
        tokenActual: () -> String?,
        alVencerSesion: () -> Unit,
        urlBase: String = BuildConfig.API_BASE_URL
    ): ApiService {
        val cliente = OkHttpClient.Builder()
            // El orden importa. OkHttp los ejecuta del primero al último al salir la petición:
            // 1. ErrorInterceptor: el más externo, convierte en ErrorApi todo lo que falle adentro.
            // 2. AuthInterceptor: pone el token.
            // 3. SesionInterceptor: ve la petición ya con token, para distinguir un 401 de sesión vencida.
            .addInterceptor(ErrorInterceptor(json))
            // Pide JSON siempre, igual que la PWA: así Laravel nunca responde con una página HTML
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Accept", "application/json").build())
            }
            .addInterceptor(AuthInterceptor(tokenActual))
            .addInterceptor(SesionInterceptor(alVencerSesion))
            // En la red de la universidad las conexiones pueden ser lentas: 20 s antes de rendirse
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
