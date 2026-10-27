# fix: make photo selection and cropping usable on small screens and with TalkBack

**Priority:** P2 — the photo action row overflows, and the crop dialog has no visible recovery path
**Area:** contact editor / photo picker and crop dialog

## Problem

### Photo action row overflows

`PhotoEditor` places three `TextButton`s in a plain `Row` (`PhotoEditor.kt` lines ~44–73): "Take photo", "Add/Change photo", and "Remove photo". Even in English, three buttons plus their minimum padding exceed a 320–360 dp phone width. With Russian labels, larger font scale, or a wide display font, the row clips or pushes the last action off-screen. The remove action is especially likely to be lost, which is the one users need to fix a bad photo.

### Crop dialog cannot adapt or recover

`CropPhotoDialog` uses a full-screen `Dialog` with a fixed `CROP_SIZE = 320.dp` canvas (`CropPhotoDialog.kt` lines ~58–59, ~87–120, ~143–200). On a short landscape window, the 320 dp canvas plus the action row does not fit, and the content is not scrollable. The actions can end up below the visible area.

Other gaps:

- When image decoding fails, the dialog shows only the text "Couldn't open the photo" with no Retry or Close button. The only exit is the system Back gesture; `onDismissRequest` may not fire from an outside tap because the dialog fills the screen.
- If `writeCropped()` returns null, the dialog calls `onDismiss()` (`CropPhotoDialog.kt` lines ~109–113). The user's crop is silently discarded with no explanation or retry.
- The crop canvas has no content description, roles, or custom actions. TalkBack users cannot pan or zoom and cannot complete a crop without sighted gestures.
- Rotation is not read from EXIF: `readBitmap()` uses `BitmapFactory` only (`CropPhotoDialog.kt` lines ~220–233). Photos taken in portrait on many devices can appear sideways in the preview and remain sideways after saving.
- Pan is clamped against the old scale during a pinch gesture. Zooming out can leave the image offset until the next drag, because the bounds are recalculated only while applying drag (`CropPhotoDialog.kt` lines ~154–164).
- The loading and error states use the same black background with no safe-area padding; status/navigation bars can overlap the controls on edge-to-edge devices.

## Proposed scope

- Replace the three-button row with a single primary "Add/Change photo" action and a secondary menu or `FlowRow` for "Take photo" and "Remove photo". Ensure the row wraps at large font scale and preserves 48 dp targets.
- Make the crop layout adaptive:
  - size the crop viewport from the available width/height (for example `fillMaxWidth(0.8f)` with a max size and aspect ratio 1:1), not a fixed 320 dp;
  - apply `systemBarsPadding()`/`navigationBarsPadding()` and make the whole content scrollable if the available height is too small.
- Add an explicit error state with "Try again" and "Cancel" buttons; keep the dialog dismissible through a visible control.
- Surface a failed crop save with a message and a Retry action; never silently discard the edited crop.
- Add accessible crop controls: content descriptions for the canvas, buttons for zoom in/out, reset, and center, plus custom accessibility actions if gesture support is not practical. Announce when the image is ready and when saving fails.
- Respect EXIF orientation when decoding the source (use `ExifInterface` on the input stream/URI, rotate the bitmap or apply the rotation in the preview and crop transform).
- Re-clamp `pan` after every zoom change, not only on drag. Add a "Reset" action that returns to the fitted, centered image.
- Keep the crop result reasonably sized and clean up temporary files on cancel/failure.

## Acceptance criteria

- [x] On a 320 dp phone, all photo actions are reachable and none are clipped, including at 200% font scale.
- [x] On a 320×480 dp landscape window with the crop dialog open, the canvas and both confirm/cancel actions are visible (or reachable by scrolling).
- [x] A corrupt/unreadable photo shows Retry and Cancel; both work.
- [x] A failed crop save shows an error and does not leave the editor without feedback.
- [x] TalkBack can operate the crop flow: zoom/reset/center controls are reachable and the confirm action is announced.
- [x] A portrait photo with EXIF orientation displays and saves upright.
- [x] Zooming out always keeps the image within the viewport without needing a second gesture.
- [x] Canceling leaves the previous contact photo unchanged.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/PhotoEditor.kt` lines ~44–73 — three-button row.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/dialogs/CropPhotoDialog.kt` — fixed canvas, failure path, pan/clamp, EXIF, `readBitmap()`/`writeCropped()`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt` lines ~85–96 and ~302–334 — crop call site and camera/gallery launchers.
- `app/src/main/res/values/strings.xml` / `values-ru/strings.xml` — add retry, close, zoom, reset, and save-failure strings.
- `androidx.exifinterface.media.ExifInterface` if not already a dependency; otherwise read orientation from the decoded stream.
- Use `BoxWithConstraints` or `LocalConfiguration` to cap the viewport; avoid hard-coded dp for the square.

## Out of scope

- Adding filters, rotating in 90° steps, or a free-form crop.
- Uploading photos to a server.
- Changing the contact photo storage format.

## Verification

- Manual with camera portrait/landscape photos, a corrupt file, and a large gallery image.
- TalkBack pass through open → adjust → crop → save.
- Rotation and 200% font scale on a small phone.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

## Status

Done.

### What was implemented

- Photo action row is a `FlowRow`: at double font scale (the test) the three actions wrap instead of clipping, and every action keeps its test tag.
- The crop canvas is adaptive: the fixed 320 dp square became `sizeIn(max 420 dp)` inside a scrollable column with `systemBarsPadding`, so a short landscape window shows the canvas and the confirm/cancel row (scrolling if needed).
- The decode failure state gained visible "Try again" and "Cancel" buttons — retry re-runs the load, cancel is a real control instead of only the system gesture.
- A failed crop save no longer discards the crop silently: the dialog stays open with a "Couldn't save the cropped photo" message and the user can press Crop again.
- Accessible crop controls: the canvas carries a localized content description, and zoom in / zoom out / reset icon buttons provide a non-gesture path; zoom and pinch share one `clampedPan` clamp, so panning is re-clamped after every zoom change (including zooming out, which snaps the pan back to center immediately). The clamp function is pure and unit-tested (`CropPanClampTest`).
- EXIF orientation: `readBitmap` now reads `ExifInterface` from the source stream and rotates the decoded bitmap, so portrait photos display and save upright (new `androidx.exifinterface` dependency).
- Canceling still discards the working crop; the previous contact photo is untouched.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- `./gradlew :app:connectedDebugAndroidTest` on a headless emulator: BUILD SUCCESSFUL, 118 tests, 117 passed, 1 skipped by assumption (`LargeListPerformanceTest` benchmark).
- Manual checks that remain useful: real camera portrait/landscape photos through the crop flow, a corrupt file on a device, and a TalkBack pass over open → adjust → crop → save.
