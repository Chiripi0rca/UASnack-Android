package mx.uasnack.app.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PerfilPantalla(
    alCambiarPassword: () -> Unit,
    alCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel(
        factory = PerfilViewModel.Fabrica
    )
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when {
            estado.cargando && estado.perfil == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            estado.error != null && estado.perfil == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No se pudo cargar tu perfil",
                        color = MaterialTheme.colorScheme.error
                    )

                    Text(estado.error.orEmpty())

                    Button(onClick = viewModel::refrescar) {
                        Text("Reintentar")
                    }
                }
            }

            estado.perfil != null -> {
                val usuario = estado.perfil!!

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Perfil",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "${usuario.nombre} ${usuario.apellido}",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                usuario.correoInstitucional,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                usuario.facultad,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        val sinFaltas = usuario.faltas == 0
                        val pocas = usuario.faltas in 1..2

                        val fondo = when {
                            sinFaltas ->
                                MaterialTheme.colorScheme.primaryContainer
                            pocas ->
                                MaterialTheme.colorScheme.tertiaryContainer
                            else ->
                                MaterialTheme.colorScheme.errorContainer
                        }

                        val texto = when {
                            sinFaltas ->
                                MaterialTheme.colorScheme.onPrimaryContainer
                            pocas ->
                                MaterialTheme.colorScheme.onSurface
                            else ->
                                MaterialTheme.colorScheme.onErrorContainer
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = androidx.compose.material3.CardDefaults
                                .cardColors(containerColor = fondo)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    when (usuario.faltas) {
                                        0 -> "Sin faltas"
                                        1 -> "1 falta"
                                        else -> "${usuario.faltas} faltas"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = texto
                                )

                                Text(
                                    if (sinFaltas) {
                                        "Recoges tus pedidos a tiempo."
                                    } else {
                                        "Una falta es un pedido que no recogiste. Se quita una por cada 10 pedidos recogidos."
                                    },
                                    color = texto
                                )
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = alCambiarPassword,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("Cambiar contraseña")
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                viewModel.cerrarSesion(alCerrarSesion)
                            },
                            enabled = !estado.cerrandoSesion,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (estado.cerrandoSesion)
                                    "Cerrando sesión…"
                                else
                                    "Cerrar sesión",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    item {
                        estado.error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
