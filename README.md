# My Wallet Android

Android companion app for [My Wallet](../my-wallet) — a personal finance tracker. Built with Kotlin + Jetpack Compose, targeting Google Play Store.

## Tech Stack

| Concern | Library |
|---------|---------|
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Repository |
| GraphQL | Apollo Kotlin 4 |
| Authentication | Supabase Kotlin SDK |
| Dependency Injection | Hilt |
| Navigation | Navigation Compose (bottom nav) |
| Charts | Vico |
| Async | Coroutines + StateFlow |

## Features

- **Dashboard** — server health, report summaries, subscription overview, upcoming renewals, net worth preview
- **Reports** — paginated list, create/rename/delete reports
- **Transactions** — add/edit/delete within a report (income & expense categories)
- **Subscriptions** — active/inactive lists, create/edit/cancel/delete, monthly cost calculations
- **Net Worth** — snapshot list, create snapshots with asset/liability entries, detail view
- **Profile** — edit full name, change password, sign out
- **Auth** — Supabase email/password login
- **Theme** — light/dark mode toggle

---

## Development Setup

### Prerequisites

- **Android Studio** (Hedgehog 2023.1.1 or newer) — installed on **Windows** (not inside WSL)
- The `my-wallet` backend running locally (see that project's README)

### Step 1 — Configure secrets

The project's `local.properties` file already has placeholder values. Fill in your actual Supabase credentials:

```properties
supabase.url=https://YOUR_PROJECT_ID.supabase.co
supabase.anon_key=YOUR_SUPABASE_ANON_KEY

# Android emulator reaches your host machine at 10.0.2.2
graphql.url=http://10.0.2.2:4000/graphql
```

> `local.properties` is gitignored and never committed.

### Step 2 — Open the project in Android Studio (from WSL)

Since you're on WSL2, Android Studio runs on **Windows** and accesses your WSL filesystem over the network share.

**In Android Studio on Windows:**

1. Go to **File → Open**
2. In the path bar, type:

   ```
   \\wsl.localhost\Ubuntu\home\antonis\source\my-wallet-android
   ```

   (Replace `Ubuntu` with your WSL distro name if different — check with `wsl -l` in PowerShell)
3. Click **OK** — Android Studio will detect it as a Gradle project and import it

**Add the Android SDK path to `local.properties`:**

Android Studio will prompt you to set the SDK location, or you can add it manually. Find your Windows SDK path (usually `C:\Users\<you>\AppData\Local\Android\Sdk`) and add:

```properties
# In local.properties — use forward slashes or escaped backslashes
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

Android Studio typically writes this automatically on first open.

### Step 3 — Let Apollo generate code

Apollo Kotlin reads the `.graphql` files and generates type-safe Kotlin classes. This happens automatically when Gradle syncs. You'll see the generated code under:

```
app/build/generated/source/apollo/
```

If you see "unresolved reference" errors on `GetReportsQuery`, `CreateReportMutation`, etc. — just **Build → Make Project** to trigger codegen.

### Step 4 — Run the app

1. Start an **Android Virtual Device** (AVD) from the Device Manager (API 26+)
2. Start the `my-wallet` backend on your host machine (`pnpm dev` from the backend package)
3. Press **Run** (▶) in Android Studio

The emulator uses `10.0.2.2` to reach your host `localhost`, so `http://10.0.2.2:4000/graphql` connects to the local backend.

**For a physical device over USB:**

- Enable USB debugging on the device
- Find your host machine's local IP (`ipconfig` on Windows or `ip addr` in WSL)
- Update `graphql.url` in `local.properties`:

  ```properties
  graphql.url=http://192.168.1.XXX:4000/graphql
  ```

---

## Project Structure

```
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
│   │   ├── AppModule.kt         # Supabase client, DataStore
│   │   └── NetworkModule.kt     # Apollo client + auth interceptor
│   │
│   ├── data/repository/
│   │   ├── AuthRepository.kt
│   │   ├── ReportRepository.kt
│   │   ├── TransactionRepository.kt
│   │   ├── SubscriptionRepository.kt
│   │   ├── NetWorthRepository.kt
│   │   └── UserRepository.kt
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

### Authentication flow

1. User signs in via Supabase email/password
2. `AuthRepository` holds the Supabase session
3. `NetworkModule`'s `AuthInterceptor` reads the session token on every Apollo request and injects `Authorization: Bearer <token>`
4. The backend validates the JWT via Supabase admin client

### GraphQL type naming

The backend uses `type Subscription` for financial subscriptions — this conflicts with GraphQL's reserved `subscription` operation root type. The local `schema.graphqls` renames it to `AppSubscription`. Apollo codegen generates `AppSubscription` types, which correctly deserialize the server's JSON response (field names are identical).

### State management

Each screen has a ViewModel exposing a single `StateFlow<UiState>` data class. Screens collect it with `collectAsState()`. No Redux/global state — all data flows through repositories.

---

## Launcher Icons

The project ships with a vector wallet icon (`res/drawable/ic_wallet.xml`) used as an adaptive icon placeholder. For production / Play Store:

1. In Android Studio, right-click `res` → **New → Image Asset**
2. Choose **Launcher Icons (Adaptive and Legacy)**
3. Use your actual app icon
4. This generates all `mipmap-*` density variants

---

## Building a Release APK / AAB

```bash
# From the project root (in Android Studio Terminal or WSL with SDK on PATH)
./gradlew bundleRelease   # produces .aab for Play Store
./gradlew assembleRelease # produces .apk
```

You'll need a signing keystore. In Android Studio: **Build → Generate Signed Bundle/APK**.

---

## Environment Variables Reference

All set in `local.properties` (gitignored):

| Key | Description | Example |
|-----|-------------|---------|
| `sdk.dir` | Android SDK path (Windows) | `C\:\\Users\\you\\AppData\\Local\\Android\\Sdk` |
| `supabase.url` | Your Supabase project URL | `https://xxxx.supabase.co` |
| `supabase.anon_key` | Supabase anon/public key | `eyJhbGci...` |
| `graphql.url` | GraphQL endpoint | `http://10.0.2.2:4000/graphql` |
