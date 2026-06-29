package com.example.service.lighting

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class PhilipsHueLightManager : SmartLightManager {
    private var bridgeIp: String = ""
    private var username: String = ""
    private var lightId: String = ""
    private var api: HueBridgeApi? = null

    override fun configure(bridgeIp: String, username: String, lightId: String) {
        this.bridgeIp = bridgeIp
        this.username = username
        this.lightId = lightId

        if (bridgeIp.isNotEmpty()) {
            val client = getUnsafeOkHttpClient()
            val retrofit = Retrofit.Builder()
                .baseUrl("https://$bridgeIp/") // Hue Bridge API v2 uses HTTPS locally
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            this.api = retrofit.create(HueBridgeApi::class.java)
            Log.d("PhilipsHueLightManager", "Configured Hue Light Manager on https://$bridgeIp/")
        }
    }

    override fun isConfigured(): Boolean {
        return bridgeIp.isNotEmpty() && username.isNotEmpty() && lightId.isNotEmpty()
    }

    override suspend fun triggerEffect(effectName: String) {
        if (!isConfigured()) {
            Log.w("PhilipsHueLightManager", "Cannot trigger effect '$effectName': Hue is not configured.")
            return
        }

        val request = when (effectName.uppercase()) {
            "EXPLOSION_RED" -> HueLightStateRequest(
                on = HueOn(true),
                dimming = HueDimming(100.0),
                color = HueColor(HueXy(0.675, 0.322)), // Vibrant Red
                dynamics = HueDynamics(100) // Fast 100ms transition
            )
            "CALM_BLUE" -> HueLightStateRequest(
                on = HueOn(true),
                dimming = HueDimming(60.0),
                color = HueColor(HueXy(0.167, 0.04)), // Calm Deep Blue
                dynamics = HueDynamics(1500) // Soft 1.5s transition
            )
            "SPOOKY_PURPLE" -> HueLightStateRequest(
                on = HueOn(true),
                dimming = HueDimming(40.0),
                color = HueColor(HueXy(0.2727, 0.1136)), // Deep Purple
                dynamics = HueDynamics(1000) // 1s transition
            )
            "FOREST_GREEN" -> HueLightStateRequest(
                on = HueOn(true),
                dimming = HueDimming(75.0),
                color = HueColor(HueXy(0.408, 0.517)), // Green
                dynamics = HueDynamics(2000) // Smooth 2s transition
            )
            "SUNSHINE_YELLOW" -> HueLightStateRequest(
                on = HueOn(true),
                dimming = HueDimming(90.0),
                color = HueColor(HueXy(0.443, 0.511)), // Gold Yellow
                dynamics = HueDynamics(1000)
            )
            "WHITE_LIGHT" -> HueLightStateRequest(
                on = HueOn(true),
                dimming = HueDimming(100.0),
                color = HueColor(HueXy(0.3127, 0.329)), // Daylight white
                dynamics = HueDynamics(500)
            )
            "OFF" -> HueLightStateRequest(
                on = HueOn(false)
            )
            else -> {
                Log.i("PhilipsHueLightManager", "Unknown effect '$effectName', defaulting to white light")
                HueLightStateRequest(
                    on = HueOn(true),
                    dimming = HueDimming(80.0),
                    color = HueColor(HueXy(0.3127, 0.329)),
                    dynamics = HueDynamics(500)
                )
            }
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d("PhilipsHueLightManager", "Sending PUT update for light $lightId, effect: $effectName")
                val response = api?.updateLightState(username, lightId, request)
                if (response != null && response.isSuccessful) {
                    Log.d("PhilipsHueLightManager", "Hue Bridge updated successfully: ${response.body()}")
                } else {
                    Log.e("PhilipsHueLightManager", "Hue Bridge update failed: Code ${response?.code()} - ${response?.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("PhilipsHueLightManager", "Error updating Hue light state", e)
            }
        }
    }

    /**
     * Hue local bridges usually use self-signed SSL certificates.
     * This helper creates an OkHttpClient that trusts all certs, strictly for local bridge communication.
     */
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder().build()
        }
    }
}
