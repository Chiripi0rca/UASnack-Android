package mx.uasnack.app.data.red.interceptores

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class IdempotencyInterceptor : Interceptor {

    companion object {
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val method = originalRequest.method.uppercase()

        val requiresIdempotency = method == "POST" || method == "PUT" || method == "PATCH"

        if (!requiresIdempotency || originalRequest.header(IDEMPOTENCY_HEADER) != null) {
            return chain.proceed(originalRequest)
        }

        val idempotencyKey = UUID.randomUUID().toString()
        val modifiedRequest = originalRequest.newBuilder()
            .header(IDEMPOTENCY_HEADER, idempotencyKey)
            .build()

        return chain.proceed(modifiedRequest)
    }
}