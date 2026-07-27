# Auth Manager (Android)

Native Kotlin + Jetpack Compose admin app for managing the same `keys.txt` /
`devices.txt` / `blocked.txt` files in your `authbot` GitHub repo. This app
talks directly to the GitHub Contents API — it does **not** go through the
Telegram bot.

## Token setup — runtime file, never committed to git

The GitHub token is **not** stored anywhere in the source code, so it can
never trip GitHub's push protection and GitHub Actions can build the APK
with zero secrets involved.

Instead, the app reads it at runtime from a hidden file on your phone:

```
/storage/emulated/0/Download/.private_token
```

Create that file (plain text) with one line:

```
TOKEN=github_pat_xxxxxxxxxxxxxxxxxxxxx
```

From Termux:
```bash
nano /storage/emulated/0/Download/.private_token
# paste: TOKEN=github_pat_xxxxxxxxxxxxxxxxxxxxx
# Ctrl+O, Enter, Ctrl+X
```

If this file is missing or malformed, every screen that needs GitHub access
shows a clear "GitHub token not found" banner — the app never crashes for
this, it just can't sync until the file is in place.

The first time the app runs, Android will prompt for storage/file access —
grant it, otherwise the app can't read the Download folder.

## Config that IS in source — fill in before building

Open `app/src/main/java/com/authmanager/app/network/GitHubConfig.kt`:

```kotlin
const val OWNER = "Abyssrte"                       // your GitHub username
const val REPO = "authbot"                          // the repo with keys.txt etc.
const val BRANCH = "main"

const val ADMIN_HASH_PREFIX = "PASTE_10_CHAR_HASH_PREFIX"  // first 10 chars of your device hash
const val LOGIN_PASSWORD = "admin"
```

None of these are secrets — they're safe to commit. `ADMIN_HASH_PREFIX` is
a device hash prefix (not reversible to anything sensitive) and
`LOGIN_PASSWORD` is a local app-lock, not a credential for any external
service.

**Getting your device hash prefix:** run `auth.py`'s `get_android_device_id()`
(or the app itself once — the login screen shows the first 10 characters of
what it *computes* on this device as the "Username" field) and take those
same 10 characters. On every login attempt, the app recomputes this device's
own hash and compares it against `ADMIN_HASH_PREFIX`. If they don't match —
a different phone, or you left the placeholder unfilled — login is refused
with "This device isn't authorized for this app", regardless of the
password. The password field is a second, independent check on top of that.

## Building the APK

This repo builds automatically via GitHub Actions on every push to `main` —
and since the token lives only on your phone, **the build never needs any
secret configured in GitHub**.

1. Push this project to a **new, separate GitHub repo** (not `authbot` — keep
   app source and data separate).
2. GitHub Actions runs `.github/workflows/build-apk.yml` automatically.
3. Go to the **Actions** tab → latest run → download the `auth-manager-debug-apk`
   artifact → unzip → install `app-debug.apk` on your phone.
4. Make sure `/storage/emulated/0/Download/.private_token` exists on that
   phone (see above) before opening the app.

No local Android Studio needed. To trigger a build manually without pushing,
use the "Run workflow" button in the Actions tab (`workflow_dispatch`).

## What it does

- **Login** — username field shows the first 10 characters of this device's
  hash (read-only); password is checked against `LOGIN_PASSWORD`.
- **Key Management** — generate, custom key (any text), change duration
  (`30d` / `5h` / `10m` / `unlimited`), list, delete. Keys are shown in a
  monospace, tap-to-copy field.
- **Device Management**
  - *Registration tab* — register a device to a key, unregister, view the
    registered list.
  - *Blocked tab* — block a device hash, unblock, view the blocked list.
- **Offline fallback** — every screen tries a live GitHub fetch first. If
  that fails (no signal, API down), it falls back to the last successful
  fetch cached on-device, and shows an "Offline — showing cached data"
  banner. The next successful fetch overwrites the cache automatically.

## Notes

- All three files' formats exactly match what `bot.py` / `sync.py` /
  `auth.py` already use — this app is a drop-in alternative writer, not a
  new schema.
- Duration parsing (`d`/`h`/`m`/`unlimited`) and real-time fetching
  (worldtimeapi.org, with device-clock fallback) mirror the same logic
  `bot.py` uses, so expiries computed from either surface stay consistent.
