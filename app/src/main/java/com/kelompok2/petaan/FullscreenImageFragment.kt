package com.kelompok2.petaan

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.crossfade
import coil3.request.fallback
import coil3.request.placeholder
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kelompok2.petaan.databinding.FragmentFullscreenImageBinding
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLConnection

class FullscreenImageFragment : Fragment() {

    private val args: FullscreenImageFragmentArgs by navArgs()
    private lateinit var client: Client
    private lateinit var context: Context
    private var bottomNav: BottomNavigationView? = null
    private var binding: FragmentFullscreenImageBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFullscreenImageBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        context = view.context
        client = AppWriteHelper().getClient(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (bottomNav?.visibility == View.VISIBLE) {
            bottomNav?.visibility = View.GONE
        }

        // Inisialisasi ImageView dan Download Button
        val fullscreenImage = binding!!.fullscreenImage
        val closeButton = binding!!.closeButton
        val downloadButton = binding!!.downloadButton // Add this button to your layout XML

        // Load image menggunakan fileId
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileView = Storage(client).getFileView(
                    bucketId = BuildConfig.APP_WRITE_BUCKET_ID,
                    fileId = args.fileId
                )

                withContext(Dispatchers.Main) {
                    fullscreenImage.load(fileView) {
                        crossfade(true)
                        placeholder(R.drawable.ic_placeholder_image)
                        fallback(R.drawable.baseline_broken_image_24)
                    }
                }
            } catch (e: AppwriteException) {
                Log.d("APPWRITEEXCEPTION", "$e")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle close button
        closeButton.setOnClickListener {
            findNavController().navigateUp()
            bottomNav?.visibility = View.VISIBLE
        }

        // Handle download button
        downloadButton.setOnClickListener {
            lifecycleScope.launch {
                saveImage(downloadImage())
            }
        }
    }

    private suspend fun downloadImage(): ByteArray {
        val result = Storage(client).getFileDownload(
            bucketId = BuildConfig.APP_WRITE_BUCKET_ID,
            fileId = args.fileId
        )
        return result
    }

    private fun saveImage(image: ByteArray) {
        val resolver = requireContext().contentResolver

        val downloadCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newImageDetails = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, args.fileId)
            put(MediaStore.Downloads.MIME_TYPE, URLConnection.guessContentTypeFromStream(image.inputStream()))
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val newImageUri = resolver.insert(downloadCollection, newImageDetails)

        if (newImageUri != null) {
            resolver.openOutputStream(newImageUri).use { outputStream ->
                outputStream?.write(image)
            }
            newImageDetails.clear()
            newImageDetails.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(newImageUri, newImageDetails, null, null)
            Log.d("SAVEDIMAGE", newImageUri.toString())
        } else {
            Log.d("SAVEDIMAGE", "URI NULL")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}