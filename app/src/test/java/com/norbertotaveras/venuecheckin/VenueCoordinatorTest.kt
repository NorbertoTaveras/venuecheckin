package com.norbertotaveras.venuecheckin

import com.norbertotaveras.venuecheckin.location.UserLocation
import com.norbertotaveras.venuecheckin.model.Proximity
import com.norbertotaveras.venuecheckin.model.Venue
import com.norbertotaveras.venuecheckin.model.VenueState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class VenueCoordinatorTest {
    private lateinit var scanner: FakeBeaconScanner
    private lateinit var locationSource: FakeLocationSource
    private lateinit var coordinator: VenueCoordinator

    private val venue = Venue(
        id = "venue_1",
        name = "Test Venue",
        latitude = 25.7814,
        longitude = -80.1870,
        radiusMeters = 150f,
        beaconUuid =
            UUID.fromString(
                "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
            ),
        beaconMajor = 100,
        beaconMinor = 1
    )

    @Before
    fun setup() {
        scanner = FakeBeaconScanner()

        locationSource = FakeLocationSource(location = null)

        coordinator = VenueCoordinator(
            beaconScanner = scanner,
            locationSource = locationSource
        )
    }

    @Test
    fun `enter updates state to inside`() {
        coordinator.onEnter(venue)

        assertFalse(scanner.isScanning)
        assertEquals(
            VenueState.Inside(venue),
            coordinator.state.value
        )
    }

    @Test
    fun `start scanning starts scanner for active venue`() {
        coordinator.onEnter(venue)
        coordinator.startScanning(venue)

        assertTrue(scanner.isScanning)
        assertEquals(venue, scanner.scannedVenue)
    }

    @Test
    fun `exit stops scanning and updates state`() {
        coordinator.onEnter(venue)
        coordinator.startScanning(venue)
        coordinator.onExit(venue.id)

        assertFalse(scanner.isScanning)
        assertNull(scanner.scannedVenue)
        assertEquals(
            VenueState.Outside,
            coordinator.state.value
        )
    }

    @Test
    fun `current location inside venue returns venue`() =
        runTest {
            val locationSource = FakeLocationSource(
                location =
                    UserLocation(
                        latitude = 25.7814,
                        longitude = -80.1870
                    )
            )

            val coordinator = VenueCoordinator(
                beaconScanner = scanner,
                locationSource = locationSource,
                scope = backgroundScope,
                beaconLostTimeoutMillis = 5_000L
            )

            val result = coordinator.findCurrentVenue(
                listOf(venue)
            )

            assertEquals(
                venue,
                result
            )
        }

    @Test
    fun `current location outside venues returns null`() =
        runTest {
            val locationSource = FakeLocationSource(
                location = UserLocation(
                    latitude = 25.7900,
                    longitude = -80.1870
                )
            )

            val coordinator =
                VenueCoordinator(
                    beaconScanner = scanner,
                    locationSource = locationSource,
                    scope = backgroundScope,
                    beaconLostTimeoutMillis = 5_000L
                )

            val result = coordinator.findCurrentVenue(
                listOf(venue)
            )

            assertNull(result)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RSSI result changes state to in range`() =
        runTest {
            val coordinator = VenueCoordinator(
                    beaconScanner = scanner,
                    locationSource = locationSource,
                    scope = backgroundScope,
                    beaconLostTimeoutMillis = 5_000L
                )

            runCurrent()

            coordinator.onEnter(venue)
            coordinator.startScanning(venue)

            scanner.emitRssi(-60)

            runCurrent()

            assertEquals(VenueState.InRange(
                    venue = venue,
                    proximity = Proximity.NEAR,
                    smoothedRssi = -60
                ),
                coordinator.state.value
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `beacon lost returns state to inside`() =
        runTest {
            val coordinator = VenueCoordinator(
                    beaconScanner = scanner,
                    locationSource = locationSource,
                    scope = backgroundScope,
                    beaconLostTimeoutMillis = 5_000L
                )

            runCurrent()

            coordinator.onEnter(venue)
            coordinator.startScanning(venue)

            scanner.emitRssi(-60)

            runCurrent()

            assertTrue(coordinator.state.value is VenueState.InRange)

            advanceTimeBy(5_001L.milliseconds)

            runCurrent()

            assertEquals(
                VenueState.Inside(
                    venue
                ),
                coordinator.state.value
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `exit stops scanning while beacon is in range`() =
        runTest {
            val coordinator =
                VenueCoordinator(
                    beaconScanner = scanner,
                    locationSource = locationSource,
                    scope = backgroundScope,
                    beaconLostTimeoutMillis = 5_000L
                )

            runCurrent()

            coordinator.onEnter(venue)
            coordinator.startScanning(venue)

            assertTrue(scanner.isScanning)

            scanner.emitRssi(-60)

            runCurrent()

            assertTrue(coordinator.state.value is VenueState.InRange)

            coordinator.onExit(venue.id)

            assertFalse(scanner.isScanning)

            assertNull(scanner.scannedVenue)

            assertEquals(
                VenueState.Outside,
                coordinator.state.value
            )
        }
}