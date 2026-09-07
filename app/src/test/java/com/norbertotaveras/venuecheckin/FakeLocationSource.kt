package com.norbertotaveras.venuecheckin

import com.norbertotaveras.venuecheckin.location.LocationSource
import com.norbertotaveras.venuecheckin.location.UserLocation

class FakeLocationSource(
    private val location: UserLocation?
) : LocationSource {

    override suspend fun getCurrentLocation(): UserLocation? {
        return location
    }
}