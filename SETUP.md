# TIMA 13.3 — setup, changes and verified limits

Checked against official documentation on 13 October 2026. Provider policies and account quotas can change. Documentation checks are not proof that a particular key works.

## What changed

- Battery is measured using Termux:API, cached for at most 30 seconds, and shown as unavailable when no valid reading exists. No default 100% reading.
- Contacts use exact or unique name-token matches. Ambiguous contacts require a full name/number. If phone contacts do not resolve the name, the WhatsApp UI adapter searches WhatsApp itself, when the companion is available.
- WhatsApp automation checks observed app/package, recipient header, existing draft, composer and Send button. It reports “submitted” only after the outgoing text appears in the expected chat. Delivery/read status is not verified. An uncertain Send attempt is never automatically retried.
- Requests such as “tell Merit that…” instruct the intent model to compose a direct, polished message without inventing facts. Exact quoted text should be preserved. This is model-dependent, not a guarantee of perfect interpretation.
- Owner Telegram voice notes use Groq Whisper transcription. Low-confidence, stale and failed transcriptions cannot issue a phone command. Other Telegram users can receive conversational voice responses but cannot control the phone.
- One-time WhatsApp jobs persist in SQLite. They wait up to 15 minutes after the due time when locked, busy or disconnected, then expire with a notification/history entry. Interrupted running jobs become “unknown” rather than being resent. This is best-effort scheduling, not an exact alarm service.
- Current-information questions use retrieved source URLs and a check time. If there are no usable live sources, TIMA reports that it cannot verify the answer. Search excerpts are not full-page verification. News responses include publication dates/URLs where provided.
- Groq vision now defaults to the currently documented Qwen3.8 model. Gemini chat/vision prefer Gemini 2.5 Flash. Removed the old Mistral 7B fallback. Existing supplied credentials are preserved.
- Gemini paid image editing is disabled by default for this free-key setup. QR/logo tools remain local. Stability/Hugging Face generation can consume trial or included credits; their remaining balance is unknown.

## Install the Python update

Stop the old TIMA with Ctrl+C. Keep the existing database and workspace.

```bash
cp ~/storage/downloads/tima_13_3_0.py ~/downloads/
python3 -m pip install requests beautifulsoup4 pillow qrcode tzdata
python3 ~/downloads/tima_13_3_0.py --version
python3 ~/downloads/tima_13_3_0.py --all
```

For scan-checked logo QR codes, install `zxing-cpp` separately. If it fails to build in Termux, plain QR generation remains available but logo QR generation refuses to produce an unchecked result.

For phone contacts and battery, install the Termux:API Android companion from the same compatible distribution as Termux, run `pkg install termux-api`, and grant Contacts permission. Installing the command-line package alone does not install the Android app.

## Important: the Accessibility companion is still SOURCE, not an APK

The accompanying TIMA-Companion.zip must be built with Android Studio/JDK/Android SDK and installed before UI sending can work. It was syntax-parsed here but was not compiled or tested on a physical phone. The updated bridge adds resource IDs, clickable ancestor IDs and an unlocked-session status check. WhatsApp resource IDs can change; mismatches stop the task instead of guessing.

After building/installing it: enable Accessibility manually, copy its token into `TIMA_BRIDGE_TOKEN` in your local environment or private key file, and start its 15-minute session. The session expires, stops on process death, and does not bypass the lock screen. Do not schedule beyond the session and expect it to remain active unattended.

## Commands to test on your phone

```text
/contacts-check
/device-check
/screen
/contact Merit | +2349012345678
/whatsapp-at tomorrow at 12pm | Merit | Happy birthday, Merit! Wishing you a wonderful year ahead.
/whatsapp-jobs
/whatsapp-cancel 1
/model-check
/free-status
```

The example phone number is a placeholder: use the real recipient's number. The explicit scheduling command uses the exact supplied message. Natural-language requests require a working text model. /model-check queries model catalogs; catalog visibility does not prove inference quota or free billing. /service-check probes configured services and may consume small amounts of quota.

Test immediate sending with an intended harmless message before relying on scheduled sending. Test duplicate names, existing drafts, locked screen, stopped companion, loss of connection after Send, and public Telegram attempts. Inspect the real WhatsApp conversation.

## Locked phone and background work

Termux must still be running for its Python scheduler to execute. Android Doze can defer CPU/network work and ignore ordinary wake locks. Battery optimization exemptions and `termux-wake-lock` can help some situations but do not guarantee execution. Force-stop, reboot, missing connectivity and an expired companion session can prevent work. This release does not include boot restart or an Android exact-alarm/foreground-worker implementation.

Personal WhatsApp UI automation here cannot send while the keyguard is locked. The job waits, then expires. A server cannot click a locked phone either. A server-side messaging API would be a separate integration with its own account, template and pricing requirements; it is not implemented here.

## Current free-tier facts and sources

| Service | What the official documentation says |
|---|---|
| Groq | GPT-OSS 20B/120B, Qwen3.8 vision and Whisper appear in the free-plan limits. Quotas apply: https://console.groq.com/docs/rate-limits |
| Gemini | Gemini 2.5 Flash supports free text/image-input usage. Gemini 3.1 Flash Image output/editing has no free API tier: https://ai.google.dev/gemini-api/docs/pricing |
| OpenRouter | `openrouter/free` routes to free models. Free-account limits apply: https://openrouter.ai/openrouter/free and https://openrouter.ai/pricing |
| Mistral | Free mode includes limited usage; account Limits page is authoritative: https://docs.mistral.ai/admin/billing-usage/usage-limits |
| Cohere | Evaluation keys are free but limited to 1,000 calls/month: https://docs.cohere.com/docs/rate-limits |
| Hugging Face | Free users receive $0.10 monthly inference credits, subject to change. This is not unlimited image generation: https://huggingface.co/docs/inference-providers/pricing |
| Stability | Credit-based API with introductory credits; no promise of a continuing free allowance: https://platform.stability.ai/pricing |
| Serper | Advertises 2,500 introductory queries, followed by credit purchases: https://serper.dev/ |
| NewsAPI | Free Developer plan: development/testing, 100 requests/day and a 24-hour article delay: https://newsapi.org/pricing |
| OpenWeather | Current weather is part of its Free plan; One Call is a distinct subscription: https://openweathermap.org/price |

Android background restrictions: https://developer.android.com/training/monitoring-device-state/doze-standby

Android lock state: https://developer.android.com/reference/android/app/KeyguardManager

Termux:API compatibility: https://github.com/termux/termux-api

Groq voice format/limits: https://console.groq.com/docs/speech-to-text

Wolfram and Abstract account entitlements were not independently established. Abstract still needs its specific product identified. No claim is made that every supplied service key has current quota.

## Verification completed here

Python syntax, measured/invalid battery responses, contact ambiguity, recipient/draft guards, simulated Send readback, locked/unknown outcomes, real SQLite job persistence/expiry/recovery, owner/public voice routing, stale/unclear voice rejection, missing-live-source behavior, conversation isolation, QR decoding and generated-artifact handling passed targeted tests. Credential-preservation checks passed. Android Java source syntax parsed.

Groq and Gemini live catalog attempts failed because this workspace could not reach the services. No live WhatsApp message, physical gesture, overnight locked-device task, paid image edit or real assignment-reading accuracy test was performed. The companion APK has not been built. These are remaining acceptance tests, not completed features verified end to end.
