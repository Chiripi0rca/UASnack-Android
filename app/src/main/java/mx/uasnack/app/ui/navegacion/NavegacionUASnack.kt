package mx.uasnack.app.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.uasnack.app.ui.acceso.LoginPantalla
import mx.uasnack.app.ui.acceso.RegistroPantalla
import mx.uasnack.app.ui.acceso.VerificacionPantalla
import mx.uasnack.app.ui.tiendas.TiendasPantalla

// Todas las pantallas de la app y cómo se pasa de una a otra.
@Composable
fun NavegacionUASnack() {
    val navController = rememberNavController()

    // TODO (#161): si SesionRepository.tokenGuardado() ya tiene token, empezar en PRINCIPAL.
    // Hay que esperar a leer DataStore antes de dibujar el NavHost; si no, se ve un parpadeo del login.
    NavHost(navController = navController, startDestination = Rutas.ACCESO) {

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
                    alCrearCuenta = { correo -> navController.navigate(Rutas.verificacion(correo)) },
                    alIrALogin = { navController.popBackStack() }
                )
            }

            composable(
                route = Rutas.VERIFICACION,
                arguments = listOf(navArgument(Rutas.ARG_CORREO) { type = NavType.StringType })
            ) {
                VerificacionPantalla(alVerificar = { navController.entrarAlPrincipal() })
            }
        }

        // Grafo principal: pantallas para quien ya inició sesión
        navigation(startDestination = Rutas.TIENDAS, route = Rutas.PRINCIPAL) {
            composable(Rutas.TIENDAS) {
                TiendasPantalla()
            }
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
