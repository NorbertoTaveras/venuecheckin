package com.norbertotaveras.venuecheckin

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.norbertotaveras.venuecheckin.data.VenueData
import com.norbertotaveras.venuecheckin.location.GeofenceManager
import com.norbertotaveras.venuecheckin.permissions.PermissionChecker
import com.norbertotaveras.venuecheckin.service.BeaconScanService
import com.norbertotaveras.venuecheckin.ui.theme.VenueCheckInTheme
import com.norbertotaveras.venuecheckin.model.Venue
import com.norbertotaveras.venuecheckin.model.VenueState
import com.norbertotaveras.venuecheckin.ui.MainEvent
import com.norbertotaveras.venuecheckin.ui.MainScreen
import com.norbertotaveras.venuecheckin.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var geofenceManager: GeofenceManager
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        refreshPermissions()
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val foregroundLocationLauncher =
                rememberLauncherForActivityResult(
                    contract =
                        ActivityResultContracts
                            .RequestMultiplePermissions()
                ) {
                    refreshPermissions()
                }

            val backgroundLocationLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts
                        .RequestPermission()
                ) {
                    refreshPermissions()
                }

            val bluetoothScanLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts
                        .RequestPermission()
                ) {
                    refreshPermissions()
                }

            VenueCheckInTheme {
                MainScreen(
                    uiState = uiState,
                    showManualLocationCheck = BuildConfig.DEBUG,

                    onEvent = { event ->
                        when (event) {
                            MainEvent.RequestForegroundLocation -> {
                                foregroundLocationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission
                                            .ACCESS_COARSE_LOCATION,
                                        Manifest.permission
                                            .ACCESS_FINE_LOCATION
                                    )
                                )
                            }

                            MainEvent.RequestBackgroundLocation -> {
                                requestBackgroundLocation(
                                    backgroundLocationLauncher
                                )
                            }

                            MainEvent.RequestBluetoothScan -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    bluetoothScanLauncher.launch(
                                        Manifest.permission
                                            .BLUETOOTH_SCAN
                                    )
                                }
                            }

                            MainEvent.StartMonitoring -> {
                                geofenceManager.registerGeofences(
                                    venues = VenueData.venues,
                                    onSuccess = {
                                        viewModel.setMonitoring(true)
                                        viewModel.addLog("Venue monitoring started")
                                        viewModel.findCurrentVenue(
                                            VenueData.venues
                                        ) { venue ->
                                            Log.d(
                                                "VenueTest",
                                                "Current venue result: ${venue?.name ?: "NONE"}"
                                            )

                                            if (venue != null) {
                                                viewModel.enterVenue(venue)

                                                if (PermissionChecker.hasBluetoothScan(this@MainActivity)) {
                                                    startBeaconService(venue.id)
                                                } else {
                                                    viewModel.addLog(
                                                        "Inside ${venue.name}; Bluetooth scan permission missing"
                                                    )
                                                }
                                            }
                                        }
                                    },

                                    onError = { exception ->
                                        viewModel.addLog(
                                            "Monitoring failed: " + (
                                                    exception.message
                                                        ?: "Unknown error"
                                                    )
                                        )
                                    }
                                )
                            }

                            MainEvent.CheckCurrentLocation -> {
                                checkCurrentLocation(
                                    uiState
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        viewModel.updatePermissions(
            hasFineLocation = PermissionChecker.hasFineLocation(this),
            hasBackgroundLocation = PermissionChecker.hasBackgroundLocation(this),
            hasBluetoothScan = PermissionChecker.hasBluetoothScan(this)
        )
    }

    private fun requestBackgroundLocation(
        launcher:
        androidx.activity.result
        .ActivityResultLauncher<String>
    ) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:$packageName".toUri()
                )
                startActivity(intent)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun checkCurrentLocation(
        currentUiState: com.norbertotaveras.venuecheckin.ui.MainUiState
    ) {
        viewModel.addLog(
            "Manual location check started"
        )

        viewModel.findCurrentVenue(
            VenueData.venues
        ) { venue ->
            Log.d(
                "VenueTest",
                "Manual current venue result: ${venue?.name ?: "NONE"}"
            )
            reconcileVenueFromCurrentLocation(
                venue = venue,
                previousState = currentUiState.venueState
            )
        }
    }

    private fun reconcileVenueFromCurrentLocation(
        venue: Venue?,
        previousState: VenueState
    ) {
        if (venue != null) {
            viewModel.enterVenue(
                venue
            )

            if (PermissionChecker.hasBluetoothScan(this)) {
                startBeaconService(venue.id)
            } else {
                viewModel.addLog(
                    "Inside ${venue.name}; Bluetooth scan permission missing"
                )
            }
            return
        }

        val activeVenue = when (previousState) {
            is VenueState.Inside -> previousState.venue
            is VenueState.InRange -> previousState.venue
            VenueState.Outside -> null
        }

        if (activeVenue == null) {
            viewModel.addLog(
                "Manual location check found no active venue"
            )
            return
        }

        viewModel.exitVenue(activeVenue.id)

        stopService(
            Intent(
                this,
                BeaconScanService::class.java
            )
        )
    }

    private fun startBeaconService(
        venueId: String
    ) {
        val serviceIntent = Intent(
            this,
            BeaconScanService::class.java
        ).apply {
            putExtra(
                BeaconScanService.EXTRA_VENUE_ID,
                venueId
            )
        }

        try {
            ContextCompat.startForegroundService(
                this,
                serviceIntent
            )
        } catch (exception: RuntimeException) {
            Log.e(
                "VenueTest",
                "Could not start beacon foreground service; remaining Inside without BLE scan",
                exception
            )
            viewModel.addLog(
                "Could not start BLE scan; still inside venue"
            )
        }
    }
}
