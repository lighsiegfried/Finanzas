package com.kratt.finanzas.data.backup

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.SecureRandom
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// mide cuanto tarda el derivado argon2id con el perfil por defecto en el dispositivo
@RunWith(AndroidJUnit4::class)
class Argon2DurationTest {

    @Test
    fun defaultProfileDerivationDuration() {
        val salt = ByteArray(KdfParams.SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val params = KdfParams.defaultProfile(salt)
        val password = "contraseña-de-respaldo-medición".toByteArray()

        // descarta la primera corrida (jit/carga de clases) y mide varias
        Argon2idKdf.derive(password, params)
        val samples = LongArray(3)
        for (i in samples.indices) {
            val start = System.nanoTime()
            Argon2idKdf.derive(password, params)
            samples[i] = (System.nanoTime() - start) / 1_000_000
        }
        samples.sort()
        val median = samples[samples.size / 2]
        Log.i(
            "Phase2C",
            "argon2_default_profile mem=${params.memoryKiB}KiB iters=${params.iterations} " +
                "par=${params.parallelism} median_ms=$median samples=${samples.joinToString()}",
        )
        // cota amplia: solo confirma que termina en un tiempo razonable en el dispositivo
        assertTrue("argon2 debe completar en un tiempo razonable, fue ${median}ms", median in 1..10_000)
    }
}
