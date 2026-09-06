package com.norbertotaveras.venuecheckin.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AndroidLocationSource @Inject constructor(
    private val locationClient:
    FusedLocationProviderClient
) : LocationSource {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): UserLocation? {
        val location = locationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).await() ?: return null

        return UserLocation(
            latitude = location.latitude,
            longitude = location.longitude
        )
    }
}