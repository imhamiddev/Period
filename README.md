# Luna (Period)

Minimal period / PMS tracker for Android. Her logs the cycle; her partner gets gentle notifications before PMS and period, and whenever she records her mood.

- Cycle engine: Kotlin port of the [Bloomery](https://github.com/krugerhomeassistant/Bloomery) model (weighted cycle length, luteal-based ovulation, fertile window) + a PMS window.
- Backend: Supabase (email + password, RLS, pairing by code with approval).
- Updates: the app checks the latest GitHub Release and installs it in-app.

## Setup
1. Supabase → SQL Editor → run `supabase/schema.sql`, then each `supabase/migrations_00N_*.sql` in order (the newest, `migrations_005_security.sql`, must be applied **before** releasing the app version that uses `request_pair_v2`). Authentication → Providers → Email → disable "Confirm email".
2. Create `supabase.properties` from `supabase.properties.example` and commit it (it holds only the public anon key; Row Level Security protects the data).
3. Signing key (never commit it): in GitHub → Settings → Secrets and variables → Actions add
   - `KEYSTORE_BASE64`: `base64 -w0 luna.jks`
   - `KEYSTORE_PASSWORD` (and `KEY_ALIAS` / `KEY_PASSWORD` if they differ; the alias defaults to `luna` and the key password to the store password)

   Locally, export `KEYSTORE_FILE` (path to the `.jks`) and `KEYSTORE_PASSWORD`. Without them a release build falls back to the debug key.
4. Release: `git tag v1.0.0 && git push --tags` → GitHub Actions runs the unit tests, builds the signed APK and publishes it with a `.sha256` file; the in-app updater verifies the download against it. Tag builds fail on purpose when the signing secrets are missing.

## Security notes
- Tokens, the offline copy and the unsent-changes queue are encrypted on the phone (AES-GCM, Android Keystore). With the app lock on, screenshots and the Recents preview are blocked.
- Pairing codes are rate-limited on the server (8 tries / 10 min) and new accounts get 8-character codes.

## Tests
`gradle :app:testDebugUnitTest`


## Lunari redesign (v2.0.0)

The whole UI follows the *Lunari – Period Tracker App UI Kit* (pink, rounded cards, bottom bar with Home / Calendar / Tracker / Articles / Account), in Persian with a Jalali calendar.

- Home: cycle dial, phase carousel + phase detail pages, edit-period calendar.
- Calendar with pregnancy chance, add log (flow, 19 symptoms, 12 moods, contraception, note, weight, temperature, water).
- Tracker: log counts, my cycles history, weight / BMI / temperature / water charts.
- Articles (Supabase `articles`), account & settings (personal info, units, reminder, security, data export, theme, help…).
- Sign up / sign in / password reset by e-mail code, 7-step setup.
- Subscription and payment screens are **UI only** (no gateway; nothing is charged, only the last 4 digits of a card stay on the phone).

Run `supabase/migrations_006_lunari.sql` **before** installing v2.0.0. Weight, temperature, water, medicines, notes and body data live in `health_logs` / `private_profile`, readable only by their owner.
