# Security model

Neuro Granny is an offline Android memory-training app, not a conventional password manager. It uses Android's application-private storage and device security to protect its local SQLite database.

## Storage modes

Each entry uses one of two modes:

- **Recoverable:** The secret is stored in readable form in the app's private database so it can be shown again when needed.
- **Verify only:** The app stores an individually salted Argon2id verifier instead of the original secret. It can check an answer without keeping a recoverable copy.

Conventional password managers typically encrypt credentials because they need to retrieve them. Neuro Granny's Verify-only mode does not need to recover the original secret. If a database copy becomes accessible to someone else, Recoverable entries can be read, while Verify-only entries require guessing against their verifiers. Argon2id makes guessing more expensive; short or predictable secrets remain easier to guess.

## Database backups and transfers

Database exports are useful for backups and moving data to another device, including entries, review data and image cues. They preserve the selected storage mode: Recoverable entries contain readable secrets, while Verify-only entries contain Argon2id verifiers.

Choose a backup location that other people cannot access. Android's document picker may offer local or cloud-backed locations, but Neuro Granny does not upload backups automatically. Android-managed app backups and device transfers are disabled.

## Device security and data handling

- The app does not request internet access or send telemetry.
- As with other apps that accept secrets, a compromised device or malicious keyboard can expose entered information.

Keep an independent recovery method for important passwords and access codes, particularly when using Verify-only mode.

## Security reports

The project does not claim an independent security audit or certification. When reporting security issues, use test data and do not include real passwords, PINs, database exports or other personal information in public reports.