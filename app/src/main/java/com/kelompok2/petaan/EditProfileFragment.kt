package com.kelompok2.petaan


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private var bottomNav: BottomNavigationView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(layoutInflater, container, false)
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebasestore = FirebaseFirestore.getInstance()

        var oldName: String? = null
        var oldUsername: String? = null
        var oldEmail: String? = null

        val user = firebaseAuth.currentUser
        if (user != null) {
            val uid = user.uid
            firebasestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        oldName = document.getString("name")
                        oldUsername = document.getString("username")
                        oldEmail = document.getString("email")
                        Log.d("OLDNAME", "$oldName")
                        binding?.apply {
                            editName.setText(oldName)
                            editUsername.setText(oldName)
                            editEmail.setText(oldEmail)
                        }
                    }
                }
        }

        //Back button
        binding?.EditProfileTopBar?.setNavigationOnClickListener {
            bottomNav?.visibility = View.VISIBLE
            findNavController().popBackStack()
        }

        //Hide bottom nav
        bottomNav?.visibility = View.GONE

        // Save button logic
        binding?.saveButton?.setOnClickListener {

            if (user != null) {
                var allFilled = true
                val editName = binding?.editName
                val editUsername = binding?.editUsername
                val editEmail = binding?.editEmail

                if (editName?.text.toString().trim().isEmpty()) {
                    editName?.error = "This field is required."
                    allFilled = false
                }

                if (editUsername?.text.toString().trim().isEmpty()) {
                    editUsername?.error = "This field is required."
                    allFilled = false
                }

                if (editEmail?.text.toString().trim().isEmpty()) {
                    editEmail?.error = "This field is required."
                    allFilled = false
                }

                val newName = editName?.text.toString().trim()
                val newUsername = editUsername?.text.toString().trim()
                val newEmail = editEmail?.text.toString().trim()

                if (
                    allFilled &&
                    newName != oldName &&
                    newUsername != oldUsername &&
                    newEmail != oldEmail
                ) {
                    Log.d("NAME", "$oldName/$newName")
                    val uid = user.uid
                    val userData = mapOf(
                        "name" to newName,
                        "username" to newUsername,
                        "email" to newEmail
                    )
                    firebasestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Nothing has changed.", Toast.LENGTH_SHORT).show()
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
