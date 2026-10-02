package mx.uasnack.app.data.red.interceptores

import okhttp3.Interceptor
import okhttp3.Response

// Detecta que la sesión venció (el backend respondió 401 a una petición que llevaba token) y avisa
// para limpiar la sesión y regresar al login.
//
// Ojo: no todo 401 es sesión vencida. El 401 del login significa "correo o contraseña incorrectos"
// y esa petición no lleva token, por eso solo se avisa si la petición llevaba Authorization.
//
// Debe ir DESPUÉS de AuthInterceptor en ModuloRed, para ver la petición ya con el token puesto.
class SesionInterceptor(
    private val alVencerSesion: () -> Unit
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val respuesta = chain.proceed(peticion)

        if (respuesta.code == 401 && peticion.header("Authorization") != null) {
            alVencerSesion()
        }

        // La respuesta sigue su camino: ErrorInterceptor la convierte en ErrorApi(NO_AUTENTICADO)
        return respuesta
    }
}
