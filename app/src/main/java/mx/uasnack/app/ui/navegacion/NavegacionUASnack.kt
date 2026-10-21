package mx.uasnack.app.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import mx.uasnack.app.data.repositorio.SesionRepository
import mx.uasnack.app.ui.acceso.LoginPantalla
import mx.uasnack.app.ui.acceso.RegistroPantalla
import mx.uasnack.app.ui.acceso.VerificacionPantalla
import mx.uasnack.app.ui.catalogo.CatalogoPantalla
import mx.uasnack.app.ui.catalogo.DetalleProductoPantalla
import mx.uasnack.app.ui.componentes.PantallaPendiente
import mx.uasnack.app.ui.pedido.CarritoPantalla
import mx.uasnack.app.ui.tiendas.TiendasPantalla

// Todas las pantallas de la app y cómo se pasa de una a otra.
// sesionRepository: se usa para saber, al abrir la app, si ya hay una sesión guardada (#161).
@Composable
fun NavegacionUASnack(sesionRepository: SesionRepository) {
    // ¿Hay sesión guardada? null mientras se lee DataStore; después true o false.
    // produceState corre el bloque una sola vez y redibuja la pantalla cuando cambia "value".
    // first() espera a que DataStore lea el disco y toma el token guardado (o null).
    val haySesion = produceState<Boolean?>(initialValue = null) {
        value = sesionRepository.tokenGuardado().first() != null
    }.value

    // Mientras se lee DataStore (unos milisegundos) no se dibuja nada: solo se ve el fondo del tema.
    // Si se dibujara el NavHost antes, quien ya tenía sesión vería un parpadeo del login.
    if (haySesion == null) return

    val navController = rememberNavController()

    // Con sesión guardada la app abre directo en el grafo principal (Tiendas);
    // sin sesión, en el grafo de acceso (Login).
    NavHost(
        navController = navController,
        startDestination = if (haySesion) Rutas.PRINCIPAL else Rutas.ACCESO
    ) {

        // Grafo de acceso: pantallas para quien no tiene sesión
        navigation(startDestination = Rutas.LOGIN, route = Rutas.ACCESO) {

            composable(Rutas.LOGIN) {
                LoginPantalla(
                    alIniciarSesion = { navController.entrarAlPrincipal() },
                    alIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                    alIrAVerificacion = { correo -> navController.navigate(Rutas.verificacion(correo)) }
                )
            }

            composable(Rutas.REGISTRO) {
                RegistroPantalla(
                    alCrearCuenta = { correo, reenviarEnSegundos ->
                        navController.navigate(Rutas.verificacion(correo, reenviarEnSegundos))
                    },
                    alIrALogin = { navController.popBackStack() }
                )
            }

            composable(
                route = Rutas.VERIFICACION,
                arguments = listOf(
                    navArgument(Rutas.ARG_CORREO) { type = NavType.StringType },
                    navArgument(Rutas.ARG_REENVIAR_EN) {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) {
                VerificacionPantalla(alVerificar = { navController.entrarAlPrincipal() })
            }
        }

        // Grafo principal: pantallas para quien ya inició sesión.
        // Cada pantalla va dentro de ConBarraInferior para que tenga la barra de navegación (#229).
        navigation(startDestination = Rutas.TIENDAS, route = Rutas.PRINCIPAL) {
            composable(Rutas.TIENDAS) {
                ConBarraInferior(navController) {
                    TiendasPantalla(alAbrirTienda = { idTienda -> navController.navigate(Rutas.catalogo(idTienda)) })
                }
            }

            // Catálogo de una tienda (#48): el id va en la ruta y el ViewModel lo lee de SavedStateHandle
            composable(
                route = Rutas.CATALOGO,
                arguments = listOf(navArgument(Rutas.ARG_ID_TIENDA) { type = NavType.IntType })
            ) {
                ConBarraInferior(navController) {
                    CatalogoPantalla(
                        alAbrirProducto = { idProducto -> navController.navigate(Rutas.producto(idProducto)) },
                        alVolver = { navController.popBackStack() },
                        // Desde la #170 ya hay a dónde ir: aparece el botón "Ver carrito"
                        alIrACarrito = { navController.navigate(Rutas.CARRITO) }
                    )
                }
            }

            // Detalle de producto (#51)
            composable(
                route = Rutas.PRODUCTO,
                arguments = listOf(navArgument(Rutas.ARG_ID_PRODUCTO) { type = NavType.IntType })
            ) {
                ConBarraInferior(navController) {
                    DetalleProductoPantalla(alVolver = { navController.popBackStack() })
                }
            }

            // Carrito (#170)
            composable(Rutas.CARRITO) {
                ConBarraInferior(navController) {
                    CarritoPantalla(
                        // Al quitar la última línea (o "Agregar más productos"): al catálogo de esa tienda,
                        // sacando Carrito de la pila para que atrás no regrese a un carrito vacío
                        alRegresarACatalogo = { idTienda ->
                            navController.navigate(Rutas.catalogo(idTienda)) {
                                popUpTo(Rutas.CARRITO) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        alIrATiendas = {
                            navController.navigate(Rutas.TIENDAS) {
                                popUpTo(Rutas.TIENDAS)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            // Destinos de la barra que aún no tienen pantalla (#229)
            composable(Rutas.PEDIDOS) { ConBarraInferior(navController) { PantallaPendiente("Pedidos") } }
            composable(Rutas.PAGOS) { ConBarraInferior(navController) { PantallaPendiente("Pagos") } }
            composable(Rutas.PERFIL) { ConBarraInferior(navController) { PantallaPendiente("Perfil") } }
        }
    }
}

// Va al grafo principal y borra todo el grafo de acceso,
// así el botón atrás no regresa al login, al registro ni a la verificación.
private fun NavController.entrarAlPrincipal() {
    navigate(Rutas.PRINCIPAL) {
        popUpTo(Rutas.ACCESO) { inclusive = true }
    }
}
