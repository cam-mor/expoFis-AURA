package com.roamoralesgonzalez.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _magneticStrength = MutableStateFlow(0f)
    val magneticStrength: StateFlow<Float> = _magneticStrength

    private val _warningLevel = MutableStateFlow(0)
    val warningLevel: StateFlow<Int> = _warningLevel

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    fun updateMagneticStrength(strength: Float) {
        viewModelScope.launch {
            _magneticStrength.value = strength
            updateWarningLevel(strength)
        }
    }

    private fun updateWarningLevel(strength: Float) {
        val level = when {
            strength > 500f -> 3
            strength > 300f -> 2
            strength > 100f -> 1
            else -> 0
        }
        _warningLevel.value = level
    }

    fun setMonitoring(isActive: Boolean) {
        _isMonitoring.value = isActive
    }
}
