# Brainrush

Minimal dopamine manager for Android that visualizes your screen time. Built with Kotlin, Jetpack Compose, Material 3, Room, and ComposeCharts.

## Overview
- Tracks daily total screen time and the top apps you used today.
- Tap any app to see its last 7 days of usage as a line chart.
- The app auto-scrolls to the chart and keeps it at the top for quick focus.
- Works on Android 7.0+ (minSdk 24). Requires Usage Access permission.

## Features
- Today’s total screen time (in minutes).
- Scrollable Top Apps list (ranked by today’s usage).
- Per‑app 7‑day history using `ComposeCharts` (line chart).
- Auto-scroll to the chart after selecting an app.
- Refresh button to pull latest usage stats.
- Direct link to open Usage Access settings when permission is missing.

## Tech Stack
- Kotlin, Coroutines, Flow
- Jetpack Compose + Material 3
- Room (with KSP)
- ComposeCharts (`ir.ehsannarmani:compose-charts:0.2.0`)

## Project Structure
```
app/
  src/main/java/com/ga/brainrush/
    MainActivity.kt
    ui/home/HomeScreen.kt                // Home UI (cards, top apps, 7‑day chart)
    ui/components/ScreenTimeCard.kt      // Example UI component
    ui/theme/Theme.kt                    // Light theme
    data/util/UsageStatsHelper.kt        // Usage access helpers + per‑app 7‑day data
    data/db/*                            // Room database, DAO, entities
    domain/repository/ScreenTimeRepository.kt
```

## Getting Started
### Prerequisites
- Android Studio (latest stable)
- Android SDK 24–35
- JDK 17
- A device/emulator with Google Play Services recommended

### Build & Run (CLI)
```bash
# Build
./gradlew assembleDebug

# Install to the connected device/emulator
./gradlew installDebug

# Launch
adb shell am start -n com.ga.brainrush/.MainActivity
```

### Grant Usage Access
The app needs Usage Access to read app usage stats (minutes in foreground).
- Open device Settings > Security & privacy > More security > Usage access.
- Find “Brainrush” and allow access.
- OEM menus vary; inside the app you can tap “Open Usage Access Settings” to jump to the right screen.

## How It Works
- Today’s usage is collected using `UsageStatsManager` and summarized per package.
- Tapping a package in the Top Apps list loads `getUsageLastNDays(context, pkg, 7)` and shows a 7‑day line chart at the top.
- Missing days are filled with `0`, and the 7‑day window is ordered chronologically.

Key pieces:
- `UsageStatsHelper.getTodayUsage(context)`: Returns today’s per‑app minutes.
- `UsageStatsHelper.getUsageLastNDays(context, packageName, days)`: Returns a `List<Double>` of minutes per day over the last N days, zero‑filled and ordered.
- `HomeScreen.kt`: Renders cards, Top Apps (scrollable), handles selection and auto‑scrolls the list to focus the chart at the top.

## UI Notes
- The Top Apps list is fully scrollable (no fixed 5‑item cap).
- Selecting an app updates the chart and animates scroll to the top item (the chart card).
- Some social apps have friendly labels/colors based on package name detection (e.g., TikTok, Instagram, YouTube, Facebook, Twitter/X).

## Development Notes
- Compose Compiler Extension: `1.9.20`
- Min SDK: `24`, Target SDK: `35`
- Module coordinates:
  - Application ID: `com.ga.brainrush`
  - ComposeCharts artifact: `io.github.ehsannarmani:compose-charts:0.2.0`
  - Import package used: `ir.ehsannarmani.compose_charts.*`
- Deprecation: `LinearProgressIndicator(progress: Float, ...)` is used; consider switching to the lambda overload when convenient.

## Troubleshooting
- Empty/zero data:
  - Ensure Usage Access is granted for Brainrush.
  - Some OEMs delay or restrict usage stat updates.
- “Unresolved reference: LineChart”:
  - Ensure `mavenCentral()` is enabled and dependency is present.
  - Correct imports: `ir.ehsannarmani.compose_charts.LineChart` and `ir.ehsannarmani.compose_charts.models.Line`.
- Build issues with KSP/Room:
  - Ensure KSP matches the Kotlin/AGP versions in `build.gradle.kts`.

## Roadmap
- Sticky header for “Top Apps Hari Ini”.
- Default chart for the top app on first open.
- “Clear selection” action to hide the chart.
- More detailed stats screen (weekly/monthly trends, goals, streaks).
- Notifications or widgets for daily insights.

## Contributing
Pull requests are welcome! Please:
- Keep changes focused and consistent with existing style.
- Include clear descriptions and testing steps.

## License
No license specified yet. If you plan to use or distribute this project, please add an appropriate license.