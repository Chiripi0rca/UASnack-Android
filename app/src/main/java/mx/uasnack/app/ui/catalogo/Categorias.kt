package mx.uasnack.app.ui.catalogo

// Nombres de las categorías por id. TEMPORAL, igual que shared/catalogo/categorias.js en la web:
// el backend todavía no tiene GET /categorias. Los ids deben coincidir con la tabla "categorias".
private val NOMBRES_CATEGORIA = mapOf(
    1 to "Bebidas",
    2 to "Botanas",
    3 to "Dulces",
    4 to "Comida rápida"
)

fun nombreCategoria(idCategoria: Int?): String = NOMBRES_CATEGORIA[idCategoria] ?: "Otros"
