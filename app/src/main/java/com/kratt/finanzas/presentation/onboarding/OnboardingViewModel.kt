package com.kratt.finanzas.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.onboarding.OnboardingPreferences
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {

    // marca la configuracion inicial como completada para no volver a mostrarla
    fun complete() {
        viewModelScope.launch { onboardingPreferences.setCompleted() }
    }
}
