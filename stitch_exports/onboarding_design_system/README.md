# Stitch export: Onboarding design system

This folder contains the exported artifacts for the Stitch project `Onboarding` (`16218346004387712907`) and its design-system instance `assets-3b70cc54ba9d4901bcaf5e1efd67afc3-1775054917951`.

## Files

- `project-thumbnail.png`: downloaded with `curl -L` from the hosted Stitch thumbnail URL.
- `design-system.tokens.json`: raw token export mapped from Stitch metadata.
- `design-system.css`: implementation-ready CSS variables and component primitives for the smartwatch app.
- `design-system-preview.html`: a local preview page that applies the exported CSS tokens.

## Notes

- Stitch exposed hosted screenshot and HTML URLs for regular screens, but not for this design-system asset instance.
- Because the requested item is a design-system asset rather than a normal screen, the export includes the direct hosted image that Stitch exposed at the project level and a code-form export of the design tokens.
- The visual direction is optimized for a smartwatch sleep-care app: dark layered surfaces, quiet metadata, glassy warning states, and one dominant primary action.
