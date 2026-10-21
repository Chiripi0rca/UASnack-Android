package mx.uasnack.app.ui.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import mx.uasnack.app.ui.navegacion.Rutas

// Los 4 destinos, en el mismo orden y con los mismos textos que la PWA (#227)
private data class Destino(val ruta: String, val texto: String, val icono: ImageVector)

private val DESTINOS = listOf(
    Destino(Rutas.TIENDAS, "Tiendas", Icons.Outlined.Storefront),
    Destino(Rutas.PEDIDOS, "Pedidos", Icons.AutoMirrored.Outlined.ReceiptLong),
    Destino(Rutas.PAGOS, "Pagos", Icons.Outlined.CreditCard),
    Destino(Rutas.PERFIL, "Perfil", Icons.Outlined.AccountCircle)
)

// Barra inferior del grafo principal (#229). NavigationBar de Material 3: cada destino mide más de
// 48 dp y el seleccionado lleva la pastilla de color (indicador), no solo otro color de texto.
@Composable
fun BarraInferior(navController: NavController) {
    // Se vuelve a dibujar cada vez que cambia la pantalla actual
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActivo = destinoDe(entradaActual?.destination?.route)

    NavigationBar {
        DESTINOS.forEach { destino ->
            NavigationBarItem(
                selected = destino.ruta == destinoActivo,
                onClick = {
                    navController.navigate(destino.ruta) {
                        // Patrón oficial de barra inferior: no apila pantallas al cambiar de pestaña,
                        // guarda el estado de cada pestaña y no duplica la misma pantalla.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destino.icono, contentDescription = null) },
                label = { Text(destino.texto) }
            )
        }
    }
}

// A qué pestaña pertenece una ruta. Catálogo, Detalle y Carrito cuelgan de "Tiendas",
// igual que en la PWA (/tiendas/3 marca Tiendas).
private fun destinoDe(ruta: String?): String = when {
    ruta == null -> Rutas.TIENDAS
    ruta.startsWith(Rutas.PEDIDOS) -> Rutas.PEDIDOS
    ruta.startsWith(Rutas.PAGOS) -> Rutas.PAGOS
    ruta.startsWith(Rutas.PERFIL) -> Rutas.PERFIL
    else -> Rutas.TIENDAS
}
