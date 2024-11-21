package com.kelompok2.petaan

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.crossfade
import coil3.request.fallback
import coil3.request.placeholder
import com.kelompok2.petaan.databinding.FragmentFullscreenImageBinding
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FullscreenImageFragment : Fragment() {

    private val args: FullscreenImageFragmentArgs by navArgs()
    private lateinit var client: Client
    private lateinit var context: Context
    private var binding: FragmentFullscreenImageBinding? = null
    private var imageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFullscreenImageBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        context = view.context
        client = AppWriteHelper().getClient(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                // Store the image URL for later use in download
                imageUrl = fileView.toString()

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
        }

        // Handle download button
        downloadButton.setOnClickListener {
            downloadImage()
        }
    }

    private fun downloadImage() {
        imageUrl?.let { url ->
            try {
                val downloadManager = ContextCompat.getSystemService(
                    context,
                    DownloadManager::class.java
                ) as DownloadManager

                // Generate a unique filename
                val fileName = "petaan_image_${System.currentTimeMillis()}.jpg"

                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("Downloading Image")
                    .setDescription("Downloading image from Petaan")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                downloadManager.enqueue(request)

                Toast.makeText(
                    context,
                    "Download started. Check your Downloads folder.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("DOWNLOAD_ERROR", "Error downloading image", e)
                Toast.makeText(
                    context,
                    "Failed to download image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } ?: run {
            Toast.makeText(
                context,
                "Image URL not available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}