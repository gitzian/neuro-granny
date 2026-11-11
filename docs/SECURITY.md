# Security model

Neuro Granny is an offline Android memory-training app, not a conventional password manager. It uses Android's application-private storage and device security to protect its local SQLite database.

## Storage modes

Each entry uses one of two modes:

- **Recoverable mode:** Stores secrets in retrievable form in Android's application-private database, allowing users to reveal them during learning and recall practice.
- **Verify only:** The app stores an individually salted Argon2id verifier instead of the original secret. It can check an answer without keeping a recoverable copy.

Conventional password managers typically encrypt credentials because they need to retrieve them. Neuro Granny's Verify-only mode does not need to recover the original secret. If a database copy becomes accessible to someone else, Recoverable entries can be read, while Verify-only entries require guessing against their verifiers. Argon2id makes guessing more expensive; short or predictable secrets remain easier to guess.

## Database backups and transfers

Database exports let you back up your data or transfer it to another device, including entries, review history and image cues. Each entry retains its selected storage mode: Recoverable secrets remain retrievable, while Verify-only entries retain their Argon2id verifiers.

You choose where to save backups using Android's document picker, which supports local and cloud storage providers. Neuro Granny does not upload or synchronize data automatically.

Keep your backups in a location only you can access. Android-managed app backups and device transfers are disabled.

## Device security and data handling

- The app does not request internet access or send telemetry.
- As with other apps that accept secrets, a compromised device or malicious keyboard can expose entered information.

Keep an independent recovery method for important passwords and access codes, particularly when using Verify-only mode.

## Security reports

The project does not claim an independent security audit or certification. When reporting security issues, use test data and do not include real passwords, PINs, database exports or other personal information in public reports.