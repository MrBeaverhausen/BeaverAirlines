package com.example.beaverairlines.auth

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.beaverairlines.AuthViewModel
import com.example.beaverairlines.R
import com.example.beaverairlines.data.User
import com.example.beaverairlines.databinding.FragmentSigninBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.login_card.view.*
import kotlinx.android.synthetic.main.signup_card.view.*
import java.security.AccessController.getContext

//THIS FRAGMENT IS PART OF THE LOGIN / SIGN IN PROCEDURE:

class SigninLoginFragment : Fragment() {

    private lateinit var signup_card: Scene
    private lateinit var login_card: Scene
    private lateinit var currentScene: Scene
    private var wasLoginClicked: Boolean = true
    private lateinit var transitionSignup2Login: Transition
    private lateinit var transitionLogin2Signup: Transition
    private var auth = FirebaseAuth.getInstance()
    private val viewModel: AuthViewModel by activityViewModels()
    private lateinit var binding: FragmentSigninBinding



    fun Activity.hideKeyboard(view: View) {
        hideKeyboard(currentFocus ?: View(this))
    }



    fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transitionAnim = TransitionInflater.from(requireContext()).inflateTransition(
            android.R.transition.move
        )

        sharedElementEnterTransition = transitionAnim
        sharedElementReturnTransition = transitionAnim

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSigninBinding.inflate(inflater, container, false)

        return binding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val wasLoginClicked = requireArguments().getBoolean("isLoginClicked", false)

        //TO SWITCH BETWEEN LOGIN AND SIGN UP FRAMES, SCENES WERE USED:
        signup_card = Scene.getSceneForLayout(binding.cardLayout, R.layout.signup_card, requireActivity())
        login_card = Scene.getSceneForLayout(binding.cardLayout, R.layout.login_card, requireActivity())


        if (wasLoginClicked) {
            login_card.enter()
            currentScene = login_card
            binding.signUpCardSwapFooterTv.text = getString(R.string.new_to_beaver_airlines)
            binding.signUpCardSwapBttn.text = getString(R.string.register)

            binding.cardLayout.login_login_bttn.setOnClickListener {
                val email: String =
                    binding.cardLayout.login_emaiInput_et.text.toString().trim { it <= ' ' }
                val password: String =
                    binding.cardLayout.login_passwordInput_et.text.toString().trim { it <= ' ' }
                loginUser(email, password)
            }

        } else {
            currentScene = signup_card
            signup_card.enter()
            binding.signUpCardSwapFooterTv.text = getString(R.string.Joined_us_before)
            binding.signUpCardSwapBttn.text = getString(R.string.login)

            binding.cardLayout.signUp_signUp_bttn.setOnClickListener {
                val name: String =
                    binding.cardLayout.signUp_nameInput_et.text.toString().trim { it <= ' ' }
                val email: String =
                    binding.cardLayout.signUp_emaiInput_et.text.toString().trim { it <= ' ' }
                val password: String =
                    binding.cardLayout.signUp_passwordInput_et.text.toString().trim { it <= ' ' }
                registerUser(name, email, password)
            }
        }




        //TRANSITIONS BETWEEN SCENES:
        transitionSignup2Login = TransitionInflater.from(requireActivity())
            .inflateTransition(R.transition.card_change_signup2login)
        transitionLogin2Signup = TransitionInflater.from(requireActivity())
            .inflateTransition(R.transition.card_change_login2signup)


        exitTransition = MaterialElevationScale(false)
        reenterTransition = MaterialElevationScale(true)


        binding.signUpCardSwapBttn.setOnClickListener {
            if (currentScene == signup_card) {
                TransitionManager.go(login_card, transitionSignup2Login)
                currentScene = login_card

                binding.cardLayout.login_login_bttn.setOnClickListener {
                    val email: String =
                        binding.cardLayout.login_emaiInput_et.text.toString().trim { it <= ' ' }
                    val password: String =
                        binding.cardLayout.login_passwordInput_et.text.toString().trim { it <= ' ' }
                    loginUser(email, password)
                }

                binding.signUpCardSwapFooterTv.text = getString(R.string.new_to_beaver_airlines)
                binding.signUpCardSwapBttn.text = getString(R.string.register)

            } else {
                TransitionManager.go(signup_card, transitionLogin2Signup)
                currentScene = signup_card

                binding.cardLayout.signUp_signUp_bttn.setOnClickListener {
                    val name: String =
                        binding.cardLayout.signUp_nameInput_et.text.toString().trim { it <= ' ' }
                    val email: String =
                        binding.cardLayout.signUp_emaiInput_et.text.toString().trim { it <= ' ' }
                    val password: String =
                        binding.cardLayout.signUp_passwordInput_et.text.toString()
                            .trim { it <= ' ' }
                    registerUser(name, email, password)
                }

                binding.signUpCardSwapFooterTv.text = getString(R.string.Joined_us_before)
                binding.signUpCardSwapBttn.text = getString(R.string.login)
            }
        }
    }


    //FUNCTIONALITY FOR REGISTERING A NEW USER USING FIREBASE:
    private fun registerUser(name: String, email: String, password: String) {

        if (validateRegistration(name, email, password)) {
            Toast.makeText(getContext(), "Please wait...", Toast.LENGTH_SHORT).show()

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val newUser = User(
                        userId = viewModel.userIdGenerator(),
                        firstName = name,
                        email = email,
                        mileHighClubNbr = viewModel.mileHighGenerator()
                    )

                    viewModel.setUser(newUser)
                    loginUser(email, password)

                } else {
                    Toast.makeText(getContext(), task.exception!!.message, Toast.LENGTH_SHORT).show()

                }
            }
        }
    }

    //FUNCTIONALITY FOR VALIDATING USER INPUT:
    private fun validateRegistration(name: String, email: String, password: String): Boolean {

        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter your name")
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter your email address")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter a password")
                false
            }
            else -> {
                true
            }
        }
    }


    //FUNCTIONALITY FOR LOGGING IN:
    private fun loginUser(email: String, password: String) {

        if (validateLogin(email, password)) {
            Toast.makeText(getContext(), "Please wait...", Toast.LENGTH_SHORT).show()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModel.currentUser = auth.currentUser
                        findNavController().navigate(R.id.action_signinFragment_to_NavControllerFragment)
                    } else {
                        Toast.makeText(getContext(), "Login failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Log.e("firebase login", it.message!!)
                }
        }
    }


    //FUNCTIONALITY FOR VALIDATING LOGIN:
    private fun validateLogin(email: String, password: String): Boolean {

        return when {
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter your email address")
                //Toast.makeText(getContext(), "Please enter your email address", Toast.LENGTH_SHORT).show()
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter your password")
                //Toast.makeText(getContext(), "Please enter a password", Toast.LENGTH_SHORT).show()
                false
            }
            else -> {
                true
            }
        }
    }


    //FUNCTIONALITY FOR SHOWING ERROR MSG WHEN F.E. INPUT WAS WRONG:
    fun showErrorSnackBar(message: String) {
        val snackbar = Snackbar.make(requireActivity().findViewById(android.R.id.content), message,
            Snackbar.LENGTH_LONG)

        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700))

        snackbar.show()
    }


}











