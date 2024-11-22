package com.kelompok2.petaan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.kelompok2.petaan.databinding.FragmentListviewBinding
import io.appwrite.services.Storage
import kotlinx.coroutines.launch

class ListViewFragment : Fragment() {

    private var binding: FragmentListviewBinding? = null
    private var bottomNav: BottomNavigationView? = null
    private var deleteButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentListviewBinding.inflate(layoutInflater, container, false)
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        deleteButton = binding?.listviewDeleteButton
        val view = binding!!.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = Firebase.firestore
        val appWriteClient = AppWriteHelper().getClient(requireContext())

        var dataset = mutableListOf<SearchItem>()
        var adapter: ListAdapter? = null
        val recyclerView: RecyclerView = binding!!.roiRecylerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        db.collection("reports").get().addOnSuccessListener { docs ->
            docs.forEach { doc ->
                var location: GeoPoint? = null
                try {
                    location = doc.getGeoPoint("location")
                } catch (e: Exception) {
                    Log.d("GEOPOINT", "$e")
                }
                dataset.add(SearchItem(
                    objectId = doc.id,
                    subject = doc.getString("subject").toString(),
                    location = "${location!!.latitude}, ${location.longitude}"
                ))
            }
            adapter = ListAdapter(dataset) { position ->
                val action = ListViewFragmentDirections.actionListViewFragmentToHomepageFragment(
                    dataset[position].objectId
                )
                findNavController().navigate(action)
            }
            recyclerView.adapter = adapter
            Log.d("ADAPTERCOUNT", "${adapter.itemCount}")
        }.addOnFailureListener { err ->
            Log.d("FIRESTOREERR", "$err")
        }

        binding!!.roiAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding!!.roiAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_delete) {
                val isDelete = adapter?.changeDeleteMode()
                Log.d("DELETEMODE", "MAIN")
                adapter?.notifyDataSetChanged()
                if (isDelete == true) {
                    bottomNav?.visibility = View.GONE
                    deleteButton?.visibility = View.VISIBLE
                } else {
                    bottomNav?.visibility = View.VISIBLE
                    deleteButton?.visibility = View.GONE
                }
            }
            true
        }
        deleteButton?.setOnClickListener {
            val items = adapter?.getSelectedItems()
            try {
                items?.forEach { item ->
                    db.collection("reports")
                        .document(item)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("Firestore", "DocumentSnapshot successfully deleted!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error deleting document", e)
                        }
                    lifecycleScope.launch {
                        val result = Utils().deleteAlgoliaIndex(item)
                        val result1 = Storage(appWriteClient).deleteFile(BuildConfig.APP_WRITE_BUCKET_ID, item)
                        Log.d("DELETEALGOLIAINDEX", "$result")
                        Log.d("DELETEIMAGEAPPWRITE", "$result1")
                    }
                }
                Toast.makeText(requireContext(), "Delete success.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.d("DELETEFAILURE", "$e")
                Toast.makeText(requireContext(), "Delete failed.", Toast.LENGTH_SHORT).show()
            } finally {
                bottomNav?.visibility = View.VISIBLE
                deleteButton!!.visibility = View.GONE
                adapter?.clearData()
                adapter?.changeDeleteMode()
                adapter?.notifyDataSetChanged()
            }
        }
    }
}