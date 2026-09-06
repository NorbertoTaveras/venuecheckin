package com.norbertotaveras.venuecheckin.model

import java.util.UUID

data class Venue(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val beaconUuid: UUID,
    val beaconMajor: Int,
    val beaconMinor: Int
)