package com.kelompok2.petaan

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.algolia.client.api.SearchClient
import com.algolia.client.model.search.SearchParamsObject
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.GeoPoint
import kotlinx.serialization.json.*

class Utils {
    private val searchClient = SearchClient(BuildConfig.ALGOLIA_APP_ID, BuildConfig.ALGOLIA_SEARCH_KEY)

    fun addFirestoreDocument(
        context: Context,
        collection: CollectionReference,
        data: HashMap<String, Any>,
        onSuccessCallback: (() -> Unit)? = null,
        onFailedCallback: (() -> Unit)? = null
    ) {
        collection
            .add(data)
            .addOnSuccessListener { dr ->
                Log.d("Firebase", "DocumentSnapshot ID: ${dr.id}")
                Toast.makeText(context, "Report added!", Toast.LENGTH_SHORT).show()
                onSuccessCallback?.invoke()
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error adding document", e)
                Toast.makeText(context, "Failed to add report: $e", Toast.LENGTH_SHORT).show()
                onFailedCallback?.invoke()
            }
    }

    fun addFirestoreDocument(
        context: Context,
        documentId: String,
        collection: CollectionReference,
        data: HashMap<String, Any>,
        progressIndicator: ProgressBar?,
        onSuccessCallback: (() -> Unit)? = null,
        onFailedCallback: (() -> Unit)? = null
    ) {
        if (progressIndicator != null) {
            progressIndicator.visibility = View.VISIBLE
        }
        collection.document(documentId)
            .set(data)
            .addOnSuccessListener { dr ->
                if (progressIndicator != null) {
                    progressIndicator.visibility = View.INVISIBLE
                }
                Log.d("Firebase", "$documentId added.")
                Toast.makeText(context, "Report added!", Toast.LENGTH_SHORT).show()
                onSuccessCallback?.invoke()
            }
            .addOnFailureListener { e ->
                if (progressIndicator != null) {
                    progressIndicator.visibility = View.INVISIBLE
                }
                Log.w("Firebase", "Error adding document", e)
                Toast.makeText(context, "Failed to add report: $e", Toast.LENGTH_SHORT).show()
                onFailedCallback?.invoke()
            }
    }

    suspend fun indexRecordsToAlgolia(data: HashMap<String, Any>, objectId: String) {
        val location: GeoPoint? = data.get("location") as GeoPoint
        val geoloc = buildJsonObject {
            put("lat", location?.latitude)
            put("lng", location?.longitude)
        }
        val record = buildJsonObject {
            put("subject", data.get("subject").toString())
            put("_geoloc", geoloc)
            put("severity", data.get("severity").toString().toInt())
        }
        searchClient.addOrUpdateObject("reports_index", objectId, record)
    }

    suspend fun search(query: String): MutableList<SearchItem> {
        val list = mutableListOf<SearchItem>()
        val results = searchClient.searchSingleIndex(
            indexName = "reports_index",
            searchParams = SearchParamsObject(
                query = query
            )
        )
        results.hits.forEach { hit ->
            val location = hit.additionalProperties?.get("_geoloc") as JsonObject
            val searchItem = SearchItem(
                subject = hit.additionalProperties?.get("subject").toString().replace("\"", ""),
                location = "${location["lat"]}, ${location["lng"]}",
                objectId = hit.objectID,
            )
            list.add(searchItem)
        }
        return list
    }
}
