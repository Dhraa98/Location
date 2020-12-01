package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.MainActivityViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "MainActivity"
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    var mMarker: Marker? = null
    var mMarkerLocation: Marker? = null
    lateinit var mMap: GoogleMap
    var markerList: MutableList<Marker> =
        mutableListOf()
    private lateinit var binding: ActivityMainBinding
    val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initControls()
    }

    private fun initControls() {
        binding.lifecycleOwner = this

        binding.viewmodel = viewModel

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
        if (googleMap != null) {
            mMap = googleMap
        }
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



                        viewModel.setAddress(location.latitude, location.longitude)

                        mMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(currentLocation)
                                .title(viewModel.searchText.value)
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
                        val builder: LatLngBounds.Builder = LatLngBounds.Builder()


                        builder.include(mMarker!!.getPosition())
                        builder.include(mMarkerLocation!!.getPosition())

                        val bounds = builder.build()

                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding =
                            (width * 0.10).toInt() // offset from edges of the map 10% of screen


                        val cu: CameraUpdate =
                            CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)

                        mMap!!.animateCamera(cu)
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

    /* private fun setAddress(latitude: Double, longitude: Double) {
         val geocoder: Geocoder
         var addresses: List<Address>? = null
         geocoder = Geocoder(this, Locale.getDefault())
         try {
             addresses = geocoder.getFromLocation(
                 latitude,
                 longitude,
                 1
             ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5
         } catch (e: IOException) {
             e.printStackTrace()
         }
         if (addresses!!.size > 0) {
             Log.d("max", " " + addresses[0].maxAddressLineIndex)
             val address = addresses[0]
                 .getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
             val city = addresses[0].locality
             val state = addresses[0].adminArea
             val country = addresses[0].countryName
             val postalCode = addresses[0].postalCode
             val knownName =
                 addresses[0].featureName // Only if available else return NULL
             addresses[0].adminArea


             tvSerchText.text = city

         }

     }*/
}
