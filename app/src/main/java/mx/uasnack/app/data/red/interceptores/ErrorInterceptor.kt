package mx.uasnack.app.data.red.interceptores

import kotlinx.serialization.json.Json
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.dto.RespuestaErrorDto
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

// Convierte cualquier respuesta de error de la API en ErrorApi, con el mensaje que manda el servidor.
// Así los repositorios no leen JSON de error ni códigos HTTP: solo atrapan ErrorApi.
//
// Debe ser el PRIMER interceptor en ModuloRed (el más externo) para atrapar también los errores
// de conexión de los demás.
class ErrorInterceptor(
    private val json: Json
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val respuesta = try {
            chain.proceed(chain.request())
        } catch (e: ErrorApi) {
            throw e
        } catch (e: IOException) {
            // Sin internet, servidor apagado o tiempo de espera agotado: no hay respuesta que leer
            throw ErrorApi(
                status = 0,
                codigo = ErrorApi.SIN_CONEXION,
                mensaje = ErrorApi.MENSAJE_SIN_CONEXION,
                causa = e
            )
        }

        if (respuesta.isSuccessful) return respuesta

        // A partir de aquí es un error: se lee el cuerpo y se cierra la respuesta antes de lanzar
        val cuerpo = respuesta.body?.string().orEmpty()
        val retryAfter = respuesta.header("Retry-After")?.toIntOrNull()
        val status = respuesta.code
        respuesta.close()

        // Si el cuerpo no tiene la forma {error:{...}} (por ejemplo un 500 de HTML), se usa un mensaje genérico
        val error = runCatching { json.decodeFromString<RespuestaErrorDto>(cuerpo).error }.getOrNull()

        throw ErrorApi(
            status = status,
            codigo = error?.codigo ?: ErrorApi.ERROR_DESCONOCIDO,
            mensaje = error?.mensaje ?: ErrorApi.MENSAJE_DESCONOCIDO,
            campos = error?.campos ?: emptyMap(),
            reintentarEn = retryAfter
        )
    }
}
