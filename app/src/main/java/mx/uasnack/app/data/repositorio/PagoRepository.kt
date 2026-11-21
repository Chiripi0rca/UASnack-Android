package mx.uasnack.app.data.repositorio

import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.dto.aDominio
import mx.uasnack.app.domain.modelo.MetodoPago
import mx.uasnack.app.domain.modelo.Pago

// ESQUELETO (Sprint 04). Única puerta de la app para los pagos del alumno.
// Cada función la completa su tarea:
//   pagos() → #186 (Daniel), con paginación
//   metodos(), agregarMetodo(), eliminarMetodo() → #190 (Josef)
// Por ahora regresan DATOS DE EJEMPLO para que Historial (#72) y Métodos de pago (#191) se puedan
// hacer sin esperar al backend. Quien la complete cambia SOLO el contenido.
@Suppress("UNUSED_PARAMETER", "unused")
class PagoRepository(
    private val api: ApiService
) {

    // GET /pagos
    suspend fun pagos(pagina: Int = 1): Result<List<Pago>> = runCatching {
        api.obtenerPagos(pagina = pagina).datos.map { it.aDominio() }
    }



    // GET /pagos/metodos
    suspend fun metodos(): Result<List<MetodoPago>> = intentar {
        api.obtenerMetodosPago()
            .datos
            .map { it.aDominio() }
    }

    // POST /pagos/metodos
    // Regresa el client_secret para presentar Stripe PaymentSheet.
    suspend fun agregarMetodo(): Result<String> = intentar {
        api.agregarMetodoPago()
            .datos
            .clientSecret
    }

    // DELETE /pagos/metodos/{id}
    suspend fun eliminarMetodo(id: String): Result<Unit> = intentar {
        api.eliminarMetodoPago(id)
    }

    // Conserva los errores originales del servidor dentro de Result.failure.
    private suspend fun <T> intentar(
        bloque: suspend () -> T
    ): Result<T> = try {
        Result.success(bloque())
    } catch (e: ErrorApi) {
        Result.failure(e)
    }

    private companion object {
        val PAGOS_DE_EJEMPLO = listOf(
            Pago(
                id = 12,
                codigoPedido = "TA-2451",
                monto = "85.00",
                estado = "pagado",
                creadoEn = "2026-11-09 11:21:00"
            )
        )
    }
}
