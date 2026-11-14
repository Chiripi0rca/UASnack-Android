package mx.uasnack.app.data.pago

import android.content.Context
import com.stripe.android.PaymentConfiguration
import mx.uasnack.app.BuildConfig

object StripeConfiguracion {

    fun inicializar(
        context: Context,
        stripeAccountId: String? = null
    ): Boolean {

        val clavePublicable =
            BuildConfig.STRIPE_PUBLISHABLE_KEY.trim()

        val claveValida =
            clavePublicable.startsWith("pk_test_") ||
                    clavePublicable.startsWith("pk_live_")

        if (!claveValida) {
            return false
        }

        PaymentConfiguration.init(
            context.applicationContext,
            clavePublicable,
            stripeAccountId
        )

        return true
    }
}