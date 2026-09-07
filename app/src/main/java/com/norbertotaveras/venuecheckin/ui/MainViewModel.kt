package com.norbertotaveras.venuecheckin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.norbertotaveras.venuecheckin.VenueCoordinator
import com.norbertotaveras.venuecheckin.model.LogEntry
import com.norbertotaveras.venuecheckin.model.Venue
import com.norbertotaveras.venuecheckin.model.VenueState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val venueCoordinator:
    VenueCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var previousVenueState: VenueState? = null

    init {
        observeVenueState()
    }

    private fun observeVenueState() {
        viewModelScope.launch {
            venueCoordinator.state
                .collect { venueState ->
                    val previous = previousVenueState
                    _uiState.update {
                        it.copy(
                            venueState =
                                venueState
                        )
                    }
                    logVenueStateChange(
                        previous = previous,
                        current = venueState
                    )
                    previousVenueState = venueState
                }
        }
    }

    fun findCurrentVenue(
        venues: List<Venue>,
        onResult: (Venue?) -> Unit
    ) {
        viewModelScope.launch {
            val venue = venueCoordinator.findCurrentVenue(venues)
            onResult(venue)
        }
    }

    fun enterVenue(venue: Venue) {
        venueCoordinator.onEnter(venue)
    }

    fun exitVenue(venueId: String) {
        venueCoordinator.onExit(venueId)
    }

    fun updatePermissions(
        hasFineLocation: Boolean,
        hasBackgroundLocation: Boolean,
        hasBluetoothScan: Boolean
    ) {
        _uiState.update {
            it.copy(
                hasFineLocation = hasFineLocation,
                hasBackgroundLocation = hasBackgroundLocation,
                hasBluetoothScan = hasBluetoothScan
            )
        }
    }

    fun setMonitoring(
        monitoring: Boolean
    ) {
        _uiState.update {
            it.copy(isMonitoring = monitoring)
        }
    }

    fun addLog(message: String) {
        val timestamp = LocalTime.now()
            .format(
                DateTimeFormatter.ofPattern("HH:mm:ss")
            )

        val logEntry = LogEntry(timestamp = timestamp, message = message)
        _uiState.update {
            it.copy(logs = it.logs + logEntry)
        }
    }

    private fun logVenueStateChange(
        previous: VenueState?,
        current: VenueState
    ) {
        when (current) {
            VenueState.Outside -> {
                val previousVenue = when (previous) {
                    is VenueState.Inside -> previous.venue
                    is VenueState.InRange -> previous.venue
                    else -> null
                }

                if (previousVenue != null) {
                    addLog("Exited ${previousVenue.name}")
                }
            }

            is VenueState.Inside -> {
                when (previous) {
                    is VenueState.InRange -> {

                        if (previous.venue.id == current.venue.id) {
                            addLog("Beacon lost at ${current.venue.name}")
                        }
                    }

                    is VenueState.Inside -> {}

                    else -> {
                        addLog("Entered ${current.venue.name}")
                    }
                }
            }

            is VenueState.InRange -> {

                val shouldLog = when (previous) {
                    is VenueState.InRange ->
                        previous.venue.id != current.venue.id ||
                                previous.proximity != current.proximity

                    else -> true
                }

                if (shouldLog) {
                    addLog(
                        "Beacon ${current.proximity.name.lowercase()} " +
                                "(${current.smoothedRssi} dBm)"
                    )
                }
            }
        }
    }
}
