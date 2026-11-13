# Luna (Period)

Minimal period / PMS tracker for Android. Her logs the cycle; her partner gets gentle notifications before PMS and period.

- Cycle engine: Kotlin port of the [Bloomery](https://github.com/krugerhomeassistant/Bloomery) model (weighted cycle length, luteal-based ovulation, fertile window) + a PMS window.
- Backend: Supabase (email + password, RLS, pairing by code with approval).
- Updates: the app checks the latest GitHub Release and installs it in-app.

## Setup
1. Supabase → SQL Editor → run `supabase/schema.sql`. Authentication → Providers → Email → disable "Confirm email".
2. Create `supabase.properties` from `supabase.properties.example` and commit it.
3. Release: `git tag v1.0.0 && git push --tags` → GitHub Actions builds the signed APK and publishes the release.
