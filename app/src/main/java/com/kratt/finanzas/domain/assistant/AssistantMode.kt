package com.kratt.finanzas.domain.assistant

// modos de operacion del asistente local
enum class AssistantMode {
    // siempre disponible, sin modelo generativo, funciona en android 11
    DETERMINISTIC_LOCAL,

    // solo cuando el dispositivo y el runtime lo soportan; nunca es la fuente de la verdad
    OPTIONAL_GENERATIVE_LOCAL,
}

// disponibilidad del modo avanzado en este dispositivo
enum class GenerativeAvailability {
    AVAILABLE,
    UNSUPPORTED_DEVICE,
    DISABLED_BY_USER,
}

// estado que la ui muestra sobre el asistente
data class AssistantStatus(
    val activeMode: AssistantMode,
    val generativeAvailability: GenerativeAvailability,
)

// decide el modo y la disponibilidad de forma pura, sin depender de android
object AssistantModeSelector {

    // el modo avanzado solo se usa si el dispositivo lo soporta, el usuario lo activo y el runtime esta listo
    fun select(deviceSupported: Boolean, userEnabled: Boolean, runtimeReady: Boolean): AssistantMode =
        if (deviceSupported && userEnabled && runtimeReady) {
            AssistantMode.OPTIONAL_GENERATIVE_LOCAL
        } else {
            AssistantMode.DETERMINISTIC_LOCAL
        }

    fun availability(deviceSupported: Boolean, userEnabled: Boolean): GenerativeAvailability = when {
        !deviceSupported -> GenerativeAvailability.UNSUPPORTED_DEVICE
        !userEnabled -> GenerativeAvailability.DISABLED_BY_USER
        else -> GenerativeAvailability.AVAILABLE
    }
}
