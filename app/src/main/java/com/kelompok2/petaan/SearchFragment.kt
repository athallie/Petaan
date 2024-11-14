package com.kelompok2.petaan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchView
import com.kelompok2.petaan.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchView.show()

        var dataset = mutableListOf<SearchItem>()
        var adapter = SearchAdapter(dataset) { position ->
            val action = SearchFragmentDirections.actionSearchFragmentToHomepageFragment(
                dataset[position].objectId
            )
            findNavController().navigate(action)
        }
        val recyclerView: RecyclerView = binding.searchRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.HIDDEN) {
                findNavController().navigate(R.id.homepageFragment)
            }
        }
        binding.searchView.editText.doOnTextChanged { text, _, _, _ ->
            if (text!!.isEmpty()) {
                adapter.clear()
            } else {
                lifecycleScope.launch {
                    dataset = Utils().search(text.toString())
                    adapter.updateData(dataset)
                    Log.d("ALGOLIASEARCH", dataset.joinToString("\n"))
                    Log.d("ADAPTERCOUNT", "${adapter.itemCount}")
                }
            }
        }
    }
}