package mx.uasnack.app.data.repositorio

// Modelo o estructura de la franja (si aplica)
data class FranjaHoraria(
    val id: String,
    val horaInicio: String,
    val horaFin: String
)

interface PedidoRepository {
    // Método para obtener las franjas de los pedidos
    suspend fun obtenerFranjas(): List<FranjaHoraria>
}g