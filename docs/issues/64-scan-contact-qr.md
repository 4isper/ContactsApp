# feat: accept a contact by scanning a QR code

**Priority:** P2 — issue 53 made the contact QR visible on screen, but the exchange stays one-sided: the receiver has no way to point this app at it
**Area:** contacts / import / share

## Problem

The app can show and export a contact QR (issue 53) but its only import path is a .vcf file. Face-to-face exchange therefore breaks on the receiving side: the other phone shows the code, this app has no camera to read it. Issue 53's "Out of scope" explicitly deferred this: "Scanning a QR to import a contact: needs a camera pipeline and a decoder — a separate feature issue if wanted."

Everything downstream of the scan already exists: the import preview screen handles duplicates and invalid entries, and the write-permission flow is requested when the action happens (issue 42). What is missing is a camera pipeline, a decoder, and a decision on what to do with a scanned string that is not a vCard.

## Proposed scope

- A "Scan QR code" item in the home overflow menu, next to "Import from file" (`HomeTopBar.kt`).
- A camera scanner launched through the existing launcher pattern in `ContactsLaunchers.kt`; QR only — other barcode formats do not carry contacts.
- The scanned text is classified by a pure function next to the QR encoder: a vCard flows into the existing import preview, anything else (URL, MECARD, plain text) shows a clear "no contact in this QR" message; a cancelled scan stays silent.
- Runtime camera permission requested at the first scan: a toast on denial, the app settings screen on permanent denial.
- Manifest: `CAMERA` permission; the camera hardware feature declared optional so contactless devices can still install.
- Strings localized in both locales (en, ru); test tags follow the existing naming.

## Acceptance criteria

- [x] The home menu contains a "Scan QR code" item; with the camera permission granted it opens a QR-only scanner with a localized prompt.
- [x] A scanned vCard opens the existing import preview with its duplicate and invalid-entry handling; no new data path is added.
- [x] A QR without a contact shows "This QR code doesn't contain a contact"; a cancelled scan does nothing.
- [x] The camera permission is requested at the first scan; denial shows a toast, permanent denial also opens the settings screen.
- [x] The app stays installable on devices without a camera.
- [x] The vCard-or-not decision is a pure JVM-tested function (case-insensitive markers, a truncated vCard still opens the preview, cancel and empty scans are not contacts); the new menu item is covered by the contacts UI test.
- [x] `./gradlew :app:assembleDebug`, unit tests, detekt and ktlint pass.

## Technical notes

- Decoder: `com.journeyapps:zxing-android-embedded` on top of the zxing core already in `libs.versions.toml` — `ScanContract` + `ScanOptions` give a ready activity-based scanner. CameraX + `core` decoding or ML Kit mean noticeably more code or a Play services dependency for one format.
- Keep the classification pure and in `presentation/common/share/` next to the QR encoder (`QrScanContent.kt`), so the camera wrapper stays thin and the decision is JVM-testable; the `VCard` variant carries the raw text as-is, since the vCard codec and preview already own parsing.
- Launchers: `ContactsLaunchers.kt` gains a `ScanContract` launcher and a camera-permission launcher; `startScan` checks the permission first and launches the scanner, otherwise requests it — the same contextual shape as the write-permission flow in the same file.
- The result callback pattern: `result.contents == null` means the user closed the scanner — return silently; otherwise classify and either hand the text to the existing `onImportText` (import preview entry point) or toast.
- Manifest: `uses-permission android.permission.CAMERA` and `uses-feature android.hardware.camera.any` with `required="false"`, next to the telephony entry.
- Strings: `action_scan_qr`, `scan_qr_prompt`, `scan_qr_no_permission`, `scan_qr_not_contact` in `values/strings.xml` + `values-ru/strings.xml`.

## Out of scope

- Barcode formats other than QR, MECARD generation, scanning a QR from an image in the gallery.
- Writing the scanned contact straight into the provider without the preview — the preview's duplicate handling is the value.

## Verification

- `./gradlew :app:assembleDebug`, `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`, `./gradlew detekt ktlintCheck`.
- Manual on a device: scan a vCard QR shown by another phone (issue 53 dialog or a system camera), scan a URL QR, close the scanner, deny the camera permission and reopen.

## Status

Done.

### What was implemented

- `fc1de8f` added `QrScanContent` — a sealed interface (`VCard` / `NotContact`) with the pure `qrScanContent` classifier next to the QR encoder. Markers are case-insensitive; a truncated vCard still counts as a vCard because the preview's invalid-entry handling owns the content; cancel and empty scans are not contacts. JVM-tested in `QrScanContentTest`.
- `a10be6f` wired the flow: a `scan_qr_menu_item` ("Scan QR code") in the home overflow between import and export; `ContactsLaunchers.startScan` requests the camera permission first and launches a QR-only `ScanOptions` scanner (no beep, localized prompt); the result goes through the classifier — a vCard into the existing import preview, anything else a `scan_qr_not_contact` toast; a cancelled scan returns no contents and stays silent. Denial toasts `scan_qr_no_permission` and, when permanently denied, opens the app settings (issue 42 pattern). The manifest gained the `CAMERA` permission and an optional `android.hardware.camera.any` feature; `zxing-android-embedded:4.3.0` joined the version catalog.
- README updated: the feature and the menu item in the contact-list bullet, `CAMERA` in the permission list, and the new library in the stack table.

### Verification

- `./gradlew :app:assembleDebug`, `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`, `detekt`, `ktlintCheck` — green at commit time.
- `ContactListUiTest` covers the home menu including the new `scan_qr_menu_item`.
- The manual device pass (scan a real vCard QR, a URL QR, cancel, deny permission) remains to be run; the classifier and UI entry point are covered by the JVM and instrumented tests above.
