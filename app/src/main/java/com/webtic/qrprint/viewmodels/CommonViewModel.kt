package com.webtic.qrprint.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class CommonViewModel (application: Application): AndroidViewModel(application) {


    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    fun setIsLoading(b: Boolean) {
        _isLoading.value = b
    }

}