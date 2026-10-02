package mx.uasnack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import mx.uasnack.app.ui.acceso.LoginScreen
import mx.uasnack.app.ui.acceso.PantallaVerificacion
import mx.uasnack.app.ui.acceso.RegisterScreen
import mx.uasnack.app.ui.navegacion.Rutas
import mx.uasnack.app.ui.tiendas.PantallaTiendas

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            MaterialTheme {

                Surface {

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Rutas.ACCESO
                    ) {

                        // Grafo de acceso: pantallas para quien no tiene sesión.
                        navigation(
                            startDestination = Rutas.LOGIN,
                            route = Rutas.ACCESO
                        ) {

                            composable(Rutas.LOGIN) {

                                LoginScreen(

                                    onLoginClick = { correo, password ->

                                        // Después conectaremos el login
                                        // con el backend.

                                    },

                                    onRegisterClick = {

                                        navController.navigate(Rutas.REGISTRO)
                                    }
                                )
                            }

                            composable(Rutas.REGISTRO) {

                                RegisterScreen(

                                    onLoginClick = {

                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(Rutas.VERIFICACION) {

                                PantallaVerificacion(

                                    onVerificar = { codigo ->

                                        // TODO: cuando existan #160 y #161, verificar el código con
                                        // SesionRepository.verificar() y guardar la sesión.
                                        // Por ahora se da por bueno para probar la navegación.

                                        // Va al grafo principal y borra todo el grafo de acceso,
                                        // así el botón atrás no regresa a Verificación ni a Registro.
                                        navController.navigate(Rutas.PRINCIPAL) {
                                            popUpTo(Rutas.ACCESO) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }

                        // Grafo principal: pantallas para quien ya inició sesión.
                        navigation(
                            startDestination = Rutas.TIENDAS,
                            route = Rutas.PRINCIPAL
                        ) {

                            composable(Rutas.TIENDAS) {

                                PantallaTiendas()
                            }
                        }
                    }
                }
            }
        }
    }
}