package mx.uasnack.app.data.repositorio

import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.dto.FranjaDto
import mx.uasnack.app.domain.modelo.Estimado
import mx.uasnack.app.domain.modelo.Franja

// Única puerta de la app para lo que pasa al pedir (Sprint 03).
// Cada función la completa su tarea:
//   franjas() → #173 (Daniel) ✓
//   espera()  → #175 (Josef). Todavía regresa DATOS DE EJEMPLO, para que el panel "Voy saliendo" (#60)
//               se pueda hacer sin esperar al backend. Quien la complete cambia SOLO el contenido.
//
// Las dos van SIEMPRE a la red, sin caché: la disponibilidad cambia minuto a minuto y no sirve
// mostrar la de hace rato. Los errores (sin red, 404...) regresan como Result.failure con su ErrorApi,
// igual que en CatalogoRepository.
//
// minutos = minutos de cocina del carrito (tiempo de preparación × cantidad de cada producto).
class PedidoRepository(
    private val api: ApiService
) {

    // #173: GET /tiendas/{id}/franjas. Las próximas franjas de la tienda (máximo 8); las llenas para
    // este pedido llegan con disponible = false y se muestran deshabilitadas, nunca se ocultan.
    suspend fun franjas(idTienda: Int, minutos: Int): Result<List<Franja>> = intentar {
        api.obtenerFranjas(idTienda, minutos).datos.map { it.aDominio() }
    }

    // #175: GET /tiendas/{id}/espera → api.obtenerEspera(idTienda, minutos).datos
    @Suppress("UNUSED_PARAMETER")
    suspend fun espera(idTienda: Int, minutos: Int): Result<Estimado> =
        Result.success(ESPERA_DE_EJEMPLO)

    // Convierte el resultado en Result: los ErrorApi (sin red, 401, 404, 422...) regresan como failure
    // con su código y mensaje del servidor.
    private suspend fun <T> intentar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: ErrorApi) {
        Result.failure(e)
    }

    private companion object {
        val ESPERA_DE_EJEMPLO = Estimado(
            min = 16,
            max = 22,
            saturada = true,
            pedidosEnCocina = 14,
            voySaliendoDisponible = true
        )
    }
}

private fun FranjaDto.aDominio() = Franja(
    fecha = fecha,
    inicio = inicio,
    fin = fin,
    minutosRestantes = minutosRestantes,
    disponible = disponible,
    descuento = descuento
)
