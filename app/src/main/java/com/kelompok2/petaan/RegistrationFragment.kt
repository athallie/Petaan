package com.kelompok2.petaan

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kelompok2.petaan.databinding.FragmentRegistrationBinding

class RegistrationFragment : Fragment() {

    private var binding: FragmentRegistrationBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRegistrationBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val binding = binding ?: return // biar binding ga null

        binding!!.registerButton.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val username = binding.editTextUsername.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()


            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty() && name.isNotEmpty()) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            val userId = firebaseAuth.currentUser?.uid

                            // Create a user object with name and username
                            val user = hashMapOf(
                                "name" to name,
                                "username" to username,
                                "email" to email
                            )

                            // Store user data in Firestore under "users" collection
                            if (userId != null) {
                                firestore.collection("users").document(userId)
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Registration Successful", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
            } else {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}