package com.example.service.lighting

interface SmartLightManager {
    /**
     * Triggers a specific lighting effect (color, brightness, transition time)
     * based on an effect name (e.g. "EXPLOSION_RED", "CALM_BLUE", "SPOOKY_PURPLE").
     */
    suspend fun triggerEffect(effectName: String)

    /**
     * Configures the local bridge parameters (e.g. IP address, API username/token, light ID)
     */
    fun configure(bridgeIp: String, username: String, lightId: String)

    /**
     * Returns true if connection is configured, false otherwise.
     */
    fun isConfigured(): Boolean
}
