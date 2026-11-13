package mx.uasnack.app.data.repositorio

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
    suspend fun metodos(): Result<List<MetodoPago>> = Result.success(METODOS_DE_EJEMPLO)

    // POST /pagos/metodos: regresa el client_secret para que PaymentSheet guarde la tarjeta
    suspend fun agregarMetodo(): Result<String> = Result.success("seti_ejemplo_secret_ejemplo")

    // DELETE /pagos/metodos/{id}
    suspend fun eliminarMetodo(id: String): Result<Unit> = Result.success(Unit)

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
        val METODOS_DE_EJEMPLO = listOf(
            MetodoPago(id = "pm_ejemplo", marca = "visa", ultimos4 = "4242", vence = "12/28")
        )
    }
}
