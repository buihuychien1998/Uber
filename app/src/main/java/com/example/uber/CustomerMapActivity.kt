package com.example.uber

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.imperiumlabs.geofirestore.GeoFirestore

class CustomerMapActivity : BaseActivity(), OnMapReadyCallback,
//    LocationListener,
    View.OnClickListener {
    private lateinit var mMap: GoogleMap
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var lastLocation: Location? = null
    private var locationRequest: LocationRequest? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationCallback: LocationCallback? = null
    private var locationManager: LocationManager? = null
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 14
    internal var mCurrLocationMarker: Marker? = null

    companion object {
        private const val CUSTOMER_REQUEST_COLLECTION = "CustomerRequest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_map)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun createLocationCallback() {
//        mLocationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                super.onLocationResult(locationResult)
//                val location = locationResult?.getLastLocation()
//                location?.let {
//                    //update location
//                    lastLocation = it
//                    val currentLocation = LatLng(it.latitude, it.longitude)
//                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
//                        currentLocation, 15f
//                    )
//                    mMap.addMarker(
//                        MarkerOptions().position(currentLocation).title("Marker in current location")
//                    )
//                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
//                    mMap.animateCamera(cameraUpdate)
//                }
//            }
//        }
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val locationList = locationResult.locations
                if (locationList.isNotEmpty()) {
                    //The last location in the list is the newest
                    val location = locationList.last()
                    Log.i(
                        TAG,
                        "Location: " + location.getLatitude() + " " + location.getLongitude()
                    )
                    lastLocation = location
                    mCurrLocationMarker?.remove()

                    //Place current location marker
                    val latLng = LatLng(location.latitude, location.longitude)
                    val markerOptions = MarkerOptions()
                    markerOptions.position(latLng)
                    markerOptions.title("Current Position")
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                    mCurrLocationMarker = mMap.addMarker(markerOptions)

                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.0F))
                }
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private fun buildGoogleApiClient() {
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        // Build a GoogleSignInClient with the options specified by gso.

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onStart() {
        super.onStart()
        checkEnableGPS()
        buildGoogleApiClient()
        createLocationRequest()
        createLocationCallback()
        if (!checkPermissions()) {
            requestPermissions()
            startLocationUpdates()
        } else {
            getLastLocation()
            startLocationUpdates()
        }
    }

    override fun onPause() {
        stopLocationUpdates()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        val uid = FirebaseAuth.getInstance().uid
        val db = FirebaseFirestore.getInstance()
        val geoFire = GeoFirestore(db.collection(CUSTOMER_REQUEST_COLLECTION))
        geoFire.removeLocation(uid)
    }


    @SuppressLint("MissingPermission")
    private fun checkEnableGPS() {
        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings

        // check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        enabled?.let {
            if (!it) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
//        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000L, 0F, this)
//        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000L, 0F, this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        mMap.isMyLocationEnabled = true
        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        lastLocation?.let {
//            onLocationChanged(it)
//            val sydney = LatLng(it.latitude, it.longitude)
//            mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//        } ?: kotlin.run {
//            onStart()
////            val sydney = LatLng(lastLocation.latitude, lastLocation.longitude)
////            mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
////            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//        }

    }


    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false
        }
        return true
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }


    private fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) && ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(
                TAG,
                getString(R.string.display_permission_rationale)
            )
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                View.OnClickListener { // Request permission
                    startLocationPermissionRequest()
                })
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest()
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted.
                    getLastLocation()
                }
                else -> {
                    // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.
                    showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        View.OnClickListener {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri: Uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        })
                }
            }
        }
    }


    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     *
     *
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient?.lastLocation?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                lastLocation = task.result
//                lastLocation?.let { onLocationChanged(it) }
            } else {
                Log.w(
                    TAG,
                    "getLastLocation:exception",
                    task.exception
                )
                Toast.makeText(this, getString(R.string.no_location_detected), Toast.LENGTH_SHORT)
                    .show()
            }
        } ?: kotlin.run {
            // Handle Null case or Request periodic location update https://developer.android.com/training/location/receive-location-updates
        }
//        fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
//            location?.let { it: Location ->
//                // Logic to handle location object
//                lastLocation = it
//            }
//        }?: kotlin.run {
//            // Handle Null case or Request periodic location update https://developer.android.com/training/location/receive-location-updates
//        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(mLocationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private fun showSnackbar(
        mainTextStringId: Int, actionStringId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(mainTextStringId),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(actionStringId), listener).show()
    }

//    override fun onLocationChanged(location: Location) {
//        lastLocation = location
//        location?.let { it ->
//            val currentLocation = LatLng(it.latitude, it.longitude)
//            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
//                currentLocation, 15f
//            )
//            mMap.addMarker(
//                MarkerOptions().position(currentLocation).title("Marker in current location")
//            )
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
//            mMap.animateCamera(cameraUpdate)
//            val uid = FirebaseAuth.getInstance().uid
//            val db = FirebaseFirestore.getInstance()
//            val geoFire = GeoFirestore(db.collection(CUSTOMER_REQUEST_COLLECTION))
//            geoFire.setLocation(uid, GeoPoint(it.latitude, it.longitude))
//        }
//
//    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnLogout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }

        }
    }


}