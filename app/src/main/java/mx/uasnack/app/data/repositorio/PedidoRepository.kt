package mx.uasnack.app.data.repositorio

import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.domain.modelo.Estimado
import mx.uasnack.app.domain.modelo.Franja

// ESQUELETO (Sprint 03). Única puerta de la app para lo que pasa al pedir.
// Cada función la completa su tarea:
//   franjas() → #173 (Daniel)
//   espera()  → #175 (Josef)
//
// Por ahora regresan DATOS DE EJEMPLO, para que las pantallas de Elección de retiro (#55) y el panel
// "Voy saliendo" (#60) se puedan hacer sin esperar al backend. Quien tiene la tarea cambia SOLO el
// contenido: el nombre, lo que recibe y lo que regresa se quedan igual.
//
// Las dos van SIEMPRE a la red, sin caché: la disponibilidad cambia minuto a minuto y no sirve
// mostrar la de hace rato. Los errores (sin red, 404...) regresan como Result.failure con su ErrorApi,
// igual que en CatalogoRepository.
//
// minutos = minutos de cocina del carrito (tiempo de preparación × cantidad de cada producto).
class PedidoRepository(
    @Suppress("unused") private val api: ApiService
) {

    // #173: GET /tiendas/{id}/franjas → api.obtenerFranjas(idTienda, minutos).datos
    @Suppress("UNUSED_PARAMETER")
    suspend fun franjas(idTienda: Int, minutos: Int): Result<List<Franja>> =
        Result.success(FRANJAS_DE_EJEMPLO)

    // #175: GET /tiendas/{id}/espera → api.obtenerEspera(idTienda, minutos).datos
    @Suppress("UNUSED_PARAMETER")
    suspend fun espera(idTienda: Int, minutos: Int): Result<Estimado> =
        Result.success(ESPERA_DE_EJEMPLO)

    private companion object {
        val FRANJAS_DE_EJEMPLO = listOf(
            Franja("2026-10-22", "11:00", "11:10", minutosRestantes = 24, disponible = true, descuento = null),
            Franja("2026-10-22", "11:10", "11:20", minutosRestantes = 6, disponible = false, descuento = null),
            Franja("2026-10-22", "11:20", "11:30", minutosRestantes = 30, disponible = true, descuento = 10),
            Franja("2026-10-22", "11:30", "11:40", minutosRestantes = 18, disponible = true, descuento = null)
        )

        val ESPERA_DE_EJEMPLO = Estimado(
            min = 16,
            max = 22,
            saturada = true,
            pedidosEnCocina = 14,
            voySaliendoDisponible = true
        )
    }
}
