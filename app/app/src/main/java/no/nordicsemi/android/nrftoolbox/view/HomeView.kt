package no.nordicsemi.android.nrftoolbox.view

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.ui.view.TextInputField
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.common.analytics.view.AnalyticsPermissionButton
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.channelsoundinglogger.R
import no.nordicsemi.android.nrftoolbox.viewmodel.HomeViewModel
import no.nordicsemi.android.nrftoolbox.viewmodel.UiEvent
import no.nordicsemi.android.toolbox.lib.utils.Profile
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeView() {
    val viewModel = hiltViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onEvent: (UiEvent) -> Unit = { viewModel.onClickEvent(it) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            NordicAppBar(
                title = {
                    Text(stringResource(id = R.string.app_name))
                },
                actions = {
                    AnalyticsPermissionButton()
                }
            )
         },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(UiEvent.OnConnectDeviceClick) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Connect to device",
                    )
                    Text(text = stringResource(R.string.connect_device))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                )
                .padding(horizontal = 16.dp)
                .consumeWindowInsets(paddingValues),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // Show the title at the top
                SectionTitle(
                    title = stringResource(R.string.connected_devices)
                )
            }
            item {
                if (state.connectedDevices.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.connectedDevices.keys.forEach { device ->
                            state.connectedDevices[device]?.let { deviceData ->
                                if (deviceData.connectionState.isConnected) {
                                    // Skip if no services
                                    if (deviceData.services.isEmpty()) return@forEach
                                    // Case 1: If only one service, show it directly like battery service
                                    if (deviceData.services.size == 1 && deviceData.services.first().profile == Profile.BATTERY) {
                                        FeatureButton(
                                            icon = painterResource(R.drawable.ic_battery),
                                            description = stringResource(R.string.battery_module_full),
                                            deviceName = deviceData.peripheral.name,
                                            deviceAddress = deviceData.peripheral.address,
                                            onClick = {
                                                onEvent(
                                                    UiEvent.OnDeviceClick(
                                                        deviceData.peripheral.address,
                                                        deviceData.services.first().profile
                                                    )
                                                )
                                            },
                                        )
                                    }
                                    // Case 2: Show the first *non-Battery* profile.
                                    // This ensures only one service is shown per peripheral when multiple services are available.
                                    deviceData.services.firstOrNull { it.profile != Profile.BATTERY }
                                        ?.let { serviceManager ->
                                            val peripheral = deviceData.peripheral
                                            val services = deviceData.services
                                            val onClick = {
                                                onEvent(
                                                    UiEvent.OnDeviceClick(
                                                        peripheral.address,
                                                        serviceManager.profile
                                                    )
                                                )
                                            }
                                            when (serviceManager.profile) {
                                                Profile.HRS -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_hrs),
                                                    description = stringResource(R.string.hrs_module_full),
                                                    deviceName = peripheral.name,
                                                    profileNames = services.map { it.profile.toString() },
                                                    deviceAddress = peripheral.address,
                                                    onClick = onClick,
                                                )

                                                Profile.HTS -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_hts),
                                                    description = stringResource(R.string.hts_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.BPS -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_bps),
                                                    description = stringResource(R.string.bps_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.GLS -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_gls),
                                                    description = stringResource(R.string.gls_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.CGM -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_cgm),
                                                    description = stringResource(R.string.cgm_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.RSCS -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_rscs),
                                                    description = stringResource(R.string.rscs_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.DFS -> FeatureButton(
                                                    icon = rememberVectorPainter(Icons.Default.MyLocation),
                                                    description = stringResource(R.string.direction_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.CSC -> FeatureButton(
                                                    icon = painterResource(R.drawable.ic_csc),
                                                    description = stringResource(R.string.csc_module_full),
                                                    deviceName = peripheral.name,
                                                    deviceAddress = peripheral.address,
                                                    profileNames = services.map { it.profile.toString() },
                                                    onClick = onClick,
                                                )

                                                Profile.THROUGHPUT -> {
                                                    FeatureButton(
                                                        icon = rememberVectorPainter(Icons.Default.SyncAlt),
                                                        description = stringResource(R.string.throughput_module),
                                                        deviceName = peripheral.name,
                                                        deviceAddress = peripheral.address,
                                                        profileNames = services.map { it.profile.toString() },
                                                        onClick = onClick,
                                                    )
                                                }

                                                Profile.UART -> {
                                                    FeatureButton(
                                                        icon = painterResource(R.drawable.ic_uart),
                                                        description = stringResource(R.string.uart_module_full),
                                                        deviceName = peripheral.name,
                                                        deviceAddress = peripheral.address,
                                                        profileNames = services.map { it.profile.toString() },
                                                        onClick = onClick,
                                                    )
                                                }

                                                Profile.CHANNEL_SOUNDING -> {
                                                    val rangingData = state.channelSoundingData[peripheral.address]
                                                    FeatureButton(
                                                        icon = rememberVectorPainter(Icons.Default.SocialDistance),
                                                        description = stringResource(R.string.channel_sounding_module_full),
                                                        deviceName = peripheral.name,
                                                        deviceAddress = peripheral.address,
                                                        profileNames = services.map { it.profile.toString() },
                                                        distance = rangingData?.distance,
                                                        confidenceLevel = rangingData?.confidenceLevel,
                                                        onClick = onClick,
                                                    )
                                                }

                                                Profile.LBS -> {
                                                    FeatureButton(
                                                        icon = rememberVectorPainter(Icons.Default.Lightbulb),
                                                        description = stringResource(R.string.lbs_blinky_module_full),
                                                        deviceName = peripheral.name,
                                                        deviceAddress = peripheral.address,
                                                        profileNames = services.map { it.profile.toString() },
                                                        onClick = onClick,
                                                    )
                                                }

                                                Profile.DFU -> {
                                                    FeatureButton(
                                                        icon = painterResource(R.drawable.ic_dfu),
                                                        description = stringResource(R.string.dfu_module_full),
                                                        deviceName = peripheral.name,
                                                        deviceAddress = peripheral.address,
                                                        profileNames = services.map { it.profile.toString() },
                                                        onClick = onClick,
                                                    )
                                                }

                                                Profile.BATTERY -> {
                                                    // Battery service is handled above, do nothing here.
                                                }
                                            }
                                        }
                                }

                            }
                        }
                    }
                } else {
                    NoConnectedDeviceView()
                }
            }
            item {
                SectionTitle(
                    title = stringResource(R.string.links)
                )
            }
            item {
                Links { onEvent(it) }
            }
            // Logging Button
            if (state.connectedDevices.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            if (state.isLogging) {
                                // Stop logging and save data
                                val result = viewModel.toggleLogging()
                                result?.let { (loggedData, fileName) ->
                                    coroutineScope.launch {
                                        saveLoggedDataToFile(context, loggedData, fileName)
                                    }
                                }
                            } else {
                                // Start logging - show dialog first
                                viewModel.onClickEvent(UiEvent.OnToggleLoggingClick)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (state.isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            if (state.isLogging) {
                                stringResource(R.string.stop_logging)
                            } else {
                                stringResource(R.string.start_log_data)
                            }
                        )
                    }
                }
            }
        }
        
        // File name input dialog
        if (state.showFileNameDialog) {
            LogFileNameDialog(
                onConfirm = { fileName ->
                    viewModel.setLogFileNameAndStartLogging(fileName)
                },
                onDismiss = {
                    viewModel.hideFileNameDialog()
                }
            )
        }
    }
}

@Composable
private fun LogFileNameDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.log_file_name_dialog_title)) },
        text = {
            TextInputField(
                input = fileName,
                label = stringResource(id = R.string.log_file_name_dialog_hint),
                placeholder = stringResource(id = R.string.log_file_name_dialog_placeholder),
                errorMessage = stringResource(id = R.string.log_file_name_empty),
                errorState = isError,
            ) {
                fileName = it
                isError = false
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedFileName = fileName.trim()
                if (trimmedFileName.isNotBlank()) {
                    onConfirm(trimmedFileName)
                } else {
                    isError = true
                }
            }) {
                Text(stringResource(id = R.string.log_file_name_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.log_file_name_dialog_dismiss))
            }
        }
    )
}

private suspend fun saveLoggedDataToFile(
    context: Context, 
    loggedData: List<no.nordicsemi.android.nrftoolbox.viewmodel.LoggedDataPoint>,
    userFileName: String?
) {
    withContext(Dispatchers.IO) {
        try {
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
            val fileName = if (userFileName != null && userFileName.isNotBlank()) {
                "$userFileName.csv"
            } else {
                "channel_sounding_logged_$timestamp.csv"
            }
            val file = File(context.getExternalFilesDir(null), fileName)
            
            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("Timestamp,Device Name,Address,Distance (m),Confidence Level\n")
                
                // Write logged data points
                val formatter = DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault())
                
                loggedData.forEach { dataPoint ->
                    // Format: M/d/yyyy HH:mm:ss.SS (24-hour format, e.g., "12/21/2025 21:21:10.72")
                    val timeStr = formatter.format(Instant.ofEpochMilli(dataPoint.timestamp))
                    val distanceStr = dataPoint.distance?.let { "%.2f".format(it) } ?: "N/A"
                    val confidenceStr = dataPoint.confidenceLevel?.toString() ?: "N/A"
                    writer.write("$timeStr,${dataPoint.deviceName},${dataPoint.deviceAddress},$distanceStr,$confidenceStr\n")
                }
            }
            
            withContext(Dispatchers.Main) {
                val filePath = file.absolutePath
                Toast.makeText(
                    context,
                    "${context.getString(R.string.file_saved)}\n$fileName\n(${loggedData.size} data points)\nPath: $filePath",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.file_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

