package com.webtic.qrprint.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.webtic.qrprint.R
import com.webtic.qrprint.models.BaseResponse
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.AppConstants.DB_SERVER
import com.webtic.qrprint.util.AppConstants.DB_USER_NAME
import com.webtic.qrprint.util.AppConstants.DB_USER_PASS
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val serverTv = view.findViewById<TextInputEditText>(R.id.server)
        val databaseTv = view.findViewById<TextInputEditText>(R.id.database)
        val usernameTv = view.findViewById<TextInputEditText>(R.id.username)
        val passwordTv = view.findViewById<TextInputEditText>(R.id.password)
        serverTv.setText(DB_SERVER)
        databaseTv.setText(DB_NAME)
        usernameTv.setText(DB_USER_NAME)
        passwordTv.setText(DB_USER_PASS)
        view.findViewById<Button>(R.id.loginBtn).run {
            setOnClickListener {
                val server = serverTv.text.toString()
                val database = databaseTv.text.toString()
                DB_NAME = database
                val user = usernameTv.text.toString()
                val pass = passwordTv.text.toString()
                if (server.isNotEmpty() &&
                    database.isNotEmpty() &&
                    user.isNotEmpty() &&
                    pass.isNotEmpty()
                )
                    viewModel.tryLogin(connectionManager, server, database, user, pass)
                else

                    Snackbar.make(
                        requireView(),
                        "Az összes mező kitöltendő",
                        Snackbar.LENGTH_LONG
                    ).show()
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner){
            when(it){
                true ->{
                    showLoadingSpinner("Login", "Please wait a moment")
                }
                false ->{
                    hideLoadingSpinner()
                }
            }
        }
        viewModel.loginResult.observe(viewLifecycleOwner){
            when(it){
                is BaseResponse.Loading ->{
                    viewModel.setIsLoading(true)
                }
                is BaseResponse.Success ->{
                    viewModel.setIsLoading(false)
                    findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToMenuFragment())
                }
                is BaseResponse.Error -> {
                    viewModel.setIsLoading(false)
                    Snackbar.make(
                        requireView(),
                        it.msg!!,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                else -> {
                    viewModel.setIsLoading(false)
                }
            }
        }
    }
}