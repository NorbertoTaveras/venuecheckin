package com.norbertotaveras.venuecheckin

import com.norbertotaveras.venuecheckin.domain.VenueContainment
import com.norbertotaveras.venuecheckin.location.UserLocation
import com.norbertotaveras.venuecheckin.model.Venue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class VenueContainmentTest {
    private val venue = Venue(
        id = "venue_1",
        name = "Test Venue",
        latitude = 25.7814,
        longitude = -80.1870,
        radiusMeters = 150f,
        beaconUuid = UUID.fromString(
            "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
        ),
        beaconMajor = 100,
        beaconMinor = 1
    )

    @Test
    fun `location inside venue radius returns true`() {
        val location = UserLocation(
            latitude = 25.7814,
            longitude = -80.1870
        )

        assertTrue(
            VenueContainment.isInside(
                location = location,
                venue = venue
            )
        )
    }

    @Test
    fun `location outside venue radius returns false`() {
        val location = UserLocation(
            latitude = 25.7900,
            longitude = -80.1870
        )

        assertFalse(
            VenueContainment.isInside(
                location = location,
                venue = venue
            )
        )
    }
}