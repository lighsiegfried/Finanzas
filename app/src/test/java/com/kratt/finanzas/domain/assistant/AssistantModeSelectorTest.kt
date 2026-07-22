package com.kratt.finanzas.domain.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantModeSelectorTest {

    @Test
    fun deterministic_when_device_not_supported() {
        assertEquals(
            AssistantMode.DETERMINISTIC_LOCAL,
            AssistantModeSelector.select(deviceSupported = false, userEnabled = true, runtimeReady = true),
        )
    }

    @Test
    fun deterministic_when_runtime_not_ready() {
        assertEquals(
            AssistantMode.DETERMINISTIC_LOCAL,
            AssistantModeSelector.select(deviceSupported = true, userEnabled = true, runtimeReady = false),
        )
    }

    @Test
    fun generative_only_when_all_conditions_met() {
        assertEquals(
            AssistantMode.OPTIONAL_GENERATIVE_LOCAL,
            AssistantModeSelector.select(deviceSupported = true, userEnabled = true, runtimeReady = true),
        )
    }

    @Test
    fun availability_reflects_device_and_user() {
        assertEquals(GenerativeAvailability.UNSUPPORTED_DEVICE, AssistantModeSelector.availability(false, true))
        assertEquals(GenerativeAvailability.DISABLED_BY_USER, AssistantModeSelector.availability(true, false))
        assertEquals(GenerativeAvailability.AVAILABLE, AssistantModeSelector.availability(true, true))
    }
}
