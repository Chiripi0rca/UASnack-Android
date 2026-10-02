package mx.uasnack.app.ui.navegacion


object Rutas {

    // Grafos: agrupan pantallas para poder borrarlas juntas de la pila
    const val ACCESO = "acceso"
    const val PRINCIPAL = "principal"

    // Pantallas del grafo de acceso (sin sesión)
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val VERIFICACION = "verificacion"

    // Pantallas del grafo principal (con sesión)
    const val TIENDAS = "tiendas"
}