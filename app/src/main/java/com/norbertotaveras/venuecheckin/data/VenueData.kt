package com.norbertotaveras.venuecheckin.data

import com.norbertotaveras.venuecheckin.model.Venue
import java.util.UUID

object VenueData {
    val venues = listOf(
        Venue(
            id = "venue_1",
            name = "Kaseya Center",
            latitude = 25.7814,
            longitude = -80.1870,
            radiusMeters = 150f,
            beaconUuid = UUID.fromString(
                "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
            ),
            beaconMajor = 100,
            beaconMinor = 1
        ),
        Venue(
            id = "venue_2",
            name = "Bayfront Park",
            latitude = 25.7753,
            longitude = -80.1860,
            radiusMeters = 150f,
            beaconUuid = UUID.fromString(
                "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
            ),
            beaconMajor = 100,
            beaconMinor = 2
        ),
        Venue(
            id = "cityplace_doral",
            name = "CityPlace Doral",
            latitude = 25.8069771,
            longitude = -80.3311359,
            radiusMeters = 150f,
            beaconUuid = UUID.fromString(
                "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
            ),
            beaconMajor = 100,
            beaconMinor = 3
        )
    )
}