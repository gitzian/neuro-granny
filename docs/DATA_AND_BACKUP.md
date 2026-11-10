# Data and backups

Neuro Granny stores its data in a private SQLite database on the device. Review history and scheduling state are stored with the records they belong to.

## Export

- Export uses Android's document picker to choose a destination.
- The exported file is **unencrypted SQLite**. Recoverable entries include their secrets in readable form.
- No device-specific encryption key is required to read the export.
- A user-selected document provider may store a backup in the cloud. Neuro Granny does not upload it automatically.

**Protect exported files as you would protect the passwords they contain.**

## Import

- Import is limited to 50 MiB and validates the database structure and contents before applying changes.
- Entries with matching titles require an explicit choice to keep the local record, replace it or keep both.
- Record IDs are local identifiers, not stable identifiers across devices. Related history is remapped when imported.
- A matching title or a different Argon2id salt does not establish that two underlying secrets are equal or different.
- Applying the selected import is transactional. Canceling or failing an import must not partially modify the live database.

There is no automatic cloud synchronization or built-in recovery service. Keep an independent backup of important secrets.
