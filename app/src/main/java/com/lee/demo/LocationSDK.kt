package com.lee.demo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Criteria
import android.location.LocationManager
import android.util.Log
import com.lee.withpermission.PermissionManager
import com.lee.withpermission.WithPermissionTask

/**
 * Pretending that some module which need user location
 */
class LocationSDK() {
    companion object {
        val TAG = "LocationSDK"

        fun getLocation(activity: Activity) {
            // request location permission
            PermissionManager.withPermission(
                activity,
                object : WithPermissionTask(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    @SuppressLint("MissingPermission")
                    override fun onGrant() {
                        val locationManager =
                            activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val criteria = Criteria()
                        criteria.accuracy = Criteria.ACCURACY_FINE; // 高精度
                        criteria.isAltitudeRequired = true
                        criteria.isBearingRequired = false
                        criteria.isCostAllowed = true
                        val provider = locationManager.getBestProvider(criteria, true)
                        val location = locationManager.getLastKnownLocation(provider)
                        Log.d(
                            TAG,
                            "longitude: ${location.longitude}, latitude: ${location.latitude}, altitude: ${location.altitude}"
                        )
                    }
                })
        }
    }
}