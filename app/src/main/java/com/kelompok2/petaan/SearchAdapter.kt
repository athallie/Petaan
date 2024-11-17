package com.kelompok2.petaan

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.fallback
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchAdapter(private val dataset: MutableList<SearchItem>, private val onItemClicked: (Int) -> Unit) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private lateinit var context: Context
    private lateinit var client: Client

    class ViewHolder(view: View, onItemClicked: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val container: ConstraintLayout
        val subject: TextView
        val location: TextView
        val image: ImageView
        init {
            container = view.findViewById<ConstraintLayout>(R.id.search_item)
            subject = view.findViewById<TextView>(R.id.search_result_subject)
            location = view.findViewById<TextView>(R.id.search_result_location)
            image = view.findViewById<ImageView>(R.id.search_result_image)
            view.setOnClickListener {
                onItemClicked(this.adapterPosition)
            }
        }
    }

    fun updateData(dataset: MutableList<SearchItem>) {
        this.dataset.clear()
        this.dataset.addAll(dataset)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_item, parent, false)
        context = view.context
        client = AppWriteHelper().getClient(context)
        return ViewHolder(view) { position ->
            onItemClicked(position)
        }
    }

    override fun onBindViewHolder(holder: SearchAdapter.ViewHolder, position: Int) {
        holder.subject.text = dataset[position].subject
        holder.location.text = dataset[position].location
        CoroutineScope(Dispatchers.IO).launch {
            holder.image.load(
                try {
                    Storage(client).getFileView(
                        bucketId = BuildConfig.APP_WRITE_BUCKET_ID,
                        fileId = dataset[position].objectId.replace("\"", "")
                    )
                } catch (e: AppwriteException) {
                    Log.d("APPWRITEEXCEPTION", "$e")
                    null
                }
            ) {
                fallback(R.drawable.baseline_broken_image_24)
            }
        }
    }

    override fun getItemCount(): Int = dataset.size

    fun clear() {
        val size = dataset.size
        dataset.clear()
        notifyItemRangeRemoved(0, size)
    }
}