package com.norbertotaveras.venuecheckin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.norbertotaveras.venuecheckin.VenueCoordinator
import com.norbertotaveras.venuecheckin.data.VenueData
import com.norbertotaveras.venuecheckin.permissions.PermissionChecker
import com.norbertotaveras.venuecheckin.service.BeaconScanService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceReceiver : BroadcastReceiver() {
    @Inject
    lateinit var venueCoordinator: VenueCoordinator
    private val tag = "GeofenceTest"

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val event = GeofencingEvent.fromIntent(intent)

        if (event == null) {
            Log.w(tag, "Receiver invoked without a geofencing event. action=${intent.action}")
            return
        }

        Log.d(
            tag,
            "Receiver triggered. action=${intent.action}, " +
                    "transition=${transitionName(event.geofenceTransition)}, " +
                    "error=${event.errorCode}, " +
                    "triggeringLocation=${event.triggeringLocation?.latitude}," +
                    "${event.triggeringLocation?.longitude}"
        )

        if (event.hasError()) {
            Log.e(
                tag,
                "GeofencingEvent error: " +
                        GeofenceStatusCodes.getStatusCodeString(
                            event.errorCode
                        )
            )
            return
        }

        val transition = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences

        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(tag, "GeofencingEvent had no triggering geofences")
            return
        }

        triggeringGeofences.forEach { geofence ->
                Log.d(tag, "Triggered geofence: ${geofence.requestId}")

                val venue = VenueData.venues.firstOrNull { it.id == geofence.requestId } ?: return@forEach

                when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.d(tag, "ENTER: ${venue.name}")
                        venueCoordinator.onEnter(venue)

                        if (PermissionChecker.hasBluetoothScan(context)) {
                            startBeaconService(
                                context,
                                venue.id
                            )
                        } else {
                            Log.d(
                                tag,
                                "ENTER kept geographic Inside state; Bluetooth scan permission denied"
                            )
                        }
                    }

                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.d(tag, "EXIT: ${venue.name}")
                        venueCoordinator.onExit(venue.id)
                        context.stopService(
                            Intent(
                                context,
                                BeaconScanService::class.java
                            )
                        )
                    }

                    else -> {
                        Log.w(
                            tag,
                            "Unknown transition: $transition"
                        )
                    }
                }
            }
    }

    private fun startBeaconService(
        context: Context,
        venueId: String
    ) {

        val intent =
            Intent(
                context,
                BeaconScanService::class.java
            ).apply {
                putExtra(
                    BeaconScanService.EXTRA_VENUE_ID,
                    venueId
                )
            }

        try {
            ContextCompat.startForegroundService(
                context,
                intent
            )
        } catch (exception: RuntimeException) {
            Log.e(
                tag,
                "Could not start beacon foreground service; remaining Inside without BLE scan",
                exception
            )
        }
    }

    private fun transitionName(
        transition: Int
    ): String {
        return when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                "ENTER"

            Geofence.GEOFENCE_TRANSITION_EXIT ->
                "EXIT"

            else ->
                "UNKNOWN($transition)"
        }
    }
}
