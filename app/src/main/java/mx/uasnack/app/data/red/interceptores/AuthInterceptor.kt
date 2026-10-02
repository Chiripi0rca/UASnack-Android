package mx.uasnack.app.data.red.interceptores

import okhttp3.Interceptor
import okhttp3.Response

// Adjunta "Authorization: Bearer <token>" a cada petición cuando hay sesión.
//
// Recibe una función que da el token en lugar de leer DataStore aquí, para no depender de cómo se
// guarde la sesión (#161). Esa función se llama en el hilo de OkHttp, así que debe responder rápido:
// SesionAlmacen debe tener el token en memoria y no leer el disco en cada petición.
class AuthInterceptor(
    private val tokenActual: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val token = tokenActual()

        // Sin sesión (login, registro...) la petición sale tal cual
        if (token.isNullOrBlank()) return chain.proceed(peticion)

        return chain.proceed(
            peticion.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
