package com.kelompok2.petaan

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.kelompok2.petaan.databinding.FragmentLandingBinding

class LandingFragment : Fragment() {

    private var binding: FragmentLandingBinding? = null
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLandingBinding.inflate(layoutInflater, container, false)
        val view = binding!!.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is already logged in, navigate to MainActivity
            startActivity(
                Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            requireActivity().finish()
        } else {
            // User is not logged in, show login/register options
            binding?.landingLoginButton?.setOnClickListener {
                (activity as AuthActivity).navigateToFragment(LoginFragment())
            }
            binding?.landingRegisterButton?.setOnClickListener {
                (activity as AuthActivity).navigateToFragment(RegistrationFragment())
            }
        }
    }
}