package com.norbertotaveras.venuecheckin.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.norbertotaveras.venuecheckin.model.Venue
import com.norbertotaveras.venuecheckin.receiver.GeofenceReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GeofenceManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val geofencingClient: GeofencingClient
) {
    private val tag = "GeofenceTest"
    private val geofencePendingIntent: PendingIntent by lazy {

        val intent = Intent(
            context,
            GeofenceReceiver::class.java
        ).setAction(ACTION_GEOFENCE_EVENT)

        var flags = PendingIntent.FLAG_UPDATE_CURRENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }

        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )
    }

    @SuppressLint("MissingPermission")
    fun registerGeofences(
        venues: List<Venue>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val geofences = venues.map { venue ->
            Geofence.Builder().setRequestId(venue.id)
                .setCircularRegion(
                    venue.latitude,
                    venue.longitude,
                    venue.radiusMeters
                )
                .setExpirationDuration(
                    Geofence.NEVER_EXPIRE
                )
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .setNotificationResponsiveness(
                    5_000
                )
                .build()
        }

        Log.d(tag, "Registering geofences: " + venues.joinToString { venue ->
            "${venue.id}@${venue.latitude},${venue.longitude},r=${venue.radiusMeters}"
        })

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(
                    tag,
                    "Geofence registration succeeded for ids=" +
                            venues.joinToString { it.id }
                )
                onSuccess()
            }
            .addOnFailureListener { exception ->
                val statusCode = (exception as? ApiException)?.statusCode
                val status = statusCode?.let {
                    GeofenceStatusCodes
                        .getStatusCodeString(it)
                } ?: "unknown"

                Log.e(
                    tag,
                    "Geofence registration failed: ${exception.message} " +
                            "(statusCode=$statusCode, status=$status)",
                    exception
                )
                onError(exception)
            }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "com.norbertotaveras.venuecheckin.ACTION_GEOFENCE_EVENT"
    }
}
