# KSEB Bill Calculator - Android App - Implementation Plan

## Context
Build an Android app for Kerala (KSEB) electricity bill calculation. The app has 4 main forms plus a settings screen. All tariff rates are configurable. Supports both monthly and bimonthly billing cycles. All forms export to PDF.

**Tech Stack**: Kotlin + Jetpack Compose + Material 3
**PDF**: Android's built-in `PdfDocument` API (no third-party library)
**Min SDK**: 24 (Android 7.0)
**Target APK Size**: Under 10MB

---

## Table of Contents
1. [App Screens Overview](#1-app-screens-overview)
2. [KSEB Tariff Structure](#2-kseb-tariff-structure)
3. [Project Structure](#3-project-structure)
4. [Core Calculation Logic](#4-core-calculation-logic)
5. [Reverse Calculation Logic](#5-reverse-calculation-logic)
6. [Appliance Calculation Logic](#6-appliance-calculation-logic)
7. [UI/UX Design](#7-uiux-design)
8. [PDF Generation](#8-pdf-generation)
9. [Data Architecture](#9-data-architecture)
10. [Edge Cases & Validation](#10-edge-cases--validation)
11. [Implementation Order](#11-implementation-order)
12. [Verification Plan](#12-verification-plan)

---

## 1. App Screens Overview

### Form 1: Units to Bill Calculator
- User enters units consumed, selects phase (single/three), billing cycle (monthly/bimonthly)
- Shows complete bill breakdown: slab-wise energy charges, fixed charge, electricity duty, fuel surcharge, meter rent, GST
- Shows total bill amount with full splitup
- Export to PDF

### Form 2: Appliance Usage Calculator
- Dropdown to select appliances with preset wattages (editable, supports custom entry)
- User adds appliance rows: name, wattage, quantity, hours/day, days/month
- Shows per-appliance monthly kWh, estimated cost, and total monthly units
- Phase selector + billing cycle toggle
- Calculates expected bill using Form 1 logic
- Export to PDF (appliance table + bill breakdown)

### Form 3: Reverse Bill Calculator (Bill to Units)
- User enters total bill amount, phase, billing cycle
- System reverse-calculates: estimated units, slab identification, complete breakdown
- Verification section: forward-calculates the result to confirm tally with Form 1
- Handles discontinuity gaps gracefully (shows nearest valid options)
- Export to PDF

### Form 4: Phase Comparison
- User enters total units consumed, billing cycle toggle
- Side-by-side comparison: single phase vs three phase
- Shows slab breakdown, all charge components, total for each phase
- Shows difference in amount + recommendation ("Single phase saves Rs. X")
- Export to PDF

### Settings Screen
- Edit all tariff rates (slab rates, fixed charges, duty %, surcharge, meter rent, GST)
- Shows "Default rates: KSEB Domestic LT-1A, effective 05.12.2024"
- Reset to Defaults button with confirmation dialog
- "Custom rates active" indicator shown on all screens when rates differ from defaults
- Tariff version tracking

---

## 2. KSEB Tariff Structure

### Default Rates (Effective 05.12.2024 to 31.03.2027)

#### Telescopic Slabs (monthly, total consumption up to 250 units):
Each slab is charged independently at its own rate.

| Slab | Unit Range | Rate/Unit (Rs) |
|------|-----------|----------------|
| 1    | 0-50      | 3.35           |
| 2    | 51-100    | 4.25           |
| 3    | 101-150   | 5.35           |
| 4    | 151-200   | 7.20           |
| 5    | 201-250   | 8.50           |

#### Non-Telescopic Slabs (monthly, above 250 units):
Flat rate applied to ALL consumed units (not just the excess).

| Consumption Range | Rate/Unit (Rs) |
|-------------------|----------------|
| 251-300           | 6.75           |
| 301-350           | 7.60           |
| 351-400           | 7.95           |
| 401-500           | 8.25           |
| 501+              | 9.20           |

#### Fixed Charges (slab-based, monthly):

| Monthly Consumption | Single Phase (Rs) | Three Phase (Rs) |
|--------------------|-------------------|-------------------|
| 0-100 units        | 35                | 85                |
| 101-200 units      | 55                | 130               |
| 201-300 units      | 75                | 160               |
| 301-500 units      | 100               | 200               |
| 501+ units         | 125               | 250               |

> **Note**: Fixed charges vary by consumption level. This creates a circular dependency in the reverse calculator (see Section 5).

#### Additional Charges:
| Charge | Rate |
|--------|------|
| Electricity Duty | 10% of energy charge |
| Fuel Surcharge | Rs 0.10/unit (configurable, changes periodically) |
| Meter Rent (Single Phase) | Rs 6/month |
| Meter Rent (Three Phase) | Rs 15/month |
| GST on Meter Rent | 18% |

#### Bimonthly Billing:
- Divide total bimonthly units by 2 to get average monthly consumption
- Calculate monthly bill using the average
- Multiply the monthly bill result by 2
- This handles slab boundaries correctly (NOT the same as doubling slab ranges)
- Fractional units from division: round down (floor)

> **Important**: All rates are stored as configurable values in DataStore. Users can modify any rate through the Settings screen.

---

## 3. Project Structure

```
app/src/main/java/com/kseb/billcalculator/
├── MainActivity.kt                           # Single activity, hosts Compose
│
├── navigation/
│   └── AppNavigation.kt                      # Bottom nav (4 tabs) + NavHost + Settings route
│
├── model/
│   ├── TariffConfig.kt                       # Data class: all tariff rates (serializable)
│   ├── BillBreakdown.kt                      # Result: slabDetails, all charges, total
│   ├── SlabDetail.kt                         # Per-slab: label, rate, unitsInSlab, charge
│   ├── Appliance.kt                          # name, wattage, quantity, hoursPerDay, daysPerMonth
│   ├── PhaseType.kt                          # Enum: SINGLE_PHASE, THREE_PHASE
│   ├── BillingCycle.kt                       # Enum: MONTHLY, BIMONTHLY
│   └── CalculationResult.kt                  # Sealed class for form-specific results
│
├── calculation/
│   ├── BillCalculator.kt                     # Forward: units + phase + cycle -> BillBreakdown
│   ├── ReverseCalculator.kt                  # Reverse: amount + phase + cycle -> units + breakdown
│   └── ApplianceCalculator.kt               # List<Appliance> -> monthly units + per-item breakdown
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt                         # Material 3 + dark mode support
│   │
│   ├── components/
│   │   ├── BillBreakdownCard.kt             # Reusable: slab table + charges summary + total
│   │   ├── SlabBarChart.kt                  # Stacked bar visualization of slab distribution
│   │   ├── ApplianceRow.kt                  # Single appliance input row with delete
│   │   ├── PhaseSelector.kt                 # SegmentedButton: Single / Three Phase
│   │   ├── BillingCycleToggle.kt            # SegmentedButton: Monthly / Bimonthly
│   │   ├── InfoTooltip.kt                   # "i" icon -> BottomSheet with explanation
│   │   └── CustomRatesIndicator.kt          # Banner when custom rates are active
│   │
│   ├── unitstobill/
│   │   ├── UnitsToBillScreen.kt             # Form 1 UI
│   │   └── UnitsToBillViewModel.kt
│   │
│   ├── appliance/
│   │   ├── ApplianceScreen.kt               # Form 2 UI
│   │   └── ApplianceViewModel.kt
│   │
│   ├── billtounits/
│   │   ├── BillToUnitsScreen.kt             # Form 3 UI
│   │   └── BillToUnitsViewModel.kt
│   │
│   ├── phasecompare/
│   │   ├── PhaseCompareScreen.kt            # Form 4 UI
│   │   └── PhaseCompareViewModel.kt
│   │
│   └── settings/
│       ├── SettingsScreen.kt                # Tariff rate editor
│       └── SettingsViewModel.kt
│
├── pdf/
│   ├── PdfGenerator.kt                      # Main PDF generator with Canvas drawing
│   ├── PdfPageManager.kt                    # Auto page-break, Y cursor tracking
│   ├── PdfTableBuilder.kt                   # Table rendering utility (borders, columns)
│   └── PdfTextBlock.kt                      # Text rendering with wrapping, alignment
│
└── data/
    └── TariffPreferences.kt                # DataStore: persist custom tariff config as JSON

app/src/main/res/
├── xml/
│   └── file_paths.xml                       # FileProvider paths for PDF sharing
├── font/
│   └── noto_sans_regular.ttf               # Bundled font for consistent PDF rendering
├── values/
│   ├── strings.xml                          # All user-facing strings (localization-ready)
│   └── colors.xml
└── drawable-nodpi/
    └── ic_app_logo.png                      # App logo (NOT KSEB logo - trademark)
```

---

## 4. Core Calculation Logic (BillCalculator.kt)

Pure Kotlin, no Android dependencies. Receives `TariffConfig` as parameter (not hardcoded).

```
function calculateBill(units: Int, phase: PhaseType, cycle: BillingCycle, tariff: TariffConfig): BillBreakdown

  // Step 1: Handle bimonthly
  if cycle == BIMONTHLY:
    monthlyUnits = floor(units / 2)
    monthlyBill = calculateBill(monthlyUnits, phase, MONTHLY, tariff)
    return monthlyBill * 2  // double all charge components

  // Step 2: Determine billing type
  slabDetails = []
  
  if units <= 250 (telescopic limit):
    // TELESCOPIC: each slab charged at its own rate
    remainingUnits = units
    for each slab in tariff.telescopicSlabs:
      slabWidth = slab.upperLimit - slab.lowerLimit + 1
      unitsInSlab = min(remainingUnits, slabWidth)
      if unitsInSlab <= 0: break
      charge = unitsInSlab * slab.rate
      slabDetails.add(SlabDetail(slab.label, slab.rate, unitsInSlab, charge))
      remainingUnits -= unitsInSlab
    totalEnergyCharge = sum of all slab charges
    
  else:
    // NON-TELESCOPIC: find flat rate band, apply to ALL units
    flatRate = tariff.getNonTelescopicRate(units)
    totalEnergyCharge = units * flatRate
    slabDetails.add(SlabDetail("All units (flat)", flatRate, units, totalEnergyCharge))

  // Step 3: Fixed charge (depends on consumption level)
  fixedCharge = tariff.getFixedCharge(units, phase)

  // Step 4: Additional charges
  electricityDuty = totalEnergyCharge * tariff.electricityDutyPercent / 100
  fuelSurcharge = units * tariff.fuelSurchargePerUnit
  meterRent = tariff.getMeterRent(phase)
  gstOnMeterRent = meterRent * tariff.gstOnMeterRentPercent / 100

  // Step 5: Total
  totalAmount = totalEnergyCharge + fixedCharge + electricityDuty + fuelSurcharge + meterRent + gstOnMeterRent

  // Step 6: Round to 2 decimal places
  return BillBreakdown(units, phase, cycle, isTelescopicBilling, slabDetails, 
    totalEnergyCharge, fixedCharge, electricityDuty, fuelSurcharge, 
    meterRent, gstOnMeterRent, totalAmount)
```

---

## 5. Reverse Calculation Logic (ReverseCalculator.kt)

This is the most complex part due to:
1. **Slab-based fixed charges**: Fixed charge depends on consumption, creating circular dependency
2. **Telescopic to non-telescopic discontinuity**: Bill jumps at 250/251 boundary
3. **Non-telescopic inter-band discontinuities**: Bill jumps at 300/301, 350/351, 400/401, 500/501

### Algorithm:

```
function reverseBill(totalAmount: Double, phase: PhaseType, cycle: BillingCycle, tariff: TariffConfig): ReverseResult

  // Handle bimonthly: solve for monthly amount, then double units
  if cycle == BIMONTHLY:
    monthlyResult = reverseBill(totalAmount / 2, phase, MONTHLY, tariff)
    return monthlyResult with units * 2

  // Strategy: check every possible fixed-charge scenario + billing type
  candidates = []

  // --- TELESCOPIC RANGE (0-250 units) ---
  for each fixedChargeRange in tariff.fixedChargeRanges:
    fixedCharge = fixedChargeRange.getCharge(phase)
    meterRent = tariff.getMeterRent(phase)
    gstOnMeterRent = meterRent * tariff.gstOnMeterRentPercent / 100
    constants = fixedCharge + meterRent + gstOnMeterRent

    // Within telescopic range, bill = energyCharge * (1 + dutyPercent/100) + units * fuelSurcharge + constants
    // Try each telescopic slab:
    baseEnergy = 0.0
    baseUnits = 0
    for each slab in tariff.telescopicSlabs:
      R = slab.rate
      lb = baseUnits  // units before this slab
      
      // In this slab: energyCharge = baseEnergy + (u - lb) * R
      // totalBill = (baseEnergy + (u - lb) * R) * 1.10 + u * 0.10 + constants
      // Solve for u:
      // u = (totalAmount - baseEnergy * 1.10 - constants + lb * R * 1.10) / (R * 1.10 + 0.10)
      
      u = solve for units in this slab
      u_rounded = round(u)
      
      if u_rounded is within this slab's range AND within fixedChargeRange:
        verify = calculateBill(u_rounded, phase, MONTHLY, tariff)
        candidates.add(Candidate(u_rounded, verify.totalAmount, abs(verify.totalAmount - totalAmount)))
      
      baseEnergy += slabWidth * R
      baseUnits += slabWidth

  // --- NON-TELESCOPIC RANGE (251+ units) ---
  for each nonTelescopicBand in tariff.nonTelescopicSlabs:
    for each fixedChargeRange in tariff.fixedChargeRanges:
      F = nonTelescopicBand.flatRate
      constants = fixedCharge + meterRent + gstOnMeterRent
      
      // totalBill = u * F * 1.10 + u * 0.10 + constants
      // u = (totalAmount - constants) / (F * 1.10 + 0.10)
      
      u = solve for units
      u_rounded = round(u)
      
      if u_rounded is within this band's range AND within fixedChargeRange:
        verify = calculateBill(u_rounded, phase, MONTHLY, tariff)
        candidates.add(Candidate(u_rounded, verify.totalAmount, abs(verify.totalAmount - totalAmount)))

  // Find best candidate(s)
  if candidates is empty:
    return ReverseResult.NoMatch(
      message = "No exact match found",
      nearestBelow = findNearestBelow(totalAmount),
      nearestAbove = findNearestAbove(totalAmount)
    )
  
  bestMatch = candidate with smallest difference
  
  // Also try floor and ceil of best match
  verify_floor = calculateBill(floor(bestMatch.units), phase, MONTHLY, tariff)
  verify_ceil = calculateBill(ceil(bestMatch.units), phase, MONTHLY, tariff)
  pick whichever is closest to totalAmount
  
  return ReverseResult.Match(
    units = bestUnits,
    breakdown = verifiedBreakdown,
    difference = abs(verifiedBreakdown.totalAmount - totalAmount),
    note = if difference > 0: "Rounding difference of Rs. X"
  )
```

### Handling Discontinuity Gaps:
When `totalAmount` falls between telescopic-250 bill and non-telescopic-251 bill (or any inter-band gap):
- Show: "No exact unit count produces this bill amount"
- Show nearest lower option: "X units = Rs. Y"
- Show nearest upper option: "X+1 units = Rs. Z"
- Let user pick which breakdown to view

---

## 6. Appliance Calculation Logic

```
data class Appliance(
  name: String,
  wattage: Double,        // watts
  quantity: Int,
  hoursPerDay: Double,    // 0-24
  daysPerMonth: Int       // 1-30 (default 30, allows for non-daily appliances)
)

function calculateMonthlyUnits(appliances: List<Appliance>): ApplianceResult
  items = []
  for each appliance in appliances:
    monthlyKwh = (appliance.wattage * appliance.hoursPerDay * appliance.quantity * appliance.daysPerMonth) / 1000.0
    items.add(ApplianceItem(appliance, monthlyKwh))
  
  totalUnits = round(sum of all monthlyKwh)  // integer units for bill calc
  return ApplianceResult(items, totalUnits)
```

### Preset Appliances with Default Wattages:
| Appliance | Default Wattage |
|-----------|----------------|
| Ceiling Fan | 75W |
| Table Fan | 50W |
| AC (1 Ton) | 1000W |
| AC (1.5 Ton) | 1500W |
| Refrigerator | 200W |
| Washing Machine | 500W |
| LED TV (32") | 50W |
| LED TV (43") | 80W |
| LED Bulb (9W) | 9W |
| LED Bulb (12W) | 12W |
| Tube Light | 40W |
| CFL | 25W |
| Iron Box | 1000W |
| Water Heater (Geyser) | 2000W |
| Mixer Grinder | 750W |
| Desktop Computer | 200W |
| Laptop | 65W |
| WiFi Router | 15W |
| Mobile Charger | 10W |
| Microwave Oven | 1200W |
| Induction Cooker | 2000W |
| Water Pump (0.5HP) | 375W |
| Water Pump (1HP) | 750W |

Users can also type a custom appliance name not in the dropdown, with wattage entered manually.

---

## 7. UI/UX Design

### Navigation
- **Bottom Navigation Bar**: 4 tabs - "Bill Calc", "Appliance", "Reverse", "Compare"
- **Top App Bar**: Screen title + Settings gear icon (top-right)
- **State preserved** across tab switches (each tab has its own ViewModel)

### Common UI Elements
- **Phase Selector**: Material 3 SegmentedButton (Single Phase / Three Phase)
- **Billing Cycle Toggle**: Material 3 SegmentedButton (Monthly / Bimonthly)
- **Info Tooltips**: Circled "i" icon next to terms like "Telescopic", "Non-telescopic", "Electricity Duty" — opens BottomSheet with plain-language explanation
- **Custom Rates Indicator**: Banner at top when user has modified tariff rates

### Form 1: Units to Bill
```
┌──────────────────────────────────┐
│  ⚡ Bill Calculator         ⚙️   │  <- Top bar + settings
├──────────────────────────────────┤
│  [Single Phase] [Three Phase]    │  <- SegmentedButton
│  [Monthly] [Bimonthly]           │  <- SegmentedButton
│                                  │
│  Units Consumed: [________]      │  <- Number input, integer only
│                                  │
│  [CALCULATE BILL]                │  <- Primary button
│                                  │
│  ┌─ Slab Breakdown ───────────┐  │
│  │ ▓▓▓▓░░░░░░░░░░░            │  │  <- Stacked bar chart
│  │ Slab      Rate  Units  Amt  │  │
│  │ 0-50      3.35   50  167.50│  │
│  │ 51-100    4.25   50  212.50│  │
│  │ 101-150   5.35   50  267.50│  │
│  │ 151-200   7.20   35  252.00│  │
│  └─────────────────────────────┘  │
│                                  │
│  ┌─ Bill Summary ─────────────┐  │
│  │ Energy Charge    ₹ 899.50  │  │
│  │ Fixed Charge     ₹  55.00  │  │
│  │ Electricity Duty ₹  89.95  │  │
│  │ Fuel Surcharge   ₹  18.50  │  │
│  │ Meter Rent       ₹   6.00  │  │
│  │ GST (Meter Rent) ₹   1.08  │  │
│  │─────────────────────────── │  │
│  │ TOTAL            ₹1070.03  │  │  <- Bold, highlighted
│  └─────────────────────────────┘  │
│                                  │
│              [📄 Export PDF]      │  <- FAB or button
├──────────────────────────────────┤
│ [Bill Calc] [Appliance] [Rev] [Cmp] │ <- Bottom nav
└──────────────────────────────────┘
```

### Form 2: Appliance Calculator
```
┌──────────────────────────────────┐
│  🔌 Appliance Calculator    ⚙️   │
├──────────────────────────────────┤
│  Appliance: [Ceiling Fan    ▼]   │  <- Dropdown (filterable, custom entry)
│  Wattage:   [75    ] W          │  <- Auto-filled, editable
│  Quantity:  [2     ]             │
│  Hours/Day: [8     ]             │
│  Days/Month:[30    ]             │  <- Default 30
│  [+ ADD APPLIANCE]               │
│                                  │
│  ┌─ Your Appliances ───────────┐ │
│  │ Ceiling Fan  75W ×2  8h 30d │🗑│
│  │   → 36.0 kWh/month          │ │
│  │ AC (1.5T) 1500W ×1  6h 30d  │🗑│
│  │   → 270.0 kWh/month         │ │
│  │ Fridge    200W ×1  24h 30d   │🗑│
│  │   → 144.0 kWh/month         │ │
│  ├─────────────────────────────┤ │
│  │ TOTAL: 450 units/month      │ │
│  └─────────────────────────────┘ │
│                                  │
│  [Single Phase] [Three Phase]    │
│  [Monthly] [Bimonthly]           │
│  [CALCULATE BILL]                │
│                                  │
│  ┌─ Bill Breakdown ────────────┐ │
│  │  (same layout as Form 1)    │ │
│  └─────────────────────────────┘ │
│              [📄 Export PDF]      │
└──────────────────────────────────┘
```

### Form 3: Reverse Calculator
```
┌──────────────────────────────────┐
│  🔄 Reverse Calculator      ⚙️   │
├──────────────────────────────────┤
│  [Single Phase] [Three Phase]    │
│  [Monthly] [Bimonthly]           │
│                                  │
│  Bill Amount (₹): [________]     │  <- Decimal input
│                                  │
│  [CALCULATE UNITS]               │
│                                  │
│  ┌─ Result ────────────────────┐ │
│  │ Estimated Units: 185        │ │
│  │ Billing Type: Telescopic    │ │
│  │ Active Slab: 151-200        │ │
│  ├─────────────────────────────┤ │
│  │ ✅ Verification:            │ │
│  │ 185 units → ₹1,070.03      │ │
│  │ (Matches input ✓)           │ │  <- Or shows rounding diff
│  ├─────────────────────────────┤ │
│  │ (Full breakdown as Form 1)  │ │
│  └─────────────────────────────┘ │
│                                  │
│  --- If amount falls in gap --- │
│  ┌─ No Exact Match ───────────┐ │
│  │ ⚠️ No exact unit count      │ │
│  │ produces this bill amount.  │ │
│  │                             │ │
│  │ Nearest options:            │ │
│  │ • 250 units → ₹1,378.xx    │ │
│  │ • 251 units → ₹1,606.xx    │ │
│  │ [View 250 breakdown]        │ │
│  │ [View 251 breakdown]        │ │
│  └─────────────────────────────┘ │
│              [📄 Export PDF]      │
└──────────────────────────────────┘
```

### Form 4: Phase Comparison
```
┌──────────────────────────────────┐
│  ⚖️ Phase Comparison         ⚙️   │
├──────────────────────────────────┤
│  [Monthly] [Bimonthly]           │
│  Units Consumed: [________]      │
│  [COMPARE]                       │
│                                  │
│  ┌─Single Phase──┬─Three Phase─┐ │
│  │ 0-50: ₹167.50 │ 0-50: ₹167.50│ │
│  │ 51-100:₹212.50│ 51-100:₹212.50│ │
│  │ ...           │ ...          │ │
│  ├───────────────┼─────────────┤ │
│  │ Energy: ₹899  │ Energy: ₹899 │ │
│  │ Fixed:  ₹55   │ Fixed:  ₹130 │ │
│  │ Duty:   ₹89   │ Duty:   ₹89  │ │
│  │ Fuel:   ₹18   │ Fuel:   ₹18  │ │
│  │ Meter:  ₹6    │ Meter:  ₹15  │ │
│  │ GST:    ₹1.08 │ GST:    ₹2.70│ │
│  ├───────────────┼─────────────┤ │
│  │ TOTAL: ₹1,068 │ TOTAL: ₹1,154│ │
│  └───────────────┴─────────────┘ │
│                                  │
│  ┌─ Recommendation ───────────┐ │
│  │ 💡 Single Phase saves       │ │
│  │    ₹86.00 per month         │ │
│  │    for 185 units usage      │ │
│  └─────────────────────────────┘ │
│              [📄 Export PDF]      │
└──────────────────────────────────┘
```

### Calculation Behavior:
- **Form 1 & 4**: Real-time calculation (debounced 300ms after keystroke). Also show Calculate button.
- **Form 2**: Calculate on button press (multiple dynamic rows make real-time distracting)
- **Form 3**: Calculate on button press (complex reverse calc, avoid showing intermediate wrong results)

### Cross-Form Navigation:
- Form 2 result shows "Compare Phases" button → pre-fills Form 4 with calculated units
- Form 3 result shows "View in Calculator" button → pre-fills Form 1 with computed units
- Form 4 result can link to Form 1 for detailed single/three-phase breakdown

### Dark Mode:
- Fully supported via Material 3 dynamic theming
- All colors, charts, and PDF export work in both light and dark mode

### Accessibility:
- All interactive elements have `contentDescription` for TalkBack
- Visible labels on all form fields (not just placeholder hints)
- Touch targets minimum 48dp × 48dp
- WCAG AA color contrast (4.5:1 for text)
- Support system font scaling up to 200%
- Phase comparison uses color + text labels (not color alone)

### Localization:
- All user-facing strings in `strings.xml` from day one
- Malayalam translation can be added in v1.1

---

## 8. PDF Generation

### Approach
- Use Android's built-in `android.graphics.pdf.PdfDocument` API
- A4 page size (595 × 842 points)
- Bundle Noto Sans font in `assets/fonts/` for consistent rendering across devices
- Custom utility classes: `PdfPageManager`, `PdfTableBuilder`, `PdfTextBlock`

### PDF Content Structure (Form 1/3):
```
┌─────────────────────────────────────┐
│  KSEB BILL ESTIMATE                 │
│  Date: 02-Apr-2026                  │
│  ──────────────────────────────── │
│  Connection Type: Single Phase      │
│  Billing Cycle: Monthly             │
│  Units Consumed: 185                │
│  Billing Type: Telescopic           │
│  ──────────────────────────────── │
│                                     │
│  SLAB-WISE ENERGY CHARGES           │
│  ┌──────────┬───────┬─────┬───────┐ │
│  │ Slab     │ Rate  │Units│Amount │ │
│  ├──────────┼───────┼─────┼───────┤ │
│  │ 0-50     │ 3.35  │ 50  │167.50 │ │
│  │ 51-100   │ 4.25  │ 50  │212.50 │ │
│  │ 101-150  │ 5.35  │ 50  │267.50 │ │
│  │ 151-200  │ 7.20  │ 35  │252.00 │ │
│  └──────────┴───────┴─────┴───────┘ │
│                                     │
│  BILL SUMMARY                       │
│  Energy Charge ............  899.50 │
│  Fixed Charge ..............  55.00 │
│  Electricity Duty (10%) ...  89.95  │
│  Fuel Surcharge ............  18.50 │
│  Meter Rent ................   6.00 │
│  GST on Meter Rent (18%) ..   1.08  │
│  ──────────────────────────────── │
│  TOTAL AMOUNT          ₹ 1,070.03   │
│  ──────────────────────────────── │
│                                     │
│  * ESTIMATE ONLY - Not an official  │
│    KSEB bill. Not affiliated with   │
│    KSEB. Tariff rates as of         │
│    05.12.2024.                      │
│  * Generated by KSEB Bill           │
│    Calculator App                   │
└─────────────────────────────────────┘
```

### PDF Variants:
- **Form 1**: Standard bill breakdown (as above)
- **Form 2**: Appliance table (name, wattage, qty, hours, kWh) + bill breakdown
- **Form 3**: Reverse result (estimated units, verification) + bill breakdown
- **Form 4**: Two-column comparison table with difference row. Consider landscape orientation.

### Multi-Page Handling:
- `PdfPageManager` tracks Y cursor position
- When content exceeds page height (minus margin), auto-create new page
- Critical for Form 2 with many appliances

### PDF Sharing:
- Generate PDF to `context.cacheDir`
- Share via `FileProvider` + `Intent.ACTION_SEND` with `EXTRA_STREAM`
- Set `FLAG_GRANT_READ_URI_PERMISSION` on intent
- Use `Intent.createChooser()` for share sheet
- Cleanup: delete PDFs older than 24 hours on app startup, cap at 10 files

### Important Notes:
- Do NOT use official KSEB logo (trademark/IP risk) — use app's own icon
- Include "ESTIMATE ONLY" disclaimer prominently
- Show tariff version/date in PDF
- If custom rates active, note "Calculated using user-customized tariff rates"
- Use Indian number formatting: ₹1,23,456.00

---

## 9. Data Architecture

### State Management:
- **ViewModel + SavedStateHandle** per screen — survives rotation and process death
- Each form's ViewModel holds input state as `MutableStateFlow` and result as `StateFlow<CalculationResult?>`
- Navigation with `saveState = true` preserves tab state across switches

### Tariff Storage:
- **Proto DataStore** (or JSON-serialized DataStore) for tariff config — better than Preferences DataStore for structured data
- `TariffPreferences.kt` provides `Flow<TariffConfig>` that all ViewModels observe
- Default tariff config hardcoded in companion object of `TariffConfig`

### Data Flow:
```
User Input → ViewModel → Calculator (pure Kotlin) → BillBreakdown → UI
                                                   ↘ PdfGenerator → PDF File → Share Intent
```

### No Database:
- No Room database in v1.0
- No network calls, no analytics SDK
- App works fully offline
- No storage permissions needed (uses cacheDir + FileProvider)

### Number Formatting:
- Use `NumberFormat.getCurrencyInstance(Locale("en", "IN"))` for ₹ display
- All calculations use `Double`, display rounded to 2 decimal places via `BigDecimal.setScale(2, RoundingMode.HALF_UP)`

---

## 10. Edge Cases & Validation

### Input Validation:

| Form | Field | Validation | Error Message |
|------|-------|-----------|---------------|
| 1, 4 | Units | Non-negative integer, max 10000 | "Enter a valid number (0-10000)" |
| 1, 4 | Units | >2000 soft warning | "Unusually high for domestic use" |
| 2 | Wattage | Positive number, max 50000 | "Enter valid wattage (1-50000)" |
| 2 | Quantity | Positive integer, 1-100 | "Enter valid quantity (1-100)" |
| 2 | Hours/Day | 0-24, decimals allowed | "Hours must be between 0 and 24" |
| 2 | Days/Month | 1-30 | "Days must be between 1 and 30" |
| 2 | Appliance list | At least 1 item to calculate | "Add at least one appliance" |
| 2 | Max rows | Cap at 50 | "Maximum 50 appliances" |
| 3 | Amount | Positive number | "Enter a valid bill amount" |
| 3 | Amount | Below minimum bill | "Amount below minimum bill (₹X)" |
| Settings | Rates | Non-negative numbers | "Invalid value" |
| Settings | Slab rates | Should be ascending | Warning (not blocking) |

### Critical Edge Cases:

1. **0 units**: Show fixed charges + meter rent only. Energy charge = 0. "No energy consumed. Fixed charges apply."

2. **Exactly 250 units**: Telescopic billing applies (250 is the upper boundary of telescopic range)

3. **251 units**: Non-telescopic billing kicks in. All 251 units at Rs 6.75 flat = Rs 1,694.25 energy charge. This is HIGHER than 250 units telescopic (Rs 1,432.50). Show info tooltip explaining the jump.

4. **Non-telescopic inter-band jumps**: At 300→301, 350→351, 400→401, 500→501 — bill jumps because flat rate increases and applies to ALL units. Handle in reverse calculator.

5. **Bimonthly odd units**: 501 bimonthly / 2 = 250.5 → floor to 250 (telescopic). Document this rounding behavior.

6. **Reverse calculator gaps**: Bill amounts that fall between two valid totals (in discontinuity zones) → show "No exact match" with nearest options.

7. **Reverse calculator: multiple valid results**: Theoretically possible near discontinuity boundaries. Show all valid options.

8. **Very large units** (5000+): Calculate normally but show domestic-use warning.

---

## 11. Implementation Order

### Phase 1: Project Setup (files: ~8)
1. Create Android project with Gradle (Kotlin DSL)
2. Configure `build.gradle.kts` — Compose BOM, Material 3, Navigation, ViewModel, DataStore
3. Set up `AndroidManifest.xml` with FileProvider
4. Create `res/xml/file_paths.xml`
5. Set up theme: `Color.kt`, `Type.kt`, `Theme.kt` (Material 3, dark mode)
6. Create `MainActivity.kt` with Scaffold
7. Add bundled font file

### Phase 2: Model & Calculation Layer (files: ~10)
8. Create `PhaseType.kt`, `BillingCycle.kt` enums
9. Create `TariffConfig.kt` with default rates
10. Create `SlabDetail.kt`, `BillBreakdown.kt`, `CalculationResult.kt`
11. Create `Appliance.kt` data class
12. Implement `BillCalculator.kt` (forward calculation)
13. Implement `ReverseCalculator.kt` (reverse calculation)
14. Implement `ApplianceCalculator.kt`
15. Write unit tests for BillCalculator at boundaries: 0, 50, 100, 150, 200, 250, 251, 300, 350, 400, 500, 501
16. Write unit tests for ReverseCalculator: verify round-trip for units 0..600
17. Write unit tests for bimonthly calculation

### Phase 3: Data Layer (files: ~1)
18. Implement `TariffPreferences.kt` — DataStore read/write for tariff config

### Phase 4: Navigation + Form 1 (files: ~5)
19. Create `AppNavigation.kt` with bottom nav + NavHost
20. Build `UnitsToBillViewModel.kt`
21. Build `UnitsToBillScreen.kt`
22. Build reusable `BillBreakdownCard.kt`
23. Build reusable `PhaseSelector.kt`, `BillingCycleToggle.kt`, `InfoTooltip.kt`

### Phase 5: Form 3 - Reverse Calculator (files: ~2)
24. Build `BillToUnitsViewModel.kt`
25. Build `BillToUnitsScreen.kt` with verification display and gap handling

### Phase 6: Form 4 - Phase Comparison (files: ~2)
26. Build `PhaseCompareViewModel.kt`
27. Build `PhaseCompareScreen.kt` with side-by-side layout + recommendation

### Phase 7: Form 2 - Appliance Calculator (files: ~3)
28. Build `ApplianceRow.kt` component (with delete, swipe-to-delete)
29. Build `ApplianceViewModel.kt` with mutable appliance list
30. Build `ApplianceScreen.kt` with dropdown, add/remove, per-item kWh

### Phase 8: Settings Screen (files: ~2)
31. Build `SettingsViewModel.kt`
32. Build `SettingsScreen.kt` with rate editors, validation, reset button
33. Build `CustomRatesIndicator.kt` banner

### Phase 9: PDF Generation (files: ~4)
34. Implement `PdfPageManager.kt` (page breaks, Y cursor)
35. Implement `PdfTableBuilder.kt` (table with borders, columns)
36. Implement `PdfTextBlock.kt` (text alignment, wrapping)
37. Implement `PdfGenerator.kt` — all 4 form variants
38. Wire PDF export into all screens with share intent

### Phase 10: Polish
39. Cross-form navigation ("Compare Phases" from Form 2, "View in Calculator" from Form 3)
40. Stacked bar chart visualization (`SlabBarChart.kt`)
41. Input validation and error messages
42. Edge case handling (0 units, gaps, large values)
43. Accessibility: contentDescription, touch targets, contrast
44. First-launch onboarding overlay (optional, can defer)

---

## 12. Verification Plan

### Calculation Verification:
| Test | Input | Expected |
|------|-------|----------|
| Zero units | 0 units, single phase, monthly | Only fixed + meter charges |
| Mid-slab | 75 units, single phase, monthly | 50×3.35 + 25×4.25 + charges |
| Full telescopic | 250 units, single phase, monthly | All 5 slabs used |
| Non-telescopic boundary | 251 units, single phase, monthly | 251×6.75 flat + charges |
| High usage | 500 units, single phase, monthly | 500×8.25 flat + charges |
| Bimonthly | 370 bimonthly, single phase | 185 monthly × 2 |
| Three phase | 185 units, three phase, monthly | Same energy, higher fixed/meter |

### Tally Verification (Form 1 ↔ Form 3):
- For every unit count from 0 to 600: `reverse(forward(units)) == units`
- Specifically test at boundaries: 250, 251, 300, 301, 350, 351, 400, 401, 500, 501

### Phase Comparison Verification:
- 185 units: verify three phase total = single phase total + (fixed charge diff) + (meter rent diff) + (GST diff)

### Appliance Verification:
- Fan 75W × 2 × 8h × 30d = 36 kWh
- Fridge 200W × 1 × 24h × 30d = 144 kWh
- Total = 180 kWh → verify bill matches Form 1 for 180 units

### PDF Verification:
- Generate PDF from each form
- Open and verify content matches on-screen display
- Verify multi-page works (Form 2 with 20+ appliances)
- Verify share intent opens system share sheet

### Settings Verification:
- Change a slab rate, recalculate, verify new rate used
- Reset to defaults, verify original rates restored
- Verify "Custom rates active" indicator appears/disappears

---

## Dependencies (build.gradle.kts)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")  // for TariffConfig JSON
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // ViewModel + SavedStateHandle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Kotlin Serialization (for TariffConfig)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
```

---

## Key Design Decisions

1. **Pure Kotlin calculation layer**: No Android dependencies in BillCalculator/ReverseCalculator. Unit-testable on JVM without emulator.

2. **TariffConfig as parameter**: Calculators receive tariff config, not hardcoded values. Enables settings, testing, and future multi-tariff support.

3. **Slab-based fixed charges**: Fixed charge varies by consumption level (not flat). Reverse calculator handles this via candidate enumeration.

4. **Bimonthly = 2 × monthly(units/2)**: Not "doubled slabs". Handles boundary conditions correctly.

5. **No KSEB branding**: App icon only, "ESTIMATE ONLY" disclaimer. No trademark issues.

6. **No network, no permissions**: Fully offline, no storage permissions needed.

7. **Bundled font**: Noto Sans for consistent PDF rendering across all devices.

8. **Indian number formatting**: ₹1,23,456.00 throughout the app.
