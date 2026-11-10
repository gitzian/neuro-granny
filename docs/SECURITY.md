# Security model

Neuro Granny is an offline memory-training tool, **not an encrypted password vault**. It does not provide independent database encryption or a recovery service.

## Storage modes

- **Recoverable:** Stores the secret in readable form in the app's private SQLite database so it can be shown again.
- **Verify only:** Stores an individually salted Argon2id verifier. The original secret cannot be recovered by the app.

Argon2id makes offline guessing more expensive but cannot make a weak PIN or password strong. Anyone who obtains a copy of the database may attempt guesses. Conversion to Verify-only mode does not guarantee that older copies, exports or physical storage no longer contain the plaintext.

## Protections and limits

- Android's app-private storage provides access isolation on a normally secured device. The database is **not separately encrypted** by Neuro Granny.
- Android backup and device-transfer mechanisms are disabled for the app; manually exported database files are **unencrypted**.
- The app does not request network access or send telemetry.
- A compromised device, malicious keyboard or privileged attacker can observe entered secrets.
- Deletion removes records from the active database, but is not a guarantee of secure erasure from flash storage or old backups.

**Do not use Neuro Granny as the sole storage location for important account credentials.** Protect exports and keep an independent recovery method.

The project does not claim an independent security audit or certification. Security reports should avoid including real passwords, PINs, database exports or other secrets.
