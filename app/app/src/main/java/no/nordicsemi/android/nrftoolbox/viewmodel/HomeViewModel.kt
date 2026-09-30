package no.nordicsemi.android.nrftoolbox.viewmodel

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.analytics.AppAnalytics
import no.nordicsemi.android.analytics.Link
import no.nordicsemi.android.analytics.ProfileOpenEvent
import no.nordicsemi.android.common.navigation.Navigator
import no.nordicsemi.android.nrftoolbox.ScannerDestinationId
import no.nordicsemi.android.service.profile.ServiceApi
import no.nordicsemi.android.toolbox.profile.ProfileDestinationId
import no.nordicsemi.android.toolbox.profile.data.ChannelSoundingServiceData
import no.nordicsemi.android.toolbox.profile.data.RangingSessionAction
import no.nordicsemi.android.toolbox.lib.utils.Profile
import no.nordicsemi.android.toolbox.profile.manager.repository.ChannelSoundingRepository
import no.nordicsemi.android.toolbox.profile.repository.DeviceRepository
import timber.log.Timber
import javax.inject.Inject

internal data class DeviceRangingData(
    val distance: Double? = null,
    val confidenceLevel: Int? = null, // Confidence level from ranging data
)

internal data class LoggedDataPoint(
    val timestamp: Long,
    val deviceName: String,
    val deviceAddress: String,
    val distance: Double?,
    val confidenceLevel: Int?,
)

internal data class HomeViewState(
    val connectedDevices: Map<String, ServiceApi.DeviceData> = emptyMap(),
    val channelSoundingData: Map<String, DeviceRangingData> = emptyMap(),
    val isLogging: Boolean = false,
    val loggedData: List<LoggedDataPoint> = emptyList(),
    val logFileName: String? = null,
    val showFileNameDialog: Boolean = false,
)

private const val GITHUB_REPO_URL = "https://github.com/NordicSemiconductor/Android-nRF-Toolbox.git"
private const val NORDIC_DEV_ZONE_URL = "https://devzone.nordicsemi.com/"

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val navigator: Navigator,
    deviceRepository: DeviceRepository,
    private val analytics: AppAnalytics,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeViewState())
    val state = _state.asStateFlow()

    private val observedDevices = mutableSetOf<String>()
    
    init {
        // Observe connected devices from the repository
        deviceRepository.connectedDevices.onEach { devices ->
            _state.update { currentState ->
                currentState.copy(connectedDevices = devices)
            }
            
            // Observe Channel Sounding data for each device
            devices.forEach { (address, deviceData) ->
                if (deviceData.services.any { it.profile == Profile.CHANNEL_SOUNDING }) {
                    // Only observe once per device
                    if (!observedDevices.contains(address)) {
                        observedDevices.add(address)
                        ChannelSoundingRepository.getData(address).onEach { csData ->
                            val currentState = _state.value
                            val previousRangingData = currentState.channelSoundingData[address]
                            
                            val rangingData = when (val action = csData.rangingSessionAction) {
                                is RangingSessionAction.OnResult -> {
                                    val data = DeviceRangingData(
                                        distance = action.data.distance?.measurement,
                                        confidenceLevel = action.data.distance?.confidenceLevel?.value
                                    )
                                    
                                    // If logging is active, save this data point
                                    if (currentState.isLogging) {
                                        val deviceData = currentState.connectedDevices[address]
                                        // Use current phone time (milliseconds since epoch)
                                        val timestamp = System.currentTimeMillis()
                                        val loggedPoint = LoggedDataPoint(
                                            timestamp = timestamp,
                                            deviceName = deviceData?.peripheral?.name ?: "Unknown",
                                            deviceAddress = address,
                                            distance = data.distance,
                                            confidenceLevel = data.confidenceLevel
                                        )
                                        _state.update { state ->
                                            state.copy(
                                                loggedData = state.loggedData + loggedPoint
                                            )
                                        }
                                    }
                                    
                                    data
                                }
                                else -> {
                                    // Keep the previous ranging data if available, otherwise use empty data
                                    previousRangingData ?: DeviceRangingData()
                                }
                            }
                            _state.update { currentState ->
                                currentState.copy(
                                    channelSoundingData = currentState.channelSoundingData + (address to rangingData)
                                )
                            }
                        }.launchIn(viewModelScope)
                    }
                } else {
                    // Remove from observed devices if Channel Sounding service is removed
                    observedDevices.remove(address)
                }
            }
            
            // Clean up observed devices that are no longer connected
            val connectedAddresses = devices.keys.toSet()
            observedDevices.removeAll { it !in connectedAddresses }
        }.launchIn(viewModelScope)
    }

    fun onClickEvent(event: UiEvent) {
        when (event) {
            UiEvent.OnConnectDeviceClick -> navigator.navigateTo(ScannerDestinationId)
            is UiEvent.OnDeviceClick -> {
                // Log the event for analytics.
                analytics.logEvent(ProfileOpenEvent(event.profile))

                navigator.navigateTo(
                    ProfileDestinationId, event.deviceAddress
                )
            }

            UiEvent.OnGitHubClick -> {
                // Log the event for analytics.
                analytics.logEvent(ProfileOpenEvent(Link.GITHUB))
                navigator.open(GITHUB_REPO_URL.toUri())
            }

            UiEvent.OnNordicDevZoneClick -> {
                // Log the event for analytics.
                analytics.logEvent(ProfileOpenEvent(Link.DEV_ACADEMY))
                navigator.open(NORDIC_DEV_ZONE_URL.toUri())
            }
            
            UiEvent.OnToggleLoggingClick -> {
                toggleLogging()
            }
        }
    }
    
    /**
     * Shows the file name input dialog.
     */
    fun showFileNameDialog() {
        _state.update { it.copy(showFileNameDialog = true) }
    }
    
    /**
     * Hides the file name input dialog.
     */
    fun hideFileNameDialog() {
        _state.update { it.copy(showFileNameDialog = false) }
    }
    
    /**
     * Sets the log file name and starts logging.
     */
    fun setLogFileNameAndStartLogging(fileName: String?) {
        _state.update { 
            it.copy(
                logFileName = fileName,
                showFileNameDialog = false,
                isLogging = true,
                loggedData = emptyList()
            )
        }
    }
    
    /**
     * Toggles logging state. If stopping, returns the logged data and file name for saving.
     */
    fun toggleLogging(): Pair<List<LoggedDataPoint>, String?>? {
        val currentState = _state.value
        return if (currentState.isLogging) {
            // Stop logging and return logged data with file name
            val loggedData = currentState.loggedData
            val fileName = currentState.logFileName
            _state.update { it.copy(isLogging = false, loggedData = emptyList(), logFileName = null) }
            Pair(loggedData, fileName)
        } else {
            // Start logging - show dialog first
            showFileNameDialog()
            null
        }
    }
    
    fun isLogging(): Boolean = _state.value.isLogging
}