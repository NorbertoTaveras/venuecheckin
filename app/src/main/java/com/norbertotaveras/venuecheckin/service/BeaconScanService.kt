package com.norbertotaveras.venuecheckin.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.norbertotaveras.venuecheckin.R
import com.norbertotaveras.venuecheckin.VenueCoordinator
import com.norbertotaveras.venuecheckin.data.VenueData
import com.norbertotaveras.venuecheckin.permissions.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BeaconScanService : Service() {
    @Inject
    lateinit var venueCoordinator: VenueCoordinator
    private val tag = "BeaconScanService"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val venueId = intent?.getStringExtra(
            EXTRA_VENUE_ID
        ) ?: return START_NOT_STICKY

        val venue = VenueData.venues.firstOrNull {
            it.id == venueId
        } ?: return START_NOT_STICKY

        if (!PermissionChecker.hasBluetoothScan(this)) {
            Log.d(
                tag,
                "Bluetooth scan permission missing; stopping service without changing venue state"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        if (!startAsForeground(venue.name)) {
            stopSelf()
            return START_NOT_STICKY
        }

        venueCoordinator.startScanning(venue)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        venueCoordinator.stopScanning()
        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    private fun startAsForeground(
        venueName: String
    ): Boolean {
        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                R.drawable.ic_launcher_foreground
            )
            .setContentTitle(
                "Venue Check-In"
            )
            .setContentText(
                "Scanning for beacon at $venueName"
            )
            .setOngoing(true)
            .build()

        return try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                }
            )
            true
        } catch (exception: RuntimeException) {
            Log.e(
                tag,
                "Unable to promote beacon scan service to foreground",
                exception
            )
            false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Beacon scanning",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_VENUE_ID = "extra_venue_id"
        private const val CHANNEL_ID = "beacon_scan"
        private const val NOTIFICATION_ID = 1001
    }
}
