package mx.uasnack.app.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Esquema de color con todos los roles de Color.kt.
// Solo hay modo claro porque el Anexo B no define paleta oscura: con el celular en modo oscuro
// la app se sigue viendo clara. Tampoco se usa color dinámico (Material You), que cambiaría
// los colores de la marca por los del fondo de pantalla del usuario.
private val ColoresClaros = lightColorScheme(
    primary = Primario,
    onPrimary = SobrePrimario,
    primaryContainer = ContenedorPrimario,
    onPrimaryContainer = SobreContenedorPrimario,
    secondary = Primario,
    onSecondary = SobrePrimario,
    secondaryContainer = ContenedorSecundario,
    onSecondaryContainer = SobreContenedorSecundario,
    tertiary = Terciario,
    onTertiary = SobreTerciario,
    tertiaryContainer = ContenedorTerciario,
    onTertiaryContainer = SobreContenedorTerciario,
    error = ColorError,
    onError = SobreError,
    errorContainer = ContenedorError,
    onErrorContainer = SobreContenedorError,
    background = Superficie,
    onBackground = SobreSuperficie,
    surface = Superficie,
    onSurface = SobreSuperficie,
    surfaceVariant = ContenedorSuperficie,
    onSurfaceVariant = SobreSuperficieVariante,
    // B.1 define un solo "surface container"; las variantes de Material (menús, tarjetas, hojas)
    // usan ese mismo valor para no heredar el gris morado por defecto.
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = ContenedorSuperficie,
    surfaceContainer = ContenedorSuperficie,
    surfaceContainerHigh = ContenedorSuperficie,
    surfaceContainerHighest = ContenedorSuperficie,
    outline = Contorno,
    outlineVariant = ContornoVariante
)

// Radios de B.4 (4 · 8 · 12 · 16 · 28).
// small = campos y chips, medium = tarjetas, extraLarge = hojas y diálogos.
// Los botones de Material 3 ya son píldora por defecto, por eso no se definen aquí.
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Tema de toda la app. Se pone una sola vez en MainActivity; las pantallas no lo vuelven a poner.
@Composable
fun TemaUASnack(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresClaros,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
