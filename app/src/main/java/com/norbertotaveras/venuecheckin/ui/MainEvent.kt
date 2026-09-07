package com.norbertotaveras.venuecheckin.ui

sealed interface MainEvent {
    data object RequestForegroundLocation : MainEvent
    data object RequestBackgroundLocation : MainEvent
    data object RequestBluetoothScan : MainEvent
    data object StartMonitoring : MainEvent
    data object CheckCurrentLocation : MainEvent
}
