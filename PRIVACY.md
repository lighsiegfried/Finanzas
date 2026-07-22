# Privacy — Mis Finanzas

Mis Finanzas is a fully local, offline personal-finance app. This statement describes how it handles your data.
It is informational and avoids absolute-security claims.

- **Local storage.** Your financial information is stored only on this device. The app has no INTERNET
  permission and includes no network libraries, analytics, telemetry, ads, or crash-reporting services.
- **Encrypted at rest.** The database is encrypted with SQLCipher. The encryption passphrase is wrapped by a
  non-exportable Android Keystore key.
- **No bank connections.** The app never connects to bank accounts and never imports data from financial
  institutions.
- **No automatic upload.** The app does not upload your data anywhere. There is no cloud sync.
- **Manual backups.** Encrypted backups are created only when you choose to. You pick where the backup file is
  saved via the system document picker. If you select a destination backed by a cloud provider, that is your
  decision and is handled by that provider, not by this app.
- **Backup password.** Backups are protected with a password you choose (Argon2id + AES-256-GCM). If you lose
  that password, the corresponding backup cannot be recovered.
- **Uninstalling.** Uninstalling the app may remove the local data on this device. Keep an encrypted backup if
  you want to preserve it.
- **Device security.** If the device itself is compromised (for example, rooted or malware-infected), local
  protections may be reduced. Keep your device secure and up to date.

No online privacy-policy URL is provided; this app operates entirely on-device.
