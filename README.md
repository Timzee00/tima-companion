# TIMA Companion 0.2 preview — source only

This archive is Android source, not an installable APK. It was syntax-parsed but not built or tested on a phone here. It accompanies TIMA 13.3.

Build with JDK 17, Gradle 8.9, Android SDK Platform 35 and Android Gradle Plugin 8.7.3. For a cloud build without Android Studio, use the included GitHub Actions workflow and follow GITHUB-BUILD.md. For a local build, open the project in Android Studio, configure Gradle, then build app:assembleDebug. No Gradle wrapper is bundled. Install the generated debug APK on your own Android 8+ phone.

Enable its Accessibility service manually, copy the pairing token into TIMA_BRIDGE_TOKEN in your private Termux environment/key file, and start the 15-minute session. The token is required on the loopback-only port 8765. Do not expose that port. Sessions stop on expiry, STOP, interruption or process death. The lock screen is not bypassed.

Version 0.2 adds observed resource IDs, enabled flags, clickable ancestor IDs and an authenticated status check that works before an accessible app window is open. Python uses these for guarded WhatsApp UI interactions. Resource IDs vary with app updates; errors stop sending rather than guessing.

The bridge supports observe, click, type, scroll, gesture, back and home. Password fields and protected system/permission screens are excluded. Unlabeled or canvas-based apps may be inaccessible. An accepted action does not independently prove the user's goal was achieved.

The included setup guide covers provider limits, battery/contacts, voice, scheduling and phone tests. No claim of unattended locked-phone sending is made.

Tests in tests/ use mocked Android/provider responses. Copy tima.py beside them and run with Python; they are not Android instrumentation tests. No API credentials are included in this companion source archive.
