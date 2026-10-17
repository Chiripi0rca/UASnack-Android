package mx.uasnack.app.ui.navegacion

import android.net.Uri

object Rutas {

    // Grafos: agrupan pantallas para poder borrarlas juntas de la pila
    const val ACCESO = "acceso"
    const val PRINCIPAL = "principal"

    // Pantallas del grafo de acceso (sin sesión)
    const val LOGIN = "login"
    const val REGISTRO = "registro"

    // Verificación recibe el correo en la ruta. Al ir en la ruta, el ViewModel lo lee de su
    // SavedStateHandle y no se pierde aunque Android cierre la app en segundo plano.
    const val ARG_CORREO = "correo"

    // Segundos que hay que esperar para poder reenviar el código. Desde Registro llegan los que
    // mandó el servidor (reenviar_en_segundos); desde Login es 0, porque el código anterior pudo
    // haber vencido y el usuario debe poder pedir otro de inmediato.
    const val ARG_REENVIAR_EN = "reenviarEn"
    const val VERIFICACION = "verificacion/{$ARG_CORREO}?$ARG_REENVIAR_EN={$ARG_REENVIAR_EN}"

    // Arma la ruta real de Verificación. Uri.encode evita que un carácter del correo rompa la ruta.
    fun verificacion(correo: String, reenviarEnSegundos: Int = 0) =
        "verificacion/${Uri.encode(correo)}?$ARG_REENVIAR_EN=$reenviarEnSegundos"

    // Pantallas del grafo principal (con sesión)
    const val TIENDAS = "tiendas"

    // Destinos de la barra inferior (#229). Pedidos, Pagos y Perfil todavía son pantallas pendientes.
    const val PEDIDOS = "pedidos"
    const val PAGOS = "pagos"
    const val PERFIL = "perfil"
}
