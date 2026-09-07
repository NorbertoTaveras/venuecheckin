package com.norbertotaveras.venuecheckin.beacon

import com.norbertotaveras.venuecheckin.model.Venue
import kotlinx.coroutines.flow.Flow

interface BeaconScanner {
    val results: Flow<Int>

    fun start(venue: Venue)

    fun stop()
}