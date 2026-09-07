package com.norbertotaveras.venuecheckin.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.norbertotaveras.venuecheckin.beacon.AndroidBeaconScanner
import com.norbertotaveras.venuecheckin.beacon.BeaconScanner
import com.norbertotaveras.venuecheckin.location.AndroidLocationSource
import com.norbertotaveras.venuecheckin.location.LocationSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGeofencingClient(
        @ApplicationContext context: Context
    ): GeofencingClient {
        return LocationServices
            .getGeofencingClient(
                context
            )
    }

    @Provides
    @Singleton
    fun provideLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices
            .getFusedLocationProviderClient(
                context
            )
    }

    @Provides
    @Singleton
    fun provideLocationSource(
        locationClient:
        FusedLocationProviderClient
    ): LocationSource {
        return AndroidLocationSource(
            locationClient
        )
    }

    @Provides
    @Singleton
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager {
        return context.getSystemService(
            BluetoothManager::class.java
        )
    }

    @Provides
    @Singleton
    fun provideBeaconScanner(
        scanner: AndroidBeaconScanner
    ): BeaconScanner {
        return scanner
    }
}