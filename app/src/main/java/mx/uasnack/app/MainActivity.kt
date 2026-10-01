package mx.uasnack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.uasnack.app.ui.acceso.LoginScreen
import mx.uasnack.app.ui.acceso.RegisterScreen

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
                        startDestination = "login"
                    ) {

                        composable("login") {

                            LoginScreen(

                                onLoginClick = { correo, password ->

                                    // Después conectaremos el login
                                    // con el backend.

                                },

                                onRegisterClick = {

                                    navController.navigate("register")
                                }
                            )
                        }

                        composable("register") {

                            RegisterScreen(

                                onLoginClick = {

                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}