# Agent Guidelines & UI Design Rules

## UI Accessibility & Viewport Stability
- Always ensure all critical action buttons (especially primary execution buttons like "Up Next", "Done", "Start", etc.) are 100% accessible, fully visible, and meet the minimum 48dp touch target requirement.
- Never allow expanding headers, banners, or informational popups to push primary action buttons off the bottom of the screen or squeeze them so they cannot be read or tapped.
- Responsive scaling: When top control panels or drawers expand, dynamically scale down large decorative or graphic elements (such as timers, hourglasses, or illustrations) so the entire layout remains stable without forcing undesirable scrolling.
- Fallback scrolling: If screen height is severely constrained (e.g. landscape mode or small devices), ensure the view can scroll gracefully without obscuring the primary action buttons.

## Release Versioning Rules
- When bumping versions for Google Play, increment the minor revision formatted as `08.01`, `08.02`, `08.03`, etc., instead of the major version, and increment `versionCode` by 1.

