package com.norbertotaveras.venuecheckin.ui

import com.norbertotaveras.venuecheckin.model.LogEntry
import com.norbertotaveras.venuecheckin.model.VenueState

data class MainUiState(
    val venueState: VenueState = VenueState.Outside,
    val hasFineLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasBluetoothScan: Boolean = false,
    val isMonitoring: Boolean = false,
    val logs: List<LogEntry> = emptyList()
)