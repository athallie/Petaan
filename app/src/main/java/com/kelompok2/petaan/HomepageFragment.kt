package com.kelompok2.petaan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.kelompok2.petaan.databinding.FragmentHomepageBinding
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.maps.MapView

class HomepageFragment : Fragment() {

    private var binding: FragmentHomepageBinding? = null
    private lateinit var mapView: MapView
    private var locationComponent: LocationComponent? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val api_key = BuildConfig.API_KEY
    private val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$api_key"
    /*private val styleUrl = "https://demotiles.maplibre.org/style.json"*/
    private val requiredPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(
                Manifest.permission.ACCESS_FINE_LOCATION, false
            ) -> {

            }
            permissions.getOrDefault(
                Manifest.permission.ACCESS_COARSE_LOCATION, false
            ) -> {

            }
            else -> {

            }
        }
    }
    private val gpsActivation = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateLocation()
    }

    private val args: HomepageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomepageBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding!!.mapView
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val db = Firebase.firestore

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync{ map ->
            map.setStyle(styleUrl) { style ->
                locationComponent = map.locationComponent
                locationComponent!!.activateLocationComponent(
                    LocationComponentActivationOptions
                        .builder(
                            requireContext(),
                            style,
                        )
                        .locationComponentOptions(
                            LocationComponentOptions
                                .builder(requireContext())
                                .pulseEnabled(true)
                                .build()
                        )
                        .useDefaultLocationEngine(false)
                        .build()
                )
                locationComponent!!.isLocationComponentEnabled = true
            }
            map.cameraPosition = CameraPosition
                .Builder()
                .target(LatLng(0.0,0.0))
                .zoom(1.0)
                .build()
            map.setOnMarkerClickListener { m ->
                map.cameraPosition = CameraPosition
                    .Builder()
                    .target(m.position)
                    .zoom(10.0)
                    .build()
                true
            }
        }

        db.collection("reports").get().addOnSuccessListener { documents ->
            documents.forEach { documentSnapshot ->
                try {
                    val latLng: GeoPoint? = documentSnapshot.getGeoPoint("location")
                    mapView.getMapAsync { map ->
                        map.addMarker(
                            MarkerOptions().apply {
                                title = documentSnapshot.get("subject") as String
                                position = LatLng(latLng!!.latitude, latLng.longitude)
                            }
                        )
                    }
                } catch (e: RuntimeException) {
                    Log.d("FIRESTOREERROR", "$e")
                }
            }
        }

        if (findNavController().previousBackStackEntry?.destination?.id == R.id.searchFragment) {
            val searchItemImageId = args.imageId
            db.collection("reports").document(searchItemImageId.toString())
                .get()
                .addOnSuccessListener { document ->
                    Log.d("FIRESTOREGET", "SUCCESS")
                    try {
                        val geoPoint: GeoPoint? = document.get("location") as GeoPoint?
                        val location: Location = Location("")
                        location.latitude = geoPoint!!.latitude
                        location.longitude = geoPoint.longitude
                        updateLocation(location)
                    } catch (e: NullPointerException) {
                        Log.d("SEARCHRESULT", "$e")
                    }
                }
                .addOnFailureListener {
                    Log.d("FIRESTOREGET", "FAILED")
                }
        }

        binding!!.mylocationButton.setOnClickListener {
            if (
                ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationManager = activity?.getSystemService(LOCATION_SERVICE) as LocationManager
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(requireActivity(), "You need to enable GPS.", Toast.LENGTH_SHORT).show()
                    gpsActivation.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                } else {
                    updateLocation()
                }
            } else {
                requiredPermissions.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        binding!!.addButton.setOnClickListener { view ->
            view.findNavController().navigate(R.id.addROIFragment)
        }

        binding!!.tolistviewButton.setOnClickListener { view ->
            view.findNavController().navigate(R.id.listViewFragment)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation() {
        fusedLocationProviderClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                Log.d("Location", "$location")
                locationComponent!!.forceLocationUpdate(location)
                mapView.getMapAsync { map ->
                    map.cameraPosition = CameraPosition
                        .Builder()
                        .target(LatLng(location.latitude,location.longitude))
                        .zoom(10.0)
                        .build()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation(location: Location) {
        Log.d("Location", "$location")
        locationComponent!!.forceLocationUpdate(location)
        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition
                .Builder()
                .target(LatLng(location.latitude,location.longitude))
                .zoom(10.0)
                .build()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}