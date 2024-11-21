package com.kelompok2.petaan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kelompok2.petaan.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebasestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firebasestore = FirebaseFirestore.getInstance()
        val user = firebaseAuth.currentUser
        if (user != null) {
            val uid = user.uid
            firebasestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding?.apply {
                            name.text = document.getString("name")
                            username.text = document.getString("username")
                            email.text = document.getString("email")
                        }
                    } else {
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
        binding?.apply {
            ProfileTopBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_profile -> {
                        findNavController().navigate(R.id.EditProfileFragment)  // Ganti dengan ID fragment yang sesuai di nav_graph
                        true
                    }
                    else -> false
                }
            }
        }

        binding?.logoutButton?.setOnClickListener {
            // Logout dari Firebase
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "You logged out.", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(requireContext(), AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            requireActivity().finish()
        }
    }
}