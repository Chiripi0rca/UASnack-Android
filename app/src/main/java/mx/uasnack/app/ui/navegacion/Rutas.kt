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
    const val VERIFICACION = "verificacion/{$ARG_CORREO}"

    // Arma la ruta real de Verificación. Uri.encode evita que un carácter del correo rompa la ruta.
    fun verificacion(correo: String) = "verificacion/${Uri.encode(correo)}"

    // Pantallas del grafo principal (con sesión)
    const val TIENDAS = "tiendas"
}
