package com.kratt.finanzas.data.backup

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

object Argon2idKdf {

    const val VERSION = Argon2Parameters.ARGON2_VERSION_13

    // deriva la clave de la contrasena con argon2id, sin depender de servicios externos
    fun derive(password: ByteArray, params: KdfParams): ByteArray {
        val generator = Argon2BytesGenerator()
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(VERSION)
            .withSalt(params.salt)
            .withMemoryAsKB(params.memoryKiB)
            .withIterations(params.iterations)
            .withParallelism(params.parallelism)
            .build()
        generator.init(parameters)
        val out = ByteArray(params.outputLength)
        generator.generateBytes(password, out)
        return out
    }
}
