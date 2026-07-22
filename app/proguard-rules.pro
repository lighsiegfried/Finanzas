# Mis Finanzas - R8 keep rules.
# Each rule is the smallest that solves a verified need. Rationale: docs/release/r8-rules.md.
# No global "-keep class ** { *; }", no whole-app/package keep, no global -dontoptimize.

# --- SQLCipher (net.zetetic:sqlcipher-android): JNI native bindings ---
# la libreria carga codigo nativo por jni; se conservan sus clases y los metodos native
-keep class net.zetetic.database.** { *; }
-keepclasseswithmembers class * {
    native <methods>;
}
-dontwarn net.zetetic.database.**

# --- WorkManager: el worker se instancia por nombre de clase via la factory por defecto ---
-keep class com.kratt.finanzas.reminder.ReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- BouncyCastle: solo se usa el api de bajo nivel de argon2 (sin proveedor jce) ---
# se conserva el generador y sus parametros; el resto del provider puede eliminarse
-keep class org.bouncycastle.crypto.generators.Argon2BytesGenerator { *; }
-keep class org.bouncycastle.crypto.params.Argon2Parameters { *; }
-keep class org.bouncycastle.crypto.params.Argon2Parameters$Builder { *; }
-dontwarn org.bouncycastle.**

# --- Room: el codigo generado se referencia directo; solo se silencian avisos opcionales ---
-dontwarn androidx.room.paging.**
