package com.webtic.qrprint.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.webtic.qrprint.models.BaseResponse
import com.webtic.qrprint.util.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginViewModel(application: Application): CommonViewModel(application) {
    val loginResult: MutableLiveData<BaseResponse<String>> = MutableLiveData()
    fun tryLogin(connectionManager: ConnectionManager, serverAddress: String, dbName: String, dbUserName: String, dbUserPass: String){
        loginResult.value = BaseResponse.Loading()
        viewModelScope.launch {
            try{
                val loginResponse = connectionManager.tryLogin(serverAddress, dbName, dbUserName, dbUserPass)
                when(loginResponse){
                    LoginSuccess ->{
                        loginResult.value = BaseResponse.Success("Success")
                    }
                    is AuthenticationError ->{
                        loginResult.value = BaseResponse.Error("Hibás bejelentkezési adatok!")
                    }
                    is NetworkError -> {
                        loginResult.value = BaseResponse.Error("Hálózati hiba!")
                    }
                    is LoginError ->{
                        loginResult.value = BaseResponse.Error("Ismeretlen hiba!")
                    }
                }

            }
            catch (ex: Exception){
                loginResult.value = BaseResponse.Error(ex.message)
            }
        }
    }
}