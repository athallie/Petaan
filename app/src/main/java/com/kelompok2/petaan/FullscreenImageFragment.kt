package com.kelompok2.petaan

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil3.load
import coil3.request.crossfade
import coil3.request.fallback
import coil3.request.placeholder
import coil3.size.Scale
import com.kelompok2.petaan.databinding.FragmentFullscreenImageBinding
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullscreenImageFragment : Fragment() {

    private val args: FullscreenImageFragmentArgs by navArgs()
    private lateinit var client : Client
    private lateinit var context: Context
    private var binding: FragmentFullscreenImageBinding? = null

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

        // Inisialisasi ImageView
        val fullscreenImage = binding!!.fullscreenImage
        val closeButton = binding!!.closeButton
        // Load image menggunakan fileId
        CoroutineScope(Dispatchers.IO).launch {
            val fileView = try {
                Storage(client).getFileView(
                    bucketId = BuildConfig.APP_WRITE_BUCKET_ID,
                    fileId = args.fileId
                )
            } catch (e: AppwriteException) {
                Log.d("APPWRITEEXCEPTION", "$e")
                null
            }

            withContext(Dispatchers.Main) {
                fullscreenImage.load(fileView) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder_image) // Gambar sementara
                    fallback(R.drawable.baseline_broken_image_24)
                }
            }
        }


        // Handle close button
        closeButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}