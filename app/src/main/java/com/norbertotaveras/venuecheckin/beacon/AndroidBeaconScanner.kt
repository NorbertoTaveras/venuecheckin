package com.norbertotaveras.venuecheckin.beacon

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.norbertotaveras.venuecheckin.model.Venue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBeaconScanner @Inject constructor(
    private val bluetoothManager: BluetoothManager
) : BeaconScanner {
    private val _results = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    override val results: Flow<Int> = _results
    private var venue: Venue? = null
    private var isScanning = false
    private val tag = "BeaconScanner"

    private val callback = object : ScanCallback() {
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val currentVenue = venue ?: return

            if (matchesVenue(result, currentVenue)) {
                _results.tryEmit(
                    result.rssi
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(
                tag,
                "BLE scan failed with errorCode=$errorCode"
            )
            isScanning = false
            venue = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(venue: Venue) {
        if (isScanning && this.venue?.id == venue.id) {
            Log.d(
                tag,
                "Ignoring duplicate scan start for venueId=${venue.id}"
            )
            return
        }

        stop()

        this.venue = venue

        val scanner = bluetoothManager.adapter?.bluetoothLeScanner

        if (scanner == null) {
            Log.w(
                tag,
                "Bluetooth LE scanner unavailable"
            )

            this.venue = null
            return
        }

        try {
            scanner.startScan(callback)
            isScanning = true
            Log.d(
                tag,
                "Started BLE scan for venueId=${venue.id}"
            )
        } catch (exception: RuntimeException) {
            Log.e(
                tag,
                "Unable to start BLE scan for venueId=${venue.id}",
                exception
            )

            this.venue = null
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        if (!isScanning) {
            venue = null
            return
        }

        try {
            bluetoothManager.adapter
                ?.bluetoothLeScanner
                ?.stopScan(callback)
        } catch (exception: RuntimeException) {
            Log.w(
                tag,
                "Unable to stop BLE scan cleanly",
                exception
            )
        }

        venue = null
        isScanning = false

        Log.d(
            tag,
            "Stopped BLE scan"
        )
    }

    private fun matchesVenue(
        result: ScanResult,
        venue: Venue
    ): Boolean {
        val data = result.scanRecord
            ?.getManufacturerSpecificData(APPLE_COMPANY_ID)
            ?: return false

        if (data.size < 22 ||
            data[0] != IBEACON_TYPE ||
            data[1] != IBEACON_LENGTH
        ) {
            return false
        }

        val uuid = readUuid(data)
        val major = readInt(data[18], data[19])
        val minor = readInt(data[20], data[21])

        return uuid == venue.beaconUuid &&
                major == venue.beaconMajor &&
                minor == venue.beaconMinor
    }

    private fun readUuid(
        data: ByteArray
    ): UUID {

        val buffer = ByteBuffer.wrap(
            data,
            2,
            16
        )

        return UUID(
            buffer.long,
            buffer.long
        )
    }

    private fun readInt(
        first: Byte,
        second: Byte
    ): Int {
        return ((first.toInt() and 0xFF) shl 8) or (second.toInt() and 0xFF)
    }

    companion object {
        private const val APPLE_COMPANY_ID = 0x004C
        private val IBEACON_TYPE = 0x02.toByte()
        private val IBEACON_LENGTH = 0x15.toByte()
    }
}
