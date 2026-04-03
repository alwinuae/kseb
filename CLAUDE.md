# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KSEB Bill Calculator — an Android app for calculating Kerala State Electricity Board domestic electricity bills (LT-1A tariff, effective 05.12.2024 to 31.03.2027). Fully offline, no network calls, no permissions needed.

**Tech stack**: Kotlin, Jetpack Compose, Material 3, Kotlin Serialization, DataStore, Android PdfDocument API.
**Min SDK**: 24 (Android 7.0). **Target APK size**: Under 10MB.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew test                   # Run all unit tests
./gradlew testDebugUnitTest      # Run debug unit tests only
./gradlew test --tests "com.kseb.billcalculator.calculation.BillCalculatorTest"  # Single test class
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew lint                   # Run Android lint
./gradlew ktlintCheck            # Kotlin lint (if configured)
```

## Architecture

### Calculation Layer (pure Kotlin, no Android dependencies)
The core logic lives in `calculation/` and is unit-testable on JVM without an emulator:
- **BillCalculator** — Forward: units + phase + cycle + tariff → BillBreakdown
- **ReverseCalculator** — Reverse: bill amount → estimated units. Most complex component due to: (1) circular dependency between fixed charges and consumption level, (2) telescopic/non-telescopic discontinuity at 250/251 units, (3) inter-band jumps at 300/301, 350/351, 400/401, 500/501. Uses candidate enumeration across all fixed-charge/slab combinations.
- **ApplianceCalculator** — Appliance list → monthly kWh per item + total units

All calculators receive `TariffConfig` as a parameter (never hardcoded rates).

### Tariff Rules (critical for correctness)
- **Telescopic** (0-250 units monthly): each slab charged independently at its own rate
- **Non-telescopic** (251+ units): single flat rate applied to ALL consumed units (not just excess)
- **Bimonthly**: `floor(totalUnits / 2)` → calculate monthly → multiply all charges by 2. NOT the same as doubling slab ranges.
- **Fixed charges** vary by consumption level AND phase type (creates circular dependency in reverse calc)
- All monetary values: `BigDecimal.setScale(2, RoundingMode.HALF_UP)`
- Indian number formatting: `₹1,23,456.00`

### Data Flow
```
User Input → ViewModel (StateFlow) → Calculator (pure Kotlin) → BillBreakdown → UI
                                                                ↘ PdfGenerator → PDF → Share Intent
```

### 4 Screens + Settings
1. **Units to Bill** (Form 1) — real-time calculation (300ms debounce)
2. **Appliance Calculator** (Form 2) — calculate on button press, dynamic appliance rows
3. **Reverse Calculator** (Form 3) — calculate on button press, shows verification + gap handling
4. **Phase Comparison** (Form 4) — side-by-side single vs three phase
5. **Settings** — edit all tariff rates, reset to defaults, "custom rates active" indicator

Cross-form navigation: Form 2 → Form 4 (compare phases), Form 3 → Form 1 (view in calculator).

### PDF Generation
Uses Android's built-in `PdfDocument` API (no third-party library). Custom utilities: `PdfPageManager` (auto page-break, Y cursor), `PdfTableBuilder`, `PdfTextBlock`. Shares via `FileProvider` + `Intent.ACTION_SEND`. Bundled Noto Sans font for consistent rendering.

### State Management
ViewModel + SavedStateHandle per screen. Tariff config persisted via DataStore as JSON. Navigation preserves tab state with `saveState = true`.

## Key Implementation Notes

- Package: `com.kseb.billcalculator`
- No KSEB branding/logo (trademark risk). Use app's own icon. Always include "ESTIMATE ONLY" disclaimer.
- The 250→251 unit boundary is the most sensitive edge case: telescopic sum at 250 = ₹1,432.50 energy vs non-telescopic at 251 = 251×6.75 = ₹1,694.25. The reverse calculator must handle amounts falling in this gap.
- Reverse calculator round-trip invariant: `reverse(forward(units)) == units` for all units 0-600+.
- Test cases (249 scenarios) are in `userscript.md`. Implementation plan is in `plan.md`.

## Project Status

Planning phase complete (plan + test cases). No code written yet. Implementation follows the 10-phase order defined in `plan.md` Section 11.
