package com.norbertotaveras.venuecheckin

import com.norbertotaveras.venuecheckin.beacon.BeaconScanner
import com.norbertotaveras.venuecheckin.model.Venue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBeaconScanner : BeaconScanner {
    private val _results = MutableSharedFlow<Int>()

    override val results: Flow<Int> = _results

    var isScanning = false
        private set

    var scannedVenue: Venue? = null
        private set

    override fun start(venue: Venue) {
        isScanning = true
        scannedVenue = venue
    }

    override fun stop() {
        isScanning = false
        scannedVenue = null
    }

    suspend fun emitRssi(rssi: Int) {
        _results.emit(rssi)
    }
}