package mx.uasnack.app

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

// Solo en debug (la versión de release está en src/release y muestra la app directo).
//
// Desde Android 17 (API 37) la app necesita el permiso ACCESS_LOCAL_NETWORK para conectarse al
// backend local (10.0.2.2 o la IP de la PC). Sin él, toda petición termina en "Sin conexión" tras
// agotar el tiempo de espera.
// La app no se muestra hasta tener el permiso: si arrancara antes, las primeras peticiones
// (facultades, tiendas) saldrían sin permiso y fallarían aunque después se conceda.
private const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"
private const val API_ANDROID_17 = 37

@Composable
fun ConPermisoRedLocal(contenido: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT < API_ANDROID_17) {
        contenido()
        return
    }

    val contexto = LocalContext.current
    fun tienePermiso() =
        ContextCompat.checkSelfPermission(contexto, PERMISO_RED_LOCAL) == PackageManager.PERMISSION_GRANTED

    var concedido by remember { mutableStateOf(tienePermiso()) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        concedido = it
    }

    // Se pide una vez al abrir la app
    LaunchedEffect(Unit) {
        if (!concedido) pedirPermiso.launch(PERMISO_RED_LOCAL)
    }
    // Al regresar de Ajustes se vuelve a revisar, por si se activó a mano
    LifecycleResumeEffect(Unit) {
        concedido = tienePermiso()
        onPauseOrDispose { }
    }

    if (concedido) {
        contenido()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Permiso de red local necesario", style = MaterialTheme.typography.titleLarge)
        Text(
            // Si se negó dos veces, Android ya no muestra el aviso: solo queda activarlo en Ajustes
            "La app de desarrollo lo necesita para hablar con el backend de tu PC. Si ya no aparece " +
                "el aviso, actívalo en Ajustes > Apps > UASnack > Permisos.",
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = { pedirPermiso.launch(PERMISO_RED_LOCAL) }) {
            Text("Conceder permiso")
        }
    }
}
