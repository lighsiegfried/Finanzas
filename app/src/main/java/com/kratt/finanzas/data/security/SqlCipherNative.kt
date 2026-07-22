package com.kratt.finanzas.data.security

// carga la libreria nativa de sqlcipher una sola vez antes del primer uso
object SqlCipherNative {
    @Volatile
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        System.loadLibrary("sqlcipher")
        loaded = true
    }
}
