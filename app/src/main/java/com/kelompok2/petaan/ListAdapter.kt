package com.kelompok2.petaan

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.fallback
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListAdapter(private val dataset: MutableList<SearchItem>, private val onItemClicked: (Int) -> Unit) : RecyclerView.Adapter<ListAdapter.ViewHolder>()  {
    private var isDelete = false
    private lateinit var context: Context
    private lateinit var client: Client
    private var selectedItems = mutableListOf<String>()

    class ViewHolder(view: View, onItemClicked: (Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val container: RelativeLayout
        val subject: TextView
        val location: TextView
        val image: ImageView
        val checkBox: CheckBox?

        init {
            container = view.findViewById(R.id.listview_item)
            subject = view.findViewById(R.id.listview_subject)
            location = view.findViewById<TextView>(R.id.listview_location)
            image = view.findViewById<ImageView>(R.id.listview_image)
            checkBox = view.findViewById(R.id.deleteCheckbox)
            view.setOnClickListener {
                onItemClicked(this.adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.listview_item, parent, false)
        context = view.context
        client = AppWriteHelper().getClient(context)
        return ViewHolder(view) { position ->
            onItemClicked(position)
        }
    }

    override fun onBindViewHolder(
        holder: ListAdapter.ViewHolder,
        position: Int
    ) {
        if (isDelete) {
            holder.checkBox?.visibility = View.VISIBLE
            holder.checkBox?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(dataset[position].objectId)
                } else {
                    selectedItems.remove(dataset[position].objectId)
                }
            }
            Log.d("ISDELETE", "TRUE")
        } else {
            holder.checkBox?.visibility = View.INVISIBLE
            Log.d("ISDELETE", "FALSE")
        }
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

    fun changeDeleteMode(): Boolean {
        isDelete = !isDelete
        return isDelete
    }

    fun getSelectedItems(): MutableList<String> {
        return selectedItems
    }

    fun clearData() {
        dataset.removeIf { it.objectId in selectedItems }
        selectedItems = mutableListOf<String>()
    }
}