package com.norbertotaveras.venuecheckin

import android.util.Log
import com.norbertotaveras.venuecheckin.beacon.BeaconScanner
import com.norbertotaveras.venuecheckin.beacon.RssiSmoother
import com.norbertotaveras.venuecheckin.domain.VenueContainment
import com.norbertotaveras.venuecheckin.location.LocationSource
import com.norbertotaveras.venuecheckin.model.Proximity
import com.norbertotaveras.venuecheckin.model.Venue
import com.norbertotaveras.venuecheckin.model.VenueState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class VenueCoordinator internal constructor(
    private val beaconScanner: BeaconScanner,
    private val locationSource: LocationSource,
    private val scope: CoroutineScope,
    private val beaconLostTimeoutMillis: Long
) {

    @Inject
    constructor(
        beaconScanner: BeaconScanner,
        locationSource: LocationSource
    ) : this(
        beaconScanner = beaconScanner,
        locationSource = locationSource,
        scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default
        ),
        beaconLostTimeoutMillis = 5_000L
    )

    private val smoother = RssiSmoother()

    private val _state = MutableStateFlow<VenueState>(
        VenueState.Outside
    )

    val state: StateFlow<VenueState> = _state.asStateFlow()
    private var activeVenue: Venue? = null

    private var beaconLostJob: Job? = null

    private val tag = "VenueCoordinator"

    init {
        scope.launch {
            beaconScanner.results.collect { rssi ->
                handleRssi(rssi)
            }
        }
    }

    suspend fun findCurrentVenue(
        venues: List<Venue>
    ): Venue? {
        val location =
            locationSource.getCurrentLocation()
                ?: return null

        return venues.firstOrNull { venue ->
            VenueContainment.isInside(
                location = location,
                venue = venue
            )
        }
    }

    fun onEnter(venue: Venue) {
        debugLog(
            "onEnter venueId=${venue.id}, previous=${_state.value}"
        )

        activeVenue = venue

        smoother.clear()

        _state.value =
            VenueState.Inside(venue)
    }

    fun startScanning(venue: Venue) {
        if (activeVenue?.id != venue.id) {
            debugLog(
                "Ignoring scan start for inactive venueId=${venue.id}, activeVenueId=${activeVenue?.id}"
            )

            return
        }

        debugLog(
            "Starting BLE scan for venueId=${venue.id}"
        )

        beaconScanner.start(venue)
    }

    fun onExit(venueId: String) {
        val venue = activeVenue ?: return

        if (venue.id != venueId) {
            debugLog(
                "Ignoring EXIT for venueId=$venueId, activeVenueId=${venue.id}"
            )
            return
        }

        debugLog(
            "onExit venueId=$venueId"
        )

        stopScanning()

        activeVenue = null

        _state.value = VenueState.Outside
    }

    fun stopScanning() {
        beaconLostJob?.cancel()

        debugLog(
            "Stopping BLE scan"
        )

        beaconScanner.stop()

        smoother.clear()
    }

    private fun handleRssi(rssi: Int) {
        val venue = activeVenue ?: return

        val smoothedRssi = smoother.add(rssi)

        _state.value =
            VenueState.InRange(
                venue = venue,
                proximity = getProximity(
                    smoothedRssi
                ),
                smoothedRssi = smoothedRssi
            )

        restartBeaconLostTimer(
            venue
        )
    }

    private fun restartBeaconLostTimer(venue: Venue) {
        beaconLostJob?.cancel()
        beaconLostJob =
            scope.launch {
                delay(beaconLostTimeoutMillis.milliseconds)

                if (activeVenue?.id == venue.id) {
                    smoother.clear()
                    _state.value = VenueState.Inside(venue)
                }
            }
    }

    private fun getProximity(rssi: Int): Proximity {
        return when {
            rssi >= -55 -> Proximity.IMMEDIATE
            rssi >= -70 -> Proximity.NEAR
            rssi >= -90 -> Proximity.FAR
            else -> Proximity.UNKNOWN
        }
    }

    private fun debugLog(message: String) {
        try {
            Log.d(tag, message)
        } catch (_: RuntimeException) {
        }
    }
}
