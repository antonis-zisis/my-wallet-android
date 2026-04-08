# My Wallet Android — Claude Context

## Rules

- **Never create a git commit unless the user explicitly asks for one.**

## Project

Android app (Kotlin + Jetpack Compose) mirroring the web app at `../my-wallet`. Targets Google Play Store.

## Build & Run

```bash
# Build
./gradlew assembleDebug

# Install on connected emulator/device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Check connected devices
adb devices

# Connect to Windows emulator from WSL2
adb connect $(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):5554
```

The backend (`../my-wallet/apps/server`) must be running locally. The emulator reaches it at `http://10.0.2.2:4000/graphql`.

## Architecture

- **MVVM + Repository** — each screen has a `ViewModel` + `UiState` data class; data comes from repository classes
- **Single Activity** — `MainActivity` hosts a `NavHost` with a bottom navigation bar
- **Hilt** for DI — all repositories, services, and the Apollo client are `@Singleton` provided via `AppModule` / `NetworkModule`
- **Apollo Kotlin 4** — GraphQL queries/mutations in `src/main/graphql/`; codegen runs automatically on build
- **Auth** — `SupabaseAuthService` calls the Supabase REST API directly (no SDK); tokens are persisted to DataStore after login and restored via `tryRestoreSession()` (refresh-token exchange) on startup — successful restore skips the Login screen; the `AuthInterceptor` injects the Bearer token into every Apollo request

## Key Conventions

- UI state is a single immutable data class exposed as `StateFlow` from each ViewModel
- All network calls return `Result<T>` and are handled with `.fold(onSuccess, onFailure)`
- Dialogs (create, edit, delete confirmations) are driven by state fields in the ViewModel, not local composable state
- Theme is a three-way `ThemeMode` enum (`SYSTEM`/`LIGHT`/`DARK`) stored in DataStore; `SYSTEM` follows the device setting
- Profile is a top-level bottom nav destination (not navigated to from HomeScreen)
- Date strings from the API are ISO-8601 or epoch-ms; use `formatDate()` / `formatDateShort()` from `util/FormatDate.kt`
- Money formatting goes through `formatMoney()` from `util/FormatMoney.kt`
- Next renewal date is computed client-side via `getNextRenewalDate(startDate, billingCycle): LocalDate?` in `util/FormatDate.kt`

## Subscriptions Domain Notes

- A subscription can be **active and cancelled** at the same time: `isActive=true` + `cancelledAt != null` means the user cancelled but it runs until `endDate`. Always fetch `cancelledAt` in queries.
- `isActive=false` means fully inactive (past end date).
- Resume logic: if `isActive=true` (cancelled but still running), resume immediately with no new start date; if `isActive=false`, show a form asking for a new start date, amount, and billing cycle.
- Badge colours match the web: Monthly → green (`Green100`/`Green600`), Yearly → blue (`Blue100`/`Blue600`), Cancelled → red (`Red100`/`Red600`). Brand colours are defined in `ui/theme/Color.kt`.

## GraphQL Schema Note

The backend has `type Subscription` for financial subscriptions, which conflicts with GraphQL's reserved subscription root type. It is renamed to `AppSubscription` in `schema.graphqls`. Apollo codegen uses this local name; the server JSON is unaffected.

## Local Config

Secrets live in `local.properties` (gitignored). Use `local.properties.example` as the template.

## Tech Stack Versions

See `gradle/libs.versions.toml` for all pinned versions. Key ones:

- Kotlin 2.0.21
- Compose BOM 2025.05.00
- Apollo 4.0.0
- Hilt 2.52
- OkHttp 4.12.0
