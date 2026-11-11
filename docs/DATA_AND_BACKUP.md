# Data and backups

Neuro Granny stores its data in a private SQLite database on the device. Review history and scheduling state are stored with the records they belong to.

## Export

Database exports are useful for backups and transferring data to another device, including entries, review history and image cues.

- Export uses Android's document picker to choose a destination.
- The exported SQLite database preserves each entry's selected storage mode: Recoverable entries allow secrets to be retrieved, while Verify-only entries retain their Argon2id verifiers.
- A user-selected document provider may store a backup in the cloud. Neuro Granny does not upload it automatically.

Keep exported databases in a location only you can access.

## Import

- Import is limited to 50 MiB and validates the database structure and contents before applying changes.
- Entries with matching titles require an explicit choice to keep the local record, replace it or keep both.
- Record IDs are local identifiers, not stable identifiers across devices. Related history is remapped when imported.

There is no automatic cloud synchronization or built-in recovery service. Keep an independent backup of important secrets.