# fix: support standard quoted-printable vCard and stream-based share intents

**Priority:** P1
**Area:** vCard / import / intents

## Problem

Two vCard import paths are broken:

1. Standard vCard uses `ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8`, but the parser only recognizes `CHARSET=QUOTED-PRINTABLE` or `TYPE=...QUOTED-PRINTABLE`. The existing test uses the nonstandard `CHARSET=QUOTED-PRINTABLE`, masking the bug.
2. `ACTION_SEND` share intents often provide the vCard through `EXTRA_STREAM`, but `vCardText` ignores it and returns an empty string.
3. `MainActivity` calls `startActionOf` synchronously on the main thread, so large vCard files can cause ANR.

## Proposed scope

- Decode quoted-printable when `ENCODING` contains `QUOTED-PRINTABLE`.
- Read `EXTRA_STREAM` URIs with IO and proper charset handling.
- Move intent parsing/file reading off the main thread where needed.
- Add tests for standard quoted-printable and stream-based share intents.

## Acceptance criteria

- [ ] `ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8` values are decoded correctly.
- [ ] `ACTION_SEND` with `EXTRA_STREAM` imports the shared vCard.
- [ ] Large shared vCard files do not block the main thread.
- [ ] Existing vCard tests remain green.

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/vcard/VCardParser.kt`
- `data/src/test/java/com/m4isper/contactsapp/data/vcard/VCardParserTest.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/intent/StartAction.kt`
- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt`

## Out of scope

- Supporting every vCard charset/encoding variant.
