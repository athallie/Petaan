package com.kelompok2.petaan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.fallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.kelompok2.petaan.databinding.FragmentHomepageBinding
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.random.Random

class HomepageFragment : Fragment() {

    private lateinit var context: Context
    private lateinit var client: Client
    private var currentMarker: Marker? = null
    private var markerList = mutableMapOf<String, Marker>()
    private var binding: FragmentHomepageBinding? = null
    private lateinit var mapView: MapView
    private var mapInstance: MapLibreMap? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val api_key = BuildConfig.API_KEY
    private val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$api_key"
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

    //intinya untuk mengamblil data dari fragment
    private val args: HomepageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomepageBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        context = view.context
        client = AppWriteHelper().getClient(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val locationInfo = binding!!.locationInfo
        locationInfo.visibility = View.GONE
        val locationImage = binding!!.locationImage

        mapView = binding!!.mapView
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val db = Firebase.firestore

        //Fungsi untuk mendapatkan instance map secara asynchronous
        //Kode di dalam lambda akan dijalankan setelah map siap digunakan
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            mapInstance = map
            map.setStyle(styleUrl) { style ->
                locationComponent = map.locationComponent
                locationComponent!!.activateLocationComponent(
                    LocationComponentActivationOptions
                        .builder(
                            requireContext(),
                            style,
                        )
                        .useDefaultLocationEngine(false)
                        .build()
                )
                locationComponent!!.isLocationComponentEnabled = true
            }
            //posisi kamera awal
            map.cameraPosition = CameraPosition
                .Builder()
                .target(LatLng(0.0, 0.0))
                .zoom(1.0)
                .build()

            map.setOnMarkerClickListener { marker ->
                val latitude = marker.position.latitude
                val longitude = marker.position.longitude
                var documentId : String = ""

                if (latitude != null && longitude != null) {
                    val db = Firebase.firestore
                    val userLocation = GeoPoint(latitude, longitude)


                    // Query untuk mencari laporan berdasarkan latitude dan longitude yang tepat
                    db.collection("reports")
                        .whereEqualTo("location", userLocation)
                        .get()
                        .addOnSuccessListener { result ->
                            if (!result.isEmpty) {
                                val document = result.documents.first()
                                val title = document.getString("subject") ?: "Unknown Title"
                                val description = document.getString("description") ?: "No description"
                                documentId = document.id

                                map.cameraPosition = CameraPosition
                                    .Builder()
                                    .target(LatLng(latitude, longitude))
                                    .zoom(19.0)
                                    .build()

                                binding?.apply {
                                    locationTitle.text = title
                                    locationDescription.text = description
                                    CoroutineScope(Dispatchers.IO).launch {
                                        locationImage.load(
                                            try {
                                                Storage(client).getFileView(
                                                    bucketId = BuildConfig.APP_WRITE_BUCKET_ID,
                                                    fileId = documentId
                                                )
                                            } catch (e: AppwriteException) {
                                                Log.d("APPWRITEEXCEPTION", "$e")
                                                null
                                            }
                                        ) {
                                            fallback(R.drawable.baseline_broken_image_24)
                                        }
                                    }

                                    // Tampilkan layout location_info
                                    locationInfo.visibility = View.VISIBLE
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirestoreError", "Failed to fetch location data: $exception")
                        }

                    binding!!.deleteButton.setOnClickListener(){
                        db.collection("reports")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("Firestore", "DocumentSnapshot successfully deleted!")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error deleting document", e)
                            }
                        val markersToDelete = mutableListOf<String>()
                        lifecycleScope.launch {
                            val result = Utils().deleteAlgoliaIndex(documentId)
                            val result1 = Storage(client).deleteFile(BuildConfig.APP_WRITE_BUCKET_ID, documentId)
                            Log.d("DELETEALGOLIAINDEX", "$result")
                            Log.d("DELETEIMAGEAPPWRITE", "$result1")
                            markersToDelete.add(documentId)
                            refreshMarkers(markersToDelete)
                            Toast.makeText(requireContext(), "Report deleted.", Toast.LENGTH_SHORT).show()
                            binding!!.locationInfo.visibility = View.GONE
                            map.cameraPosition = CameraPosition
                                .Builder()
                                .target(LatLng(latitude, longitude))
                                .zoom(10.0)
                                .build()
                        }
                    }

                }
                locationImage.setOnClickListener {
                    // Navigate using file IDdd
                    val directions = HomepageFragmentDirections.actionHomeFragmentToFullscreenImageFragment(documentId)
                    findNavController().navigate(directions)
                }

                true // Mencegah zoom pada klik marker
            }

            map.addOnMapClickListener {
                // Sembunyikan layout location_info
                binding?.locationInfo?.visibility = View.GONE
                true // Return true untuk menangani klik
            }

        }

        getMarkers(db)

        // searchFragment/listviewfragment -> marker yang ditekan
        val previousFragment = findNavController().previousBackStackEntry?.destination?.id
        if (previousFragment == R.id.searchFragment || previousFragment == R.id.listViewFragment) {
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


    //merubah posisi camera jika yang dicari ketemu
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

    fun generateRandomLong(): Long {
        return Random.nextLong(0, 1000) // Generates a random Long
    }

    private fun getMarkers(db: FirebaseFirestore) {
        db.collection("reports").get().addOnSuccessListener { documents ->
            documents.forEach { documentSnapshot ->
                try {
                    val latLng: GeoPoint? = documentSnapshot.getGeoPoint("location")
                    mapView.getMapAsync { map ->
                        markerList.put(
                            documentSnapshot.id,
                            map.addMarker(
                                MarkerOptions().apply {
                                    title = documentSnapshot.get("subject") as String
                                    position = LatLng(latLng!!.latitude, latLng.longitude)
                                }
                            )
                        )
                        currentMarker?.id = generateRandomLong()
                    }

                } catch (e: RuntimeException) {
                    Log.d("FIRESTOREERROR", "$e")
                }
            }
        }
    }

    private fun refreshMarkers(toDelete: MutableList<String>) {
        mapView.getMapAsync { map ->
            toDelete.forEach { id ->
                markerList.keys.forEach { key ->
                    if (id == key) {
                        map.removeMarker(markerList[key] as Marker)
                        markerList.remove(key)
                    }
                }
            }
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