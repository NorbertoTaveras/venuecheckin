package com.norbertotaveras.venuecheckin.location

interface LocationSource {
    suspend fun getCurrentLocation(): UserLocation?
}