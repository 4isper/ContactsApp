# feat: show the contact QR code on screen instead of only exporting it as an image

**Priority:** P2 — sharing works, but the fastest face-to-face path (hold your screen up to another camera) is missing
**Area:** contact details / share

## Problem

The details screen offers "Share QR code" (`DetailsTopBar.kt`, `share_as_qr_menu_item`). The flow renders a 512 px PNG into `cacheDir/shared/` and hands it to the system share sheet, so face-to-face sharing — the most common scenario for a contact QR — requires the sender to pick a target app (Gallery, Photos, a messenger), open the image there, and only then let the other phone scan it. Three hops for what should be zero: the sender shows the code on their own screen and the receiver points a camera at it.

The app already has everything needed to display the code: `qrCodeBitmap(content, sizePx)` in `QrCode.kt` is a pure, testable encoder (zxing `QRCodeWriter` → `BitMatrix` → `Bitmap`). But its only caller is the export path (`ContactShare.writeQrFile`), so nothing in the app ever renders the QR it can generate. The strings only cover the share path as well.

## Proposed scope

- Add a "Show QR" menu item to the details overflow (`DetailsTopBar`) above the existing "Share QR code" item. The export item stays — sending the PNG remotely is still a valid path.
- New dialog composable (e.g. `presentation/details/QrPreviewDialog.kt`): a white rounded card with the QR (black on white, quiet zone preserved), the contact name as a caption, and a hint line telling the user the other phone can scan it with a normal camera. Buttons: "Share image" (reusing the existing `shareContactAsQr` handler) and "Close".
- Generate the bitmap off the main thread (`Dispatchers.Default`) — follow the issue 19 rule of no work in composition. For a 512 px encode this is usually instant; still show a small placeholder state while the matrix computes so a large vCard cannot stall the dialog.
- Null/blank vCard → the same share-failure toast the export path already shows (`shareDetailsAsQr` pattern); no new error handling, no crash.
- Dark theme: the QR area itself stays black-on-white regardless of theme so the camera reads it; the rest of the dialog surface follows the theme.
- Accessibility: the QR image is decorative (no misleading contentDescription — the name caption carries the meaning), the hint is readable by TalkBack, focus lands inside the dialog on open, test tags follow the existing naming (`show_qr_menu_item`, `qr_preview_dialog`, …).
- Localize all new strings in both locales (en, ru).

## Acceptance criteria

- [x] The details overflow contains a "Show QR" item; the existing "Share QR code" item is unchanged.
- [x] Choosing it opens a dialog with the QR rendered on screen; generating it does not block the main thread (no jank/ANR on large vCards).
- [x] The code is black-on-white with a visible quiet zone and stays scannable in dark theme.
- [x] The dialog shows the contact name and a scanning hint, and offers "Share image" (same behavior as the existing share item — one implementation) and "Close".
- [x] A null/blank vCard shows the share-failure toast instead of opening a broken dialog.
- [x] TalkBack reads the dialog correctly; new strings are localized in en and ru; test tags are added.
- [x] Unit/UI tests cover the menu → dialog → share/close paths and the placeholder state; `./gradlew :app:testDebugUnitTest` passes.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/common/share/QrCode.kt` — `qrCodeBitmap` already returns a ready `Bitmap?`; render via `Image(bitmap.asImageBitmap(), …)`. No new zxing API needed — the encode-only `zxing.core` dependency stays.
- zxing's `QRCodeWriter` applies the default 4-module quiet zone (`EncodeHintType.MARGIN`); the white card padding must not be thinner than that. Do not set hints that shrink the margin.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/share/ContactShare.kt:105` — `shareContactAsQr` remains the single export implementation; the dialog's share button calls the same handler instead of re-writing the PNG pipeline.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/DetailsTopBar.kt:130-135` — menu items and test tags; follow the `share_as_qr_menu_item` naming.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsScreen.kt:113-133` — the vCard is already fetched through `viewModel.vCardText()` for the share path; keep the dialog visibility state where the other presentational state lives (issue 14/18/19 conventions), not in ad-hoc composition state.
- Dialog conventions to match: `ui/components/ConfirmDialog.kt` and `presentation/editor/dialogs/CropPhotoDialog.kt` (surface, buttons, test-tag patterns).
- Strings: `app/src/main/res/values/strings.xml` + `values-ru/strings.xml` (e.g. `show_qr`, `qr_scan_hint`).
- The matrix encoding is already unit-tested without Android in `ContactTextTest.kt` (`qrMatrix` cases); the bitmap conversion needs no new test unless the placeholder state gets its own logic.

## Out of scope

- Scanning a QR to import a contact: needs a camera pipeline and a decoder (zxing decoding or ML Kit) — a separate feature issue if wanted.
- Changing the exported PNG size/format or the `shared/` FileProvider scoping (security fix already landed).
- Restructuring the share sheet usage — issue 25 owns stream-based share intents.

## Verification

- Manual: light and dark themes; scan the on-screen code with another phone from arm's length; TalkBack pass over the dialog; airplane-mode share of the PNG still works.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.

## Status

Done.

### What was implemented

- `DetailsTopBar` gained `onShowQr` and a `show_qr_menu_item` entry directly above `share_as_qr_menu_item`; the export item and the top-bar share button are unchanged.
- New `presentation/details/QrPreviewDialog.kt`: an M3 `AlertDialog` with the contact name as the title, the QR on a white rounded card, the `qr_scan_hint` line, and `qr_preview_share` / `qr_preview_close` buttons. The QR `Image` has `contentDescription = null` (decorative) and sits inside a `Color.White` card, so the encoder's 4-module quiet zone and the light-on-dark theme cannot hurt scanning.
- Encoding runs in a `LaunchedEffect` on `Dispatchers.Default` through an injectable `encode` parameter (default = `qrCodeBitmap`); the dialog shows a `qr_preview_placeholder` spinner until the bitmap arrives and hides "Share image" while there is nothing to export. A null result (data too large for a QR) shows the existing `share_failed` text instead of a broken code.
- `ContactDetailsScreen` holds `qrPreviewVCard` next to the other dialog state. `shareOrFail` was generalized to take the content and hand it to the action, so the blank-vCard toast and the "open the dialog only with real content" rule are one implementation shared by contact share, text share, QR export, and the preview. The dialog's share button calls the same `shareContactAsQr` export path.
- Strings `show_qr`, `qr_scan_hint`, `qr_preview_share` added to `values` and `values-ru`; `action_close` reused.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- `QrPreviewDialogTest` (instrumented) covers the placeholder→image transition, name + hint, share and close callbacks, and the failed-encode state without a share button.
- `ContactDetailsUiTest` covers menu → dialog → close and the blank-vCard path that must not open the dialog.
- Instrumented tests were compiled but not executed here (no emulator attached); `connectedDebugAndroidTest` and the manual light/dark + TalkBack pass remain to be run on a device.
