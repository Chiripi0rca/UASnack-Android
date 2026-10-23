package mx.uasnack.app.data.red

import mx.uasnack.app.data.red.dto.CambiarPasswordRequest
import mx.uasnack.app.data.red.dto.CodigoEnviadoDto
import mx.uasnack.app.data.red.dto.EstimadoDto
import mx.uasnack.app.data.red.dto.FacultadDto
import mx.uasnack.app.data.red.dto.FranjaDto
import mx.uasnack.app.data.red.dto.ListaDto
import mx.uasnack.app.data.red.dto.LoginRequest
import mx.uasnack.app.data.red.dto.ObjetoDto
import mx.uasnack.app.data.red.dto.OlvidePasswordRequest
import mx.uasnack.app.data.red.dto.PerfilRespuestaDto
import mx.uasnack.app.data.red.dto.ProductoDto
import mx.uasnack.app.data.red.dto.ReenviarCodigoRequest
import mx.uasnack.app.data.red.dto.RegistroRequest
import mx.uasnack.app.data.red.dto.RestablecerPasswordRequest
import mx.uasnack.app.data.red.dto.SesionDto
import mx.uasnack.app.data.red.dto.TiendaDto
import mx.uasnack.app.data.red.dto.VerificarRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    // --- Sprint 03 (esqueleto). La forma del JSON está en el documento de turnos del Sprint 03. ---

    // Próximas franjas de la tienda para un carrito de "minutos" de cocina (#53 del backend).
    // La usa PedidoRepository.franjas() (#173).
    @GET("tiendas/{idTienda}/franjas")
    suspend fun obtenerFranjas(
        @Path("idTienda") idTienda: Int,
        @Query("minutos") minutos: Int,
        @Query("limite") limite: Int = 8
    ): ListaDto<FranjaDto>

    // Tiempo de espera del modo "voy saliendo" (#57 del backend). La usa PedidoRepository.espera() (#175).
    @GET("tiendas/{idTienda}/espera")
    suspend fun obtenerEspera(
        @Path("idTienda") idTienda: Int,
        @Query("minutos") minutos: Int
    ): ObjetoDto<EstimadoDto>

    // Perfil con datos frescos del servidor (#218 del backend). Lo usa Perfil (#220).
    @GET("auth/yo")
    suspend fun obtenerPerfil(): PerfilRespuestaDto

    // HU-33. Las tres responden 204 sin contenido.
    @POST("auth/olvide-password")
    suspend fun olvidePassword(@Body body: OlvidePasswordRequest)

    @POST("auth/restablecer-password")
    suspend fun restablecerPassword(@Body body: RestablecerPasswordRequest)

    // Necesita el token (lo agrega AuthInterceptor)
    @POST("auth/cambiar-password")
    suspend fun cambiarPassword(@Body body: CambiarPasswordRequest)
}
