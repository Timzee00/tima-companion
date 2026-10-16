# Build the APK without installing Android Studio

This project includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`. GitHub's hosted Linux runner installs Java 17, Android SDK Platform 35, Build Tools 35.0.0 and Gradle 8.9, builds the debug APK and makes it available as a short-lived workflow artifact. The workflow needs no API keys or secrets.

## Steps

1. Create a **public** GitHub repository if you have no billing method. Standard GitHub-hosted runners are free for public repositories. This makes the source code visible to everyone; the companion archive contains no personal API keys, but never commit a pairing token. If you want to keep the source private, build locally with the command-line Android SDK and Gradle instead.
2. Upload the contents of this extracted `TIMA-Companion` folder to the repository root, including the hidden `.github/workflows/build-apk.yml` path. GitHub Desktop is a straightforward way to add folders and hidden files: clone the repository, copy these project files into it, commit, and push.
3. Open the repository's **Actions** tab. If GitHub asks, allow workflows for the repository.
4. Select **Build TIMA Companion APK**, choose **Run workflow**, and run it on your default branch. A push to `main` or `master` also starts a build.
5. Open the finished workflow run and download the `tima-companion-debug-apk` artifact. Unzip that artifact; inside is `app-debug.apk`.
6. Transfer the APK to your phone, install it, and manually enable its Accessibility service in Android Settings.

The artifact is retained for 7 days. Download it to your own device; do not commit the generated APK or any pairing token into the repository. The pairing token must stay private after installation.

This workflow compiles a debug APK; it does not create a Play Store release, sign a production build, or verify behavior on your phone. Test the installed app and bridge on the phone before using any WhatsApp action. The companion session expires after 15 minutes and requires the phone to be unlocked.

GitHub Actions documentation: https://docs.github.com/actions
