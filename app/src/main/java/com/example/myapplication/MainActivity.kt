package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "MainActivity"
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var ivSearch: LinearLayout
    private lateinit var tvSerchText: TextView
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    var mMarker: Marker? = null
    var mMarkerLocation: Marker? = null
    var mMap: GoogleMap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initControls()
    }

    private fun initControls() {

        ivSearch = findViewById(R.id.ivSearch)
        tvSerchText = findViewById(R.id.tvSerchText)
        ivSearch.setOnClickListener {
            onSearchCalled()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        if (!checkPermissions()) {
            requestPermissions()
        } else {
            initMap()
        }
    }


    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)


    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )


        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")


        } else {
            Log.i(TAG, "Requesting permission")

            startLocationPermissionRequest()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {

                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                initMap()
            } else {


            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap
        googleMap?.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                this, R.raw.map_style
            )
        )
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    googleMap?.apply {
                        val currentLocation = LatLng(location.latitude, location.longitude)

                        val circleDrawable =
                            resources.getDrawable(R.drawable.ic_home)
                        val markerIcon =
                            getMarkerIconFromDrawable(circleDrawable)



                        mMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(currentLocation)
                                .title("currentLocation")
                                .icon(
                                    markerIcon
                                )
                        )

                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                ), 15f
                            )
                        )
                        Log.e(TAG, "initControls: " + latitude + "====" + longitude)
                    }
                } else {
                    Toast.makeText(this, R.string.msg_location_empty, Toast.LENGTH_LONG).show()
                }

            }

    }

    private fun getMarkerIconFromDrawable(drawable: Drawable): BitmapDescriptor? {
        val canvas = Canvas()
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.getIntrinsicWidth(),
            drawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight())
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun onSearchCalled() {

        val apiKey = "AIzaSyDFjbzwtDSTsbCKwwjcSakbWjmM6I6nIhw"
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)

        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)


        val intent =
            Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).setCountry("IN")
                .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {

                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.e(TAG, "Place: ${place.name}, ${place.id},${place.latLng}")
                        if (mMarkerLocation != null) {
                            mMarkerLocation!!.remove()
                            mMarkerLocation = null

                        }
                        val circleDrawable =
                            resources.getDrawable(R.drawable.ic_location)
                        val markerIcon =
                            getMarkerIconFromDrawable(circleDrawable)
                        mMarkerLocation = mMap!!.addMarker(
                            MarkerOptions()
                                .position(place.latLng!!)
                                .title(place.name)
                                .icon(
                                    markerIcon
                                )
                        )
                        mMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(place.latLng, 15f)

                        )
                        tvSerchText.text = place.name
                    }


                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.e(TAG, status.statusMessage!!)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)

    }


}
