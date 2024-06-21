package com.kalulugamestudio.locationtracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.kalulugamestudio.locationtracker.R
import com.kalulugamestudio.locationtracker.view.TrackingActivity
import java.util.Timer
import java.util.concurrent.Executors


class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val timer = Timer()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
        setupLocationRequest()
        setupLocationCallback()
        startLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        val notificationIntent = Intent(this, TrackingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Running")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "location_service_channel"
        val channelName = "Location Service Channel"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }


    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 35000) //45 seconds
            .setMinUpdateIntervalMillis(30000) // 30 seconds
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val timestamp = System.currentTimeMillis()
                    sendLocationToFirestore(latitude, longitude, timestamp)
                    sendLocationBroadcast(latitude, longitude)
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted")
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        fusedLocationClient.requestLocationUpdates(locationRequest, executor, locationCallback)
    }

    private fun sendLocationBroadcast(latitude: Double, longitude: Double) {
        val intent = Intent("com.kalulugamestudio.locationtracker.LOCATION_UPDATE")
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        sendBroadcast(intent)
    }

    private fun sendLocationToFirestore(latitude: Double, longitude: Double, timestamp: Long) {
        val db = FirebaseFirestore.getInstance()
        val locationData = hashMapOf(
            "time" to com.google.firebase.Timestamp(timestamp / 1000, ((timestamp % 1000) * 1000000).toInt()),
            "position" to GeoPoint(latitude, longitude)
        )

        db.collection("Current Location").document("current_location")
            .set(locationData)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }
}
