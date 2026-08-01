# Line Draw

A minimal one-line puzzle game for Android. Connect every dot with a single unbroken line — no lifting your finger, no reusing a segment.

Built with **Kotlin + Jetpack Compose** (min SDK 26, target SDK 34), MVVM, Navigation-Compose, and DataStore. Monetized with **AdMob** (banner, interstitial, rewarded hints) and a **Google Play Billing** one-time "Remove Ads" purchase ($1.99).

Design: "Vivid Void" — near-black `#0C0E12` background, electric cyan `#33E8FF` line, gold `#FFC24B` stars. Fonts: Sora (display) + Inter (UI), bundled.

## Project structure

```
app/src/main/java/com/linedraw/game/
  ui/screens/      MainMenu, LevelSelect, Gameplay (+ LevelComplete overlay), Settings
  ui/components/   DotGridCanvas, StarRating, AdBanner, LineLoadingIndicator, SuccessBurst
  ui/theme/        Vivid Void colors + Sora/Inter typography
  domain/          PathSolver (DFS solver), LevelValidator
  data/            Level models, LevelRepository (assets/levels.json), ProgressRepository (DataStore)
  ads/             AdManager interface + AdMobAdManager + MockAdManager
  billing/         BillingManager (Play Billing v7, remove_ads product)
  audio/           FeedbackManager (SoundPool chimes + haptics)
tools/LevelGen.java   Level + sound generator (see below)
```

## Build & run

Open in Android Studio (Hedgehog+) or build from the CLI:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

**Debug builds use `MockAdManager`** — interstitials are no-ops and rewarded hints always grant, so the whole game is testable in an emulator with no AdMob setup and no network. Release builds use real AdMob (currently with Google's public *test* ad units).

## Game rules

- Drag from dot to dot; the line only follows valid edges.
- An edge can be used once; dots may be passed through again.
- The level is solved when **every dot** has been visited.
- Dead end? A calm "No moves left" banner appears — Undo or Reset, no fail state.
- Stars: **3** = solved with no undo/reset/hint, **2** = ≤3 undos, **1** = any solve.

## Monetization setup (before publishing)

### 1. AdMob — swap the test IDs for real ones

The app currently uses **Google's public test IDs everywhere**. Replace:

| What | Where | Current (test) value |
|---|---|---|
| App ID | `app/src/main/AndroidManifest.xml` (`com.google.android.gms.ads.APPLICATION_ID` meta-data) | `ca-app-pub-3940256099942544~3347511713` |
| Banner unit | `ads/AdManager.kt` → `AdConfig.BANNER_AD_UNIT_ID` | `.../6300978111` |
| Interstitial unit | `ads/AdManager.kt` → `AdConfig.INTERSTITIAL_AD_UNIT_ID` | `.../1033173712` |
| Rewarded unit | `ads/AdManager.kt` → `AdConfig.REWARDED_AD_UNIT_ID` | `.../5224354917` |

Create the app + 3 ad units at [apps.admob.com](https://apps.admob.com), then paste the real IDs in those two files. Ad placement rules (already implemented):

- **Banner**: Level Select screen + Level Complete overlay only — never during active gameplay.
- **Interstitial**: after every 4th level completion, with a grace period (never within the first 3 completions).
- **Rewarded**: powers the Hint button (highlights the next correct edge).
- All of the above except rewarded hints disappear permanently after the Remove Ads purchase.

### 2. Google Play Billing — the $2 "Remove Ads" purchase

1. In [Play Console](https://play.google.com/console), create the app and go to **Monetize → Products → In-app products**.
2. Create a product with ID **`remove_ads`** (must match `BillingManager.REMOVE_ADS_PRODUCT_ID`).
3. Set the price to **$1.99 USD** (Play Console does not allow exactly $2.00 in most locales; 1.99 is the standard "2 dollar" price point).
4. Activate the product. The in-app price label updates automatically from Play once live.
5. Purchases are acknowledged automatically and restored on every app start; there's also a manual **Restore purchases** button in Settings.

> Billing only works in builds signed and distributed through Play (internal testing track is enough). In local/debug builds the purchase button is enabled but Play will report the product as unavailable.

### 3. Release checklist

- Change `applicationId` in `app/build.gradle.kts` from `com.linedraw.game` to your own unique ID **before first upload** (it can never be changed after).
- Create a signing key and configure a `release` signing config.
- Build with `./gradlew bundleRelease` and upload the `.aab`.
- Fill in the Play *Data safety* form: the AdMob SDK collects the Advertising ID.
- Add a privacy policy URL (required because the app shows ads).

## Levels

Levels live in `app/src/main/assets/levels.json` — 60 levels in 4 difficulty tiers (3×3 → 6×6). Each level stores its dots, valid edges, and one known solution.

To regenerate or extend, edit `tools/LevelGen.java` (tier table in `main`) and run from the repo root:

```
java tools/LevelGen.java
```

Every generated level is built around a guaranteed Hamiltonian path and then re-verified with an independent DFS solver. The unit test suite (`BundledLevelsTest`) re-validates all bundled levels on every test run, so unsolvable levels can never ship.

You can also hand-author levels: add an object `{ id, gridSize, dots, edges, solution }` and run the tests to confirm solvability.
