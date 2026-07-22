package com.kratt.finanzas.data.backup

// errores del formato de respaldo, sus mensajes no llevan datos sensibles
sealed class BackupFormatException(message: String) : Exception(message) {
    class BadMagic : BackupFormatException("bad magic")
    class UnsupportedVersion(val version: Int) : BackupFormatException("unsupported version")
    class UnsupportedKdf(val id: Int) : BackupFormatException("unsupported kdf")
    class UnsupportedEncryption(val id: Int) : BackupFormatException("unsupported encryption")
    class UnsupportedSchema(val version: Int) : BackupFormatException("unsupported schema")
    class InvalidLength(val reason: String) : BackupFormatException("invalid length: $reason")
    class Truncated : BackupFormatException("truncated")
    class AuthenticationFailed : BackupFormatException("authentication failed")
}
