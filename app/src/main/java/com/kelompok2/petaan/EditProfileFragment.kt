package com.kelompok2.petaan


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kelompok2.petaan.databinding.FragmentEditProfileBinding
import kotlin.apply
import kotlin.to
import kotlin.toString

class EditProfileFragment : Fragment() {

    private var binding: FragmentEditProfileBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebasestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(layoutInflater, container, false)
        return binding!!.root
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
                            editName.setText(document.getString("name"))
                            editUsername.setText(document.getString("username"))
                            editEmail.setText(document.getString("email"))
                        }
                    }
                }
        }

        // Save button logic
        binding?.saveButton?.setOnClickListener {
            val name = binding?.editName?.text.toString()
            val username = binding?.editUsername?.text.toString()
            val email = binding?.editEmail?.text.toString()

            if (user != null) {
                val uid = user.uid
                val userData = mapOf(
                    "name" to name,
                    "username" to username,
                    "email" to email
                )

                firebasestore.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding?.apply {

            val toolbar = view.findViewById<MaterialToolbar>(R.id.EditProfileTopBar)

            (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
            (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

            toolbar.setNavigationOnClickListener {

                findNavController().navigateUp()
            }
        }

    }
}
