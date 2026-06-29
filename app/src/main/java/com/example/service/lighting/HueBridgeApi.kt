package com.example.service.lighting

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface HueBridgeApi {
    @PUT("clip/v2/resource/light/{id}")
    suspend fun updateLightState(
        @Header("hue-application-key") appKey: String,
        @Path("id") lightId: String,
        @Body state: HueLightStateRequest
    ): Response<HueLightStateResponse>
}

// Data models for Hue API v2
data class HueLightStateRequest(
    val on: HueOn? = null,
    val dimming: HueDimming? = null,
    val color: HueColor? = null,
    val dynamics: HueDynamics? = null
)

data class HueOn(val on: Boolean)
data class HueDimming(val brightness: Double) // 0.0 to 100.0
data class HueColor(val xy: HueXy)
data class HueXy(val x: Double, val y: Double)
data class HueDynamics(val duration: Int) // transition duration in ms

data class HueLightStateResponse(
    val errors: List<HueError>?,
    val data: List<HueData>?
)

data class HueError(val description: String)
data class HueData(val id: String)
