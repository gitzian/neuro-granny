# Contributing code

The project uses Java and aims for readable, reviewable changes.

## Style

- **Use four spaces for indentation; do not use hard tabs.** Keep indentation consistent within each file.
- Use braces for `if`, `else`, loops and other control blocks.
- Space operators and commas; avoid multiple statements on one line.
- Prefer descriptive names, including units where relevant (`durationSeconds`, `timestampMillis`).
- Name repeated or non-obvious numeric values with constants.
- Keep UI rendering, Android platform integration, database operations and cryptographic logic separate.
- Avoid unrelated formatting changes in behavioral patches.

Example (four-space indentation):

```java
if (reviewIsDue) {
    refreshDueState();
    notifyUser();
}
```

## Changes and verification

Document behavioral changes and include appropriate tests. Run the automated checks described in [Test plan](TEST_PLAN.md) and validate Android-specific behavior on a device or emulator.

Security-sensitive changes must preserve the guarantees described in [Security](SECURITY.md).
