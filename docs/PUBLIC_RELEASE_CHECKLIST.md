# Public Release Checklist

## Completed In This Pass

- Removed tracked IDE metadata, local Kotlin logs, a debug APK, and a local JDK installer
- Sanitized signing guidance in `gradle.properties`
- Added `LICENSE` and `.java-version`
- Extracted pitch detection, note mapping, smoothing, and tuning feedback into pure Kotlin modules
- Added real unit tests for the extracted tuning core
- Added GitHub Actions CI for unit tests, lint, and debug assembly
- Added emulator-backed instrumentation tests for permission handling and key UI state transitions
- Moved design-source artwork into `docs/assets/design-source/`
- Rewrote the README around architecture, testing, and reproducible setup

## Must Do Before Making The Repo Public

- Push the branch and confirm [`.github/workflows/android.yml`](../.github/workflows/android.yml) passes on GitHub with JDK 17
- Capture clean screenshots or a short demo GIF from a device/emulator and place them under `docs/assets/`
- Verify no historical commits contain real signing secrets or passwords; if they do, rotate them and scrub history before publishing

## Should Do Soon After

- Introduce a `ViewModel` or controller layer so UI rendering is state-driven rather than activity-driven
- Add benchmark-style tests or a small evaluation harness for detection latency and note accuracy
- Add release notes or a changelog for versioned portfolio snapshots
- Add a short architecture diagram or sequence chart to the docs

## Nice To Have

- Add a release build workflow that produces a signed artifact only in GitHub Actions with repository secrets
- Add baseline profile or startup performance work if this becomes a store-distributed app
- Publish a GitHub Pages privacy page from the existing static `index.html`
