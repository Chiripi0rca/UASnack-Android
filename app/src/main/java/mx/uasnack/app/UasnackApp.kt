package mx.uasnack.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.uasnack.app.data.local.SesionAlmacen
import mx.uasnack.app.data.local.dataStoreSesion
import mx.uasnack.app.data.red.ApiService
import mx.uasnack.app.data.red.ModuloRed
import mx.uasnack.app.data.repositorio.SesionRepository

// Se crea una sola vez al abrir la app, antes que cualquier pantalla (está registrada en
// AndroidManifest.xml con android:name=".UasnackApp"). Arma las piezas que se comparten entre
// pantallas: DataStore, la API y los repositorios.
//
// Desde una Activity se piden así: (application as UasnackApp).sesionRepository
class UasnackApp : Application() {

    // Corrutinas que viven lo mismo que la app. Sirven para trabajo que no depende de una pantalla,
    // como mantener el token en memoria o borrar la sesión cuando vence.
    // SupervisorJob: si una falla, las demás siguen. Dispatchers.IO: trabajo de disco y red.
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // "by lazy": cada pieza se crea la primera vez que alguien la pide, y después se reutiliza.

    // Guarda el token en DataStore
    val sesionAlmacen: SesionAlmacen by lazy { SesionAlmacen(dataStoreSesion, alcance) }

    // Cliente de la API con los interceptores de #160, conectado al almacén de sesión
    val api: ApiService by lazy {
        ModuloRed.crearApiService(
            // AuthInterceptor pide aquí el token para ponerlo en cada petición
            tokenActual = sesionAlmacen::tokenActual,
            // SesionInterceptor llama esto cuando el backend responde 401 a una petición con token.
            // Avisa desde el hilo de OkHttp, por eso el borrado de DataStore va en segundo plano.
            alVencerSesion = { alcance.launch { sesionAlmacen.borrar() } }
        )
    }

    // Lo que usarán los ViewModels de acceso, registro y verificación
    val sesionRepository: SesionRepository by lazy { SesionRepository(api, sesionAlmacen) }
}

// Para las fábricas de los ViewModels (viewModelFactory { initializer { ... } }):
// Android les pasa el Application en las CreationExtras, y de ahí se sacan los repositorios.
fun CreationExtras.uasnackApp(): UasnackApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UasnackApp
