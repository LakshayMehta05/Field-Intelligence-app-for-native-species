package com.geg.fieldintel.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geg.fieldintel.data.model.ScanResult
import com.geg.fieldintel.data.remote.RetrofitClient
import com.geg.fieldintel.data.repository.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Scanning : ScannerUiState()
    data class Result(val result: ScanResult) : ScannerUiState()
}

class ScannerViewModel(
    private val repository: PlantRepository = PlantRepository(RetrofitClient.botanicalGuideApi)
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState

    fun onPhotoCaptured(file: File, lat: Double? = null, lng: Double? = null) {
        _uiState.value = ScannerUiState.Scanning
        viewModelScope.launch {
            val result = repository.identify(file, lat, lng)
            _uiState.value = ScannerUiState.Result(result)
        }
    }

    fun reset() {
        _uiState.value = ScannerUiState.Idle
    }
}
