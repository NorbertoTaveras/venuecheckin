package com.norbertotaveras.venuecheckin.domain

import com.norbertotaveras.venuecheckin.location.UserLocation
import com.norbertotaveras.venuecheckin.model.Venue
import kotlin.math.*

object VenueContainment {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun isInside(
        location: UserLocation,
        venue: Venue
    ): Boolean {
        return distanceMeters(
            startLatitude = location.latitude,
            startLongitude = location.longitude,
            endLatitude = venue.latitude,
            endLongitude = venue.longitude
        ) <= venue.radiusMeters
    }

    private fun distanceMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
        val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
        val startLatitudeRadians = Math.toRadians(startLatitude)
        val endLatitudeRadians = Math.toRadians(endLatitude)
        val a = sin(latitudeDelta / 2).pow(2) +
                cos(startLatitudeRadians) *
                cos(endLatitudeRadians) *
                sin(longitudeDelta / 2).pow(2)

        val c = 2 * atan2(
            sqrt(a),
            sqrt(1 - a)
        )

        return EARTH_RADIUS_METERS * c
    }
}