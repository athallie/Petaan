package com.kelompok2.petaan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import coil3.load
import coil3.request.allowHardware
import coil3.request.crossfade
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.kelompok2.petaan.databinding.FragmentAddroiBinding
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLConnection

class AddROIFragment : Fragment() {

    private var binding: FragmentAddroiBinding? = null
    private lateinit var appWriteClient: Client
    private lateinit var mimeType: String
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                binding!!.reportImageView.load(uri) {
                    crossfade(true)
                    allowHardware(false)
                }
                Log.d("Photo Picker", "Selected URI: $uri")
            } else {
                Log.d("Photo Picker", "No media selected.")
            }
        }
    private val gpsActivation = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateLocation(fusedLocationProviderClient)
    }
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
        getPermissionAndGPS()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddroiBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root

        return view
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appWriteClient = AppWriteHelper().getClient(requireContext())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        getPermissionAndGPS()

        binding!!.addReportImageFab.setOnClickListener { v ->
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding!!.addROITopAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding!!.saveAddReportButton.setOnClickListener { v ->

            val severityDropdown = binding!!.severityDropDown
            val subjectTextField = binding!!.subjectTextField
            val descriptionTextField = binding!!.descriptionTextField
            val locationTextField = binding!!.locationTextField
            val reportImageView = binding!!.reportImageView

            if (
                !severityDropdown.text.toString().trim().isEmpty() &&
                !subjectTextField.text.toString().trim().isEmpty() &&
                !descriptionTextField.text.toString().trim().isEmpty() &&
                !locationTextField.text.toString().trim().isEmpty() &&
                reportImageView.drawable != null
            ) {
                val severity = when (severityDropdown.text.toString()) {
                    "Low" -> 1
                    "Moderate" -> 2
                    "High" -> 3
                    "Critical" -> 4
                    "Emergency" -> 5
                    else -> -1
                }
                val latLng = locationTextField.text?.split(",")

                val imageId = ID.unique()
                val report = hashMapOf<String, Any>(
                    "subject" to subjectTextField.text.toString(),
                    "description" to descriptionTextField.text.toString(),
                    "severity" to severity,
                    "location" to GeoPoint(latLng!![0].toDouble(), latLng[1].toDouble()),
                )
                Utils().addFirestoreDocument(
                    requireContext(),
                    imageId,
                    Firebase.firestore.collection("reports"),
                    report,
                    binding!!.addRoiProgressIndicator,
                    { v.findNavController().navigate(R.id.homepageFragment) }
                )
                lifecycleScope.launch {
                    Utils().indexRecordsToAlgolia(report, imageId)
                }

                val bitmap = binding!!.reportImageView.drawToBitmap()
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                val imageBytes = outputStream.toByteArray()

                val byteArrayInputStream = ByteArrayInputStream(imageBytes)
                val mimeType = URLConnection.guessContentTypeFromStream(byteArrayInputStream)
                Log.d("MIME", "$mimeType")
                byteArrayInputStream.close()

                try {
                    lifecycleScope.launch {
                        AppWriteHelper()
                            .getStorage(appWriteClient)
                            .createFile(
                                bucketId = BuildConfig.APP_WRITE_BUCKET_ID,
                                fileId = imageId,
                                file = InputFile.fromBytes(
                                    bytes = imageBytes,
                                    mimeType = mimeType,
                                    filename = subjectTextField.text.toString()
                                )
                            )
                        outputStream.close()
                    }
                } catch (e: Error) {
                    Log.d("AppWriteError", "$e")
                }
            } else {
                if (severityDropdown.text.toString().trim().isEmpty()) {
                    severityDropdown.error = "This field is required."
                }
                if (subjectTextField.text.toString().trim().isEmpty()) {
                    subjectTextField.error = "This field is required."
                }
                if (descriptionTextField.text.toString().trim().isEmpty()) {
                    descriptionTextField.error = "This field is required."
                }
                if (locationTextField.text.toString().trim().isEmpty()) {
                    locationTextField.error = "This field is required."
                }
                if (reportImageView.drawable == null) {
                    Toast.makeText(
                        requireContext(),
                        "You have to specify an image.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getPermissionAndGPS() {
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
                updateLocation(fusedLocationProviderClient)
            }
        } else {
            requiredPermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation(client: FusedLocationProviderClient) {
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            try {
                val text = "${location.latitude}, ${location.longitude}"
                binding!!.locationTextField.setText(text)
            } catch (e: NullPointerException) {
                Log.d("ERROR", "$e")
            }
        }
    }
}