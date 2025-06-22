package com.example.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    var place = MutableStateFlow<List<Feature>?>(null)

    fun getPlace(longitudeNow: Double, latittudeNow: Double){
        viewModelScope.launch {
            val placeInfo = RetrofitServices.searchPlacesInfo.getPlaceInfo(longitudeNow,latittudeNow)
            place.value = placeInfo.features



        }
    }
}

