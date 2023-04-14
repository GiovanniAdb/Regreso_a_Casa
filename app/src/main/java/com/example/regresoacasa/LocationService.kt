package com.example.regresoacasa

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService


class LocationService : Service(), LocationListener {
    private var locationManager: LocationManager? = null
    private var lastKnownLocation: Location? = null
    fun onCreate() {
        super.onCreate()
        locationManager = getSystemService<Any>(Context.LOCATION_SERVICE) as LocationManager?
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0f, this)
        lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    override fun onLocationChanged(location: Location?) {
        lastKnownLocation = location
    }

    @Nullable
    fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun getLastKnownLocation(): Location? {
        return lastKnownLocation
    }
}
