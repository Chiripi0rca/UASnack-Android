package mx.uasnack.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mx.uasnack.app.data.repositorio.SesionRepository
import mx.uasnack.app.ui.navegacion.NavegacionUASnack
import mx.uasnack.app.ui.tema.TemaUASnack

class MainActivity : ComponentActivity() {

    private var permisoRedLocalConcedido by mutableStateOf(false)

    private lateinit var solicitarPermisoRedLocal:
            ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )

        solicitarPermisoRedLocal =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { concedido ->
                permisoRedLocalConcedido = concedido
            }

        // Android 16 o inferior no necesita este permiso.
        // Android 17 (API 37) sí lo necesita.
        permisoRedLocalConcedido =
            Build.VERSION.SDK_INT < 37 ||
                    checkSelfPermission(
                        "android.permission.ACCESS_LOCAL_NETWORK"
                    ) == PackageManager.PERMISSION_GRANTED

        val sesionRepository =
            (application as UasnackApp).sesionRepository

        setContent {
            TemaUASnack {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (permisoRedLocalConcedido) {
                        // Solo se inicia la navegación cuando el permiso está concedido.
                        NavegacionUASnack(sesionRepository)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Permiso de red local necesario",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = "UASnack necesita conectarse a su servidor " +
                                        "para cargar facultades, tiendas y otros datos.",
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            Button(
                                onClick = {
                                    solicitarPermisoRedLocal.launch(
                                        "android.permission.ACCESS_LOCAL_NETWORK"
                                    )
                                }
                            ) {
                                Text("Conceder permiso")
                            }
                        }
                    }
                }
            }
        }

        // Solicita el permiso al abrir la aplicación si todavía no está concedido.
        if (!permisoRedLocalConcedido && Build.VERSION.SDK_INT >= 37) {
            solicitarPermisoRedLocal.launch(
                "android.permission.ACCESS_LOCAL_NETWORK"
            )
        }
    }
}