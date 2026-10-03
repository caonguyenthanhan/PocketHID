# PocketHID Context Handoff

This document preserves the precise technical context for the next AI session to resume work on PocketHID.

## Current Session Scope

### 1. Keyboard Audio Feedback
- **Root cause:** `KeyboardScreen` used direct `ActionDispatcher.execute()`, bypassing `AudioFeedbackManager`.
- **Fix:** Restored centralized audio feedback for discrete keyboard actions. Added audio throttle to avoid rapid-repeat spam.
- **Commit:** `2b169f8`
- **Status:** Unit tests: PASS, assembleDebug: PASS. Physical audio verification: PENDING.

### 2. Android Voice Input / Vietnamese
- **Improvements:** Corrected voice locale configuration and handling for Vietnamese `SpeechRecognizer`.
- **Initial issue:** Recognized Vietnamese Unicode was dropped by KeyMapper.
- **Solution:** Added deterministic reverse-Telex conversion before KeyMapper. Vietnamese Unicode characters are converted into standard ASCII Telex keystroke sequences to be sent over the existing HID keyboard path.
- **Limitation:** VNI is NOT implemented. Vietnamese PC input requires the host OS to have a Vietnamese IME configured for Telex.
- **Commit:** `0cfc494`
- **Status:** Unit tests: PASS, assembleDebug: PASS. Physical Voice → PC validation: PENDING.

### 3. Mouse Latency / Stutter
- **Physical regression observed:** Mouse movement became slower, segmented, "rít", with delays between movement segments.
- **Investigation:** Found Audio/Haptic feedback operations (system calls) were running on the UI thread, potentially blocking the high-frequency trackpad `ACTION_MOVE` / HID report loop.
- **Fix:** Moved `AudioFeedbackManager` and `HapticFeedbackManager` playback strictly to a background coroutine pool (`Dispatchers.IO`), isolating them from the main thread entirely.
- **Important Notes:** No HID protocol redesign or artificial smoothing/delays were introduced. Some tests were updated with short `Thread.sleep()` waits to accommodate the new async feedback calls; this is test-quality technical debt.
- **Commit:** `5455c37`
- **Status:** Unit tests: PASS, assembleDebug: PASS. Physical mouse smoothness: PENDING. *(Note: Do not claim 60/120Hz as measured unless future hardware testing establishes it.)*

### 4. Android Versioning / Install-over
- **Identifiers:** `applicationId` remains `dev.aleian.pockethid`.
- **Updates:** `versionCode` bumped from `1` to `2`. `versionName` bumped from `1.0.0` to `1.0.1`.
- **Commit:** `a555ec4`
- **Status:** Unit tests: PASS, assembleDebug: PASS.
- **Install-over Blocker:** The version bump fixes the Android "equal or lower versionCode" rejection, but does NOT solve the CI APK install-over issue. CI debug builds use ephemeral runner-generated debug signing keys, resulting in signing certificate mismatches (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) for subsequent installs. A stable signing identity is required for seamless upgrades without uninstallation.

## Current Overall Status

**Source Code:**
- Keyboard Audio: implemented
- Haptic Feedback: implemented
- Microphone permission: implemented
- Voice Vietnamese recognition: implemented
- Voice Vietnamese → PC via Telex: implemented
- Mouse feedback threading fix: implemented
- Android version: 1.0.1 / versionCode 2

**Verification:**
- Android unit tests: PASS
- Android assembleDebug: PASS
- Physical keyboard audio: PENDING
- Physical haptic: PENDING
- Physical Vietnamese Voice recognition: PENDING
- Physical Vietnamese Voice → PC: PENDING
- Physical mouse smoothness: PENDING
- APK install-over with existing installation: NOT VERIFIED / signing issue remains

## Known Limitations & Technical Debt
- Vietnamese voice input supports **Telex path only** (VNI is not implemented) and requires a host Vietnamese IME configured for Telex.
- iOS currently has unsigned archive only; an installable IPA requires valid Apple signing/provisioning.
- CI APK signing is not currently stable across ephemeral runners, breaking same-key debug install-overs.
- Audio/Haptic async tests use timing-based waits (`Thread.sleep()`); this is technical debt for future test cleanup.

## Important Product Principles
- **Standard Bluetooth HID / driverless PC interaction remains the core.** Do not introduce PC companion software, custom drivers, clipboard helpers, cloud voice injection, or fake transport logic.
- Keep continuous input paths (like mouse tracking) completely free of feedback-induced thread blocking.
- Physical validation must always be distinguished from source/unit/build evidence. Do not claim features are physically verified without actual device + PC evidence.

## Next Session Focus
The next session should start with:
1. Rebuild a fresh APK from current HEAD after all feature fixes.
2. Install on a real Android device (uninstalling the previous version if necessary due to the signing mismatch).
3. **Physically validate:**
   - keyboard audio
   - haptic
   - English voice
   - Vietnamese voice
   - Vietnamese Voice → PC
   - mouse smoothness/latency
   - version display
4. Only address the stable Android signing/update path if an install-over capability is strictly required for CI debug distributions.
5. Do not declare features physically verified without actual device + PC evidence.
