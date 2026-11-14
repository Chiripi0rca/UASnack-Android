package mx.uasnack.app.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

@Composable
fun StripePaymentSheet(
    alResultado: (PaymentSheetResult) -> Unit
): PaymentSheet {

    val callbackActualizado =
        rememberUpdatedState(alResultado)

    return PaymentSheet.Builder { resultado ->
        callbackActualizado.value(resultado)
    }.build()
}