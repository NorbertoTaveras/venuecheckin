package com.norbertotaveras.venuecheckin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.norbertotaveras.venuecheckin.model.VenueState

@Composable
fun MainScreen(
    uiState: MainUiState,
    showManualLocationCheck: Boolean = false,
    onEvent: (MainEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Venue Check-In"
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(text = "Current State")

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        CurrentState(
            venueState = uiState.venueState
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        PermissionRow(
            title = "Precise location",
            description =
                "Required to determine whether you are inside a venue.",
            granted = uiState.hasFineLocation,
            onRequest = {
                onEvent(
                    MainEvent.RequestForegroundLocation
                )
            }
        )

        if (uiState.hasFineLocation) {
            PermissionRow(
                title = "Background location",
                description =
                    "Required to detect venue entry and exit while the app is not open.",
                granted = uiState.hasBackgroundLocation,
                onRequest = {
                    onEvent(
                        MainEvent.RequestBackgroundLocation
                    )
                }
            )
        }

        if (uiState.hasFineLocation && uiState.hasBackgroundLocation) {
            PermissionRow(
                title = "Nearby devices",
                description =
                    "Required to scan for the venue beacon.",
                granted = uiState.hasBluetoothScan,
                onRequest = {
                    onEvent(
                        MainEvent.RequestBluetoothScan
                    )
                }
            )
        }

        if (uiState.hasFineLocation && uiState.hasBackgroundLocation) {
            Row {
                Button(
                    onClick = {
                        onEvent(
                            MainEvent.StartMonitoring
                        )
                    },
                    enabled = !uiState.isMonitoring
                ) {
                    Text(
                        text =
                            if (uiState.isMonitoring) {
                                "Monitoring"
                            } else {
                                "Start Monitoring"
                            }
                    )
                }

                if (showManualLocationCheck && uiState.isMonitoring) {
                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Button(
                        onClick = {
                            onEvent(
                                MainEvent.CheckCurrentLocation
                            )
                        }
                    ) {
                        Text(
                            text = "Check Location"
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        HorizontalDivider()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Event Log"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = uiState.logs
            ) { log ->

                Text(
                    text =
                        "${log.timestamp}  ${log.message}",
                    modifier =
                        Modifier.padding(
                            vertical = 4.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun CurrentState(
    venueState: VenueState
) {
    when (venueState) {
        VenueState.Outside -> {
            Text(
                text = "Outside"
            )
        }

        is VenueState.Inside -> {
            Text(
                text =
                    "Inside ${venueState.venue.name}"
            )
            Text(
                text = "Beacon not detected"
            )
        }

        is VenueState.InRange -> {
            Text(
                text =
                    "Inside ${venueState.venue.name}"
            )
            Text(
                text =
                    "Beacon: " +
                            venueState.proximity.name.lowercase()
            )
            Text(
                text =
                    "RSSI: ${venueState.smoothedRssi} dBm"
            )
        }
    }
}
