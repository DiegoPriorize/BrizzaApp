package com.priorizedev.brizza.data.api

import android.util.Log
import com.priorizedev.brizza.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// 1. Stripe API Input/Output Models
data class StripeCustomerList(
    @Json(name = "object") val obj: String?,
    @Json(name = "data") val data: List<StripeCustomer>?
)

data class StripeCustomer(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?
)

data class StripeSubscriptionList(
    @Json(name = "object") val obj: String?,
    @Json(name = "data") val data: List<StripeSubscription>?
)

data class StripeSubscription(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String, // "active", "trialing", "trialing", "unpaid", "canceled"
    @Json(name = "customer") val customer: String,
    @Json(name = "items") val items: StripeSubscriptionItems?
)

data class StripeSubscriptionItems(
    @Json(name = "data") val data: List<StripeSubscriptionItem>?
)

data class StripeSubscriptionItem(
    @Json(name = "id") val id: String?,
    @Json(name = "price") val price: StripePrice?
)

data class StripePrice(
    @Json(name = "id") val id: String,
    @Json(name = "recurring") val recurring: StripeRecurring?
)

data class StripeRecurring(
    @Json(name = "interval") val interval: String? // "month", "year"
)

data class StripeCheckoutSession(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String?
)

data class StripePortalSession(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String?
)

// 2. Retrofit Interface for Stripe v1 API
interface StripeApi {
    @GET("v1/customers")
    suspend fun findCustomersByEmail(
        @Header("Authorization") authHeader: String,
        @Query("email") email: String
    ): Response<StripeCustomerList>

    @GET("v1/subscriptions")
    suspend fun getSubscriptions(
        @Header("Authorization") authHeader: String,
        @Query("customer") customerId: String,
        @Query("status") status: String = "all"
    ): Response<StripeSubscriptionList>

    @FormUrlEncoded
    @POST("v1/checkout/sessions")
    suspend fun createCheckoutSession(
        @Header("Authorization") authHeader: String,
        @Field("success_url") successUrl: String,
        @Field("cancel_url") cancelUrl: String,
        @Field("mode") mode: String,
        @Field("customer_email") customerEmail: String,
        @Field("line_items[0][price]") priceId: String,
        @Field("line_items[0][quantity]") quantity: Int = 1
    ): Response<StripeCheckoutSession>

    @FormUrlEncoded
    @POST("v1/billing_portal/sessions")
    suspend fun createPortalSession(
        @Header("Authorization") authHeader: String,
        @Field("customer") customerId: String,
        @Field("return_url") returnUrl: String
    ): Response<StripePortalSession>
}

// 3. Singleton StripeSyncClient
object StripeSyncClient {
    private const val TAG = "StripeSyncClient"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getStripeApi(): StripeApi? {
        return try {
            Retrofit.Builder()
                .baseUrl("https://api.stripe.com/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(StripeApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao instanciar API do Stripe: ${e.message}", e)
            null
        }
    }

    private fun getStripeAuthHeader(): String? {
        return try {
            val secretKey = BuildConfig.STRIPE_SECRET_KEY
            if (secretKey.isBlank() || secretKey == "sk_test_placeholder" || secretKey.startsWith("YOUR_")) {
                null
            } else {
                "Bearer $secretKey"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro lendo STRIPE_SECRET_KEY: ${e.message}", e)
            null
        }
    }

    /**
     * Searches Stripe for a customer with the given email, retrieves their subscription list, Log the results
     * and returns the active plan ("PREMIUM", "ANUAL" or "FREE")
     */
    suspend fun verifySubscriptionDirectly(email: String): SubscriptionCheckResult {
        val api = getStripeApi() ?: return SubscriptionCheckResult.Error("API Stripe não inicializada")
        val authHeader = getStripeAuthHeader() ?: return SubscriptionCheckResult.NotConfigured

        try {
            Log.i(TAG, "Verificando assinatura via Stripe API para: $email")
            // 1. Find customer by email
            val customersRes = api.findCustomersByEmail(authHeader, email)
            if (!customersRes.isSuccessful) {
                val errBody = customersRes.errorBody()?.string() ?: customersRes.message()
                return SubscriptionCheckResult.Error("Falha ao buscar cliente Stripe: $errBody")
            }

            val customers = customersRes.body()?.data ?: emptyList()
            if (customers.isEmpty()) {
                Log.i(TAG, "Nenhum cliente Stripe com e-mail $email cadastrado.")
                return SubscriptionCheckResult.NoCustomer
            }

            // 2. Fetch all subscriptions for the customers found
            var activeSubscription: StripeSubscription? = null
            for (customer in customers) {
                val subsRes = api.getSubscriptions(authHeader, customer.id, "all")
                if (subsRes.isSuccessful) {
                    val subs = subsRes.body()?.data ?: emptyList()
                    val active = subs.firstOrNull { it.status == "active" || it.status == "trialing" }
                    if (active != null) {
                        activeSubscription = active
                        break
                    }
                }
            }

            if (activeSubscription != null) {
                // Subscription active! Determine plan type (Monthly or Annual)
                val item = activeSubscription.items?.data?.firstOrNull()
                val priceId = item?.price?.id ?: ""
                val interval = item?.price?.recurring?.interval ?: ""

                val plan = if (interval == "year" || priceId == BuildConfig.STRIPE_ANNUAL_PRICE_ID) {
                    "ANUAL"
                } else {
                    "PREMIUM"
                }

                Log.i(TAG, "Assinatura ativa detectada: ID ${activeSubscription.id}, Status ${activeSubscription.status}, Plano: $plan")
                return SubscriptionCheckResult.Active(
                    subscriptionId = activeSubscription.id,
                    customerId = activeSubscription.customer,
                    planType = plan
                )
            } else {
                Log.i(TAG, "Cliente existe no Stripe mas não tem nenhuma assinatura ativa.")
                return SubscriptionCheckResult.Inactive(customerId = customers.first().id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar subscrição diretamente do Stripe", e)
            return SubscriptionCheckResult.Error(e.localizedMessage ?: "Erro na chamada de rede")
        }
    }

    /**
     * Creates a Stripe Checkout Session for subscription
     */
    suspend fun createStripeCheckoutSession(email: String, isAnnualPlan: Boolean): CheckoutSessionResult {
        val api = getStripeApi() ?: return CheckoutSessionResult.Error("API Stripe não inicializada")
        val authHeader = getStripeAuthHeader() ?: return CheckoutSessionResult.NotConfigured

        val priceId = if (isAnnualPlan) {
            try { BuildConfig.STRIPE_ANNUAL_PRICE_ID } catch (e: Exception) { "" }
        } else {
            try { BuildConfig.STRIPE_MONTHLY_PRICE_ID } catch (e: Exception) { "" }
        }

        if (priceId.isBlank() || priceId == "price_monthly_placeholder" || priceId == "price_annual_placeholder") {
            return CheckoutSessionResult.Error("ID de Preço do Stripe não configurado (Verifique Secrets)")
        }

        try {
            Log.i(TAG, "Criando Checkout Session para: $email com PriceID: $priceId")
            val successUrl = "https://www.priorizeplanilhas.com.br/sst-de-bolso"
            val cancelUrl = "https://checkout.stripe.com/cancel_callback"

            val response = api.createCheckoutSession(
                authHeader = authHeader,
                successUrl = successUrl,
                cancelUrl = cancelUrl,
                mode = "subscription",
                customerEmail = email,
                priceId = priceId
            )

            if (response.isSuccessful && response.body() != null) {
                val session = response.body()!!
                if (!session.url.isNullOrBlank()) {
                    return CheckoutSessionResult.Success(session.id, session.url)
                }
            }

            val errBody = response.errorBody()?.string() ?: response.message()
            return CheckoutSessionResult.Error("Erro ao criar checkout Stripe: $errBody")
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao criar checkout session", e)
            return CheckoutSessionResult.Error(e.localizedMessage ?: "Falha de rede")
        }
    }

    /**
     * Creates a Stripe Customer Portal session (Billing Portal)
     */
    suspend fun createStripePortalSession(email: String): PortalSessionResult {
        val api = getStripeApi() ?: return PortalSessionResult.Error("API Stripe não inicializada")
        val authHeader = getStripeAuthHeader() ?: return PortalSessionResult.NotConfigured

        try {
            Log.i(TAG, "Criando Portal Session para e-mail: $email")
            // 1. Find customer by email
            val customersRes = api.findCustomersByEmail(authHeader, email)
            if (!customersRes.isSuccessful) {
                val errBody = customersRes.errorBody()?.string() ?: customersRes.message()
                return PortalSessionResult.Error("Falha ao buscar cliente Stripe: $errBody")
            }

            val customers = customersRes.body()?.data ?: emptyList()
            if (customers.isEmpty()) {
                return PortalSessionResult.Error("Nenhum cliente cadastrado no Stripe encontrado para o e-mail $email")
            }

            val customerId = customers.first().id
            val returnUrl = "https://www.priorizeplanilhas.com.br/sst-de-bolso"

            val response = api.createPortalSession(
                authHeader = authHeader,
                customerId = customerId,
                returnUrl = returnUrl
            )

            if (response.isSuccessful && response.body() != null) {
                val session = response.body()!!
                if (!session.url.isNullOrBlank()) {
                    return PortalSessionResult.Success(session.url)
                }
            }

            val errBody = response.errorBody()?.string() ?: response.message()
            return PortalSessionResult.Error("Erro ao criar portal billing Stripe: $errBody")
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao criar portal billing session", e)
            return PortalSessionResult.Error(e.localizedMessage ?: "Falha de rede")
        }
    }
}

sealed class SubscriptionCheckResult {
    object NotConfigured : SubscriptionCheckResult()
    object NoCustomer : SubscriptionCheckResult()
    data class Inactive(val customerId: String) : SubscriptionCheckResult()
    data class Active(val subscriptionId: String, val customerId: String, val planType: String) : SubscriptionCheckResult()
    data class Error(val message: String) : SubscriptionCheckResult()
}

sealed class CheckoutSessionResult {
    object NotConfigured : CheckoutSessionResult()
    data class Success(val sessionId: String, val checkoutUrl: String) : CheckoutSessionResult()
    data class Error(val message: String) : CheckoutSessionResult()
}

sealed class PortalSessionResult {
    object NotConfigured : PortalSessionResult()
    data class Success(val portalUrl: String) : PortalSessionResult()
    data class Error(val message: String) : PortalSessionResult()
}
