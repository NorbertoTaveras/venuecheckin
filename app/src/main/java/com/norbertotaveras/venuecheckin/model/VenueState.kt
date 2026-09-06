package com.norbertotaveras.venuecheckin.model

sealed interface VenueState {
    data object Outside : VenueState
    data class Inside(val venue: Venue) : VenueState
    data class InRange(
        val venue: Venue,
        val proximity: Proximity,
        val smoothedRssi: Int
    ) : VenueState
}