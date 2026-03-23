# My Wallet - Android

An Android application to help with budgeting built with. Built with Kotlin + Jetpack Compose, targeting Google Play Store.

## Tech Stack

- **UI:** Jetpack Compose + Material Design 3
- **Architecture:** MVVM + Repository
- **GraphQL:** Apollo Kotlin 4
- **Authentication:** Supabase REST API (via OkHttp — no SDK dependency)
- **Dependency Injection:** Hilt
- **Navigation:** Navigation Compose
- **Charts:** Vico
- **Async:** Coroutines + StateFlow

## Features

- **Dashboard** — server health, report summaries, subscription overview, upcoming renewals, net worth preview
- **Reports** — paginated list, create/rename/delete reports
- **Transactions** — add/edit/delete within a report (income & expense categories)
- **Subscriptions** — active/inactive lists, create/edit/cancel/delete, monthly cost calculations
- **Net Worth** — snapshot list, create snapshots with asset/liability entries, detail view
- **Profile** — edit full name, change password, sign out
- **Auth** — Supabase email/password login
- **Theme** — System / Light / Dark selection (persisted across launches)

---

## Development Setup (WSL)

The project builds and runs entirely from the WSL command line.

### First-time setup

After cloning, activate the git hooks (conventional commits enforcement):

```bash
git config core.hooksPath .githooks
```

### Prerequisites

- Java 17 in WSL (`sudo apt install openjdk-17-jdk`)
- Android SDK in WSL (with `ANDROID_HOME` set)
- Android emulator running on Windows, or a physical device connected via USB
- The `my-wallet` backend running locally (`pnpm dev:backend`)

### Step 1 — Configure secrets

`local.properties` is gitignored. A committed template is at `local.properties.example` — copy it and fill in your values:

```properties
# path to your WSL Android SDK
sdk.dir=/home/antonis/android

supabase.url=https://YOUR_PROJECT.supabase.co
supabase.publishable_key=YOUR_SUPABASE_PUBLISHABLE_KEY

# Emulator: 10.0.2.2 reaches Windows host localhost
# Physical device: use your machine's LAN IP instead
graphql.url=http://10.0.2.2:4000/graphql
```

### Step 2 — Build

```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Apollo codegen runs automatically during the build, generating type-safe Kotlin classes from the `.graphql` files.

### Step 3 — Run on emulator or device

Start the Android emulator on Windows (via Android Studio AVD Manager), then from WSL:

```bash
# Verify the emulator is visible
adb devices

# If nothing shows, connect to the Windows emulator from WSL2
adb connect $(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):5554

# Build then install
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a **physical device**: enable USB debugging, then `adb devices` should show it automatically. Update `graphql.url` to your machine's LAN IP (find it with `ipconfig` in PowerShell — IPv4 under the WiFi adapter). If the device can't reach the backend, add an inbound rule in Windows Defender Firewall for TCP port 4000.

---

## Project Structure

```text
app/src/main/
├── graphql/com/mywallet/android/
│   ├── schema.graphqls          # Server schema (used by Apollo codegen)
│   ├── reports.graphql
│   ├── transactions.graphql
│   ├── subscriptions.graphql
│   ├── netWorth.graphql
│   └── user.graphql
│
├── java/com/mywallet/android/
│   ├── MyWalletApplication.kt   # Hilt app entry point
│   ├── MainActivity.kt          # Single activity, bottom nav host
│   │
│   ├── di/
│   │   ├── AppModule.kt         # OkHttpClient, DataStore
│   │   └── NetworkModule.kt     # Apollo client + auth interceptor
│   │
│   ├── data/
│   │   ├── remote/
│   │   │   └── SupabaseAuthService.kt  # Direct Supabase REST auth calls
│   │   └── repository/
│   │       ├── AuthRepository.kt
│   │       ├── ReportRepository.kt
│   │       ├── TransactionRepository.kt
│   │       ├── SubscriptionRepository.kt
│   │       ├── NetWorthRepository.kt
│   │       └── UserRepository.kt
│   │
│   ├── ui/
│   │   ├── theme/               # Material 3 colors, typography, dark mode
│   │   ├── navigation/          # Screen routes, NavGraph
│   │   ├── components/          # Shared composables (LoadingScreen, ConfirmDialog, etc.)
│   │   ├── auth/                # LoginScreen + LoginViewModel
│   │   ├── home/                # HomeScreen (dashboard) + HomeViewModel
│   │   ├── reports/             # ReportsScreen, ReportDetailScreen + ViewModels
│   │   ├── subscriptions/       # SubscriptionsScreen + ViewModel
│   │   ├── networth/            # NetWorthScreen, NetWorthDetailScreen + ViewModels
│   │   └── profile/             # ProfileScreen + ProfileViewModel
│   │
│   └── util/
│       ├── FormatMoney.kt       # Currency formatting
│       └── FormatDate.kt        # Date parsing/formatting, renewal calculation
│
└── res/
    ├── xml/network_security_config.xml  # Allows HTTP to localhost in dev
    └── values/                          # Strings, colors, themes
```

---

## Architecture Notes

### Authentication

Auth is implemented via direct calls to the Supabase REST API using OkHttp (already a transitive dependency of Apollo). No Supabase SDK required.

`SupabaseAuthService` handles sign-in, sign-out, and password changes. After a successful sign-in the access and refresh tokens are persisted to DataStore. On the next launch `tryRestoreSession()` exchanges the stored refresh token for a new access token via Supabase's refresh endpoint — if it succeeds the app starts directly on the Home screen, skipping the Login screen. Signing out clears the stored tokens. The `AuthInterceptor` in `NetworkModule` reads the current in-memory session token and injects `Authorization: Bearer <token>` into every Apollo request.

### GraphQL type naming

The backend uses `type Subscription` for financial subscriptions, which conflicts with GraphQL's reserved subscription root type name. The local `schema.graphqls` renames it to `AppSubscription`. Apollo codegen generates `AppSubscription` Kotlin classes, which correctly deserialize the server's JSON responses (field names are identical).

### State management

Each screen has a ViewModel exposing a single `StateFlow<UiState>` data class. Screens collect it with `collectAsState()`. No global state store — all data flows through repositories.

---

## Launcher Icons

The project ships with a vector wallet icon (`res/drawable/ic_wallet.xml`) as an adaptive icon placeholder. For production:

1. In Android Studio: right-click `res` → **New → Image Asset**
2. Choose **Launcher Icons (Adaptive and Legacy)**
3. This generates all `mipmap-*` density variants

---

## Building for Release

```bash
./gradlew bundleRelease   # .aab for Play Store upload
./gradlew assembleRelease # .apk for direct install
```

You'll need a signing keystore configured in `app/build.gradle.kts` or via Android Studio: **Build → Generate Signed Bundle/APK**.

---

## Environment Variables Reference

All configured in `local.properties` (gitignored):

| Key                        | Description              | Example                        |
|----------------------------|--------------------------|--------------------------------|
| `sdk.dir`                  | Android SDK path in WSL  | `/home/antonis/android`        |
| `supabase.url`             | Supabase project URL     | `https://xxxx.supabase.co`     |
| `supabase.publishable_key` | Supabase publishable key | `sb_publishable_...`           |
| `graphql.url`              | GraphQL backend endpoint | `http://10.0.2.2:4000/graphql` |
