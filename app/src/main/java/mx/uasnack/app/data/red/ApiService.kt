package mx.uasnack.app.data.red

import mx.uasnack.app.data.red.dto.CodigoEnviadoDto
import mx.uasnack.app.data.red.dto.FacultadDto
import mx.uasnack.app.data.red.dto.ListaDto
import mx.uasnack.app.data.red.dto.LoginRequest
import mx.uasnack.app.data.red.dto.ObjetoDto
import mx.uasnack.app.data.red.dto.ProductoDto
import mx.uasnack.app.data.red.dto.ReenviarCodigoRequest
import mx.uasnack.app.data.red.dto.RegistroRequest
import mx.uasnack.app.data.red.dto.SesionDto
import mx.uasnack.app.data.red.dto.TiendaDto
import mx.uasnack.app.data.red.dto.VerificarRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Endpoints de la API (/api/v1). Las rutas no llevan "/" al inicio porque se pegan a API_BASE_URL,
// que ya termina en "/api/v1/".
//
// Si la API responde con error, ErrorInterceptor lanza ErrorApi: estas funciones solo regresan
// cuando la respuesta fue exitosa.
//
// Solo están los endpoints que ya existen en el backend o que ya tienen su forma acordada
// (catálogo, #46). Los demás del §14 (pedidos, pagos...) se agregan aquí cuando existan en la API.
interface ApiService {

    @GET("facultades")
    suspend fun obtenerFacultades(): ListaDto<FacultadDto>

    // 201 con el mensaje y los segundos para poder reenviar el código
    @POST("auth/registro")
    suspend fun registro(@Body body: RegistroRequest): CodigoEnviadoDto

    @POST("auth/verificar")
    suspend fun verificar(@Body body: VerificarRequest): SesionDto

    @POST("auth/reenviar-codigo")
    suspend fun reenviarCodigo(@Body body: ReenviarCodigoRequest): CodigoEnviadoDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): SesionDto

    // 204 sin contenido; necesita el token (lo agrega AuthInterceptor)
    @POST("auth/logout")
    suspend fun logout()

    // --- Catálogo (#228). Los endpoints son de la #46 del backend; necesitan sesión. ---

    // Tiendas de todas las facultades, con las de la facultad del alumno primero
    @GET("tiendas")
    suspend fun obtenerTiendas(): ListaDto<TiendaDto>

    // Todos los productos de la tienda, agotados incluidos. El filtro por categoría se hace en la app,
    // sobre la caché, para que funcione también sin red.
    @GET("tiendas/{idTienda}/productos")
    suspend fun obtenerProductos(@Path("idTienda") idTienda: Int): ListaDto<ProductoDto>

    @GET("productos/{idProducto}")
    suspend fun obtenerProducto(@Path("idProducto") idProducto: Int): ObjetoDto<ProductoDto>
}
