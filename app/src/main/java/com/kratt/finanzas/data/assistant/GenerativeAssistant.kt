package com.kratt.finanzas.data.assistant

import com.kratt.finanzas.domain.assistant.AssistantIntent

// abstraccion del modo avanzado opcional; en esta fase no se incluye ningun runtime generativo
// un runtime del sistema podria conectarse aqui sin volverse la fuente de la verdad financiera
interface GenerativeAssistant {

    // indica si hay un runtime local listo para usar; en esta fase siempre es falso
    fun isRuntimeReady(): Boolean

    // puede proponer una mejor interpretacion de la pregunta; nunca calcula montos ni toca la base
    // devuelve null si no puede ayudar, y el motor sigue con la interpretacion determinista
    suspend fun proposeIntent(query: String): AssistantIntent?
}

// implementacion por defecto: no hay modo avanzado, todo cae al modo compatible determinista
class NoOpGenerativeAssistant : GenerativeAssistant {
    override fun isRuntimeReady(): Boolean = false
    override suspend fun proposeIntent(query: String): AssistantIntent? = null
}
