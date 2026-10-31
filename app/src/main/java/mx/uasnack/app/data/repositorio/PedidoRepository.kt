package mx.uasnack.app.data.repositorio

import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.ErrorApi
import mx.uasnack.app.data.red.dto.FranjaDto
import mx.uasnack.app.domain.modelo.Estimado
import mx.uasnack.app.domain.modelo.Franja
import mx.uasnack.app.domain.modelo.FranjasDelDia

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
    // Del "meta" se toma si el pedido cabe hoy y cuánto cabe como máximo (aviso de pedido grande).
    suspend fun franjas(idTienda: Int, minutos: Int): Result<FranjasDelDia> = intentar {
        val respuesta = api.obtenerFranjas(idTienda, minutos)
        FranjasDelDia(
            franjas = respuesta.datos.map { it.aDominio() },
            cabeHoy = respuesta.meta.cabeHoy,
            minutosMaximosHoy = respuesta.meta.minutosMaximosHoy
        )
    }

    // #175: GET /tiendas/{id}/espera
    suspend fun espera(idTienda: Int, minutos: Int): Result<Estimado> = intentar {
        api.obtenerEspera(idTienda, minutos).datos.let { dto ->
            Estimado(
                min = dto.min,
                max = dto.max,
                saturada = dto.saturada,
                pedidosEnCocina = dto.pedidosEnCocina,
                voySaliendoDisponible = dto.voySaliendoDisponible
            )
        }
    }

    // Convierte el resultado en Result: los ErrorApi (sin red, 401, 404, 422...) regresan como failure
    // con su código y mensaje del servidor.
    private suspend fun <T> intentar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: ErrorApi) {
        Result.failure(e)
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
