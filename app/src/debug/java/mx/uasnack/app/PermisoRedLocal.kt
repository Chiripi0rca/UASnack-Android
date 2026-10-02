package mx.uasnack.app

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Solo en debug (la versión de release está en src/release y no hace nada).
//
// Desde Android 17 (API 37) la app necesita el permiso ACCESS_LOCAL_NETWORK para conectarse al
// backend local (10.0.2.2 o la IP de la PC). Sin él, toda petición termina en "Sin conexión" tras
// agotar el tiempo de espera. Aquí se pide una sola vez al abrir la app.
private const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"
private const val API_ANDROID_17 = 37

fun pedirPermisoRedLocal(actividad: Activity) {
    if (Build.VERSION.SDK_INT < API_ANDROID_17) return
    if (ContextCompat.checkSelfPermission(actividad, PERMISO_RED_LOCAL) == PackageManager.PERMISSION_GRANTED) return
    ActivityCompat.requestPermissions(actividad, arrayOf(PERMISO_RED_LOCAL), 0)
}
