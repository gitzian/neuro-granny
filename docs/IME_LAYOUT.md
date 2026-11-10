# Keyboard and dialog accessibility

Although the main interface is rendered using OpenGL ES, password prompts and editing forms use native Android text controls. This preserves keyboard composition, cursor navigation, selection, password masking and integration with accessibility tools.

Dialogs must remain usable when the on-screen keyboard reduces the available space. Input fields and action buttons must be reachable without relying on a fixed keyboard height.

## Manual checks

Test on a small Android 6 (API 23) device and at least one recent Android version:

- Open forms, focus the first and last fields, and verify that Save and Cancel remain reachable.
- Repeat in landscape and with increased system font size.
- Check numeric and text keyboards, including a third-party keyboard where available.
- Dismiss the keyboard with system Back and verify that the underlying form or review is not accidentally submitted.
- Rotate or background the app with a dialog open and confirm that input remains usable.
