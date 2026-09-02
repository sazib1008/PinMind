package com.example.pinmind.core.di

import android.content.Context
import com.example.pinmind.core.location.DefaultLocationClient
import com.example.pinmind.core.location.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationClient(
        defaultLocationClient: DefaultLocationClient
    ): LocationClient

    @Binds
    @Singleton
    abstract fun bindGeofenceController(
        geofenceManager: com.example.pinmind.core.location.GeofenceManager
    ): com.example.pinmind.domain.location.GeofenceController


    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationProviderClient(
            @ApplicationContext context: Context
        ): FusedLocationProviderClient {
            return LocationServices.getFusedLocationProviderClient(context)
        }
    }
}
