package no.nordicsemi.android.toolbox.profile.manager.repository

import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.UpdateRate

object ChannelSoundingRepository {
    private val dataMap = mutableMapOf<String, MutableStateFlow<ChannelSoundingServiceData>>()
    
    // Global update rate set from home page (null means use device-specific rate)
    private var globalUpdateRate: UpdateRate? = null

    fun getData(deviceId: String): MutableStateFlow<ChannelSoundingServiceData> =
        dataMap.getOrPut(deviceId) { MutableStateFlow(ChannelSoundingServiceData()) }
    
    /**
     * Sets the global update rate from home page.
     * If set, this will override device-specific update rates.
     */
    fun setGlobalUpdateRate(updateRate: UpdateRate?) {
        globalUpdateRate = updateRate
    }
    
    /**
     * Gets the global update rate set from home page.
     * Returns null if not set (meaning use device-specific rate).
     */
    fun getGlobalUpdateRate(): UpdateRate? = globalUpdateRate
    
    /**
     * Gets the effective update rate for a device.
     * Returns global update rate if set, otherwise returns device-specific rate or default.
     */
    fun getEffectiveUpdateRate(deviceId: String): UpdateRate {
        return globalUpdateRate ?: dataMap[deviceId]?.value?.updateRate ?: UpdateRate.NORMAL
    }
}