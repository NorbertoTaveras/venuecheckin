package com.norbertotaveras.venuecheckin.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.norbertotaveras.venuecheckin.location.AndroidLocationSource
import com.norbertotaveras.venuecheckin.location.LocationSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
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
}