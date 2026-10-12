# Comprehensive Test Scenarios: Counter-Drone Threat Management System

This document outlines the systematic test procedures to validate explainable risk scoring, intelligent alert prioritization, SQLite persistence, and duplicate-suppression policies in the **Counter-Drone Threat Management System**.

---

## Summary of Heuristic Demonstration Rules

- **Proximity Rule**: Distance $\le 300\text{ m} \implies +35\text{ points}$
- **Kinematic Velocity Rule**: Speed $\ge 100\text{ km/h} \implies +20\text{ points}$
- **Persistence / Repetition Rule**: Repetitions $\ge 3\text{ bursts} \implies +20\text{ points}$
- **Authorization / IFF Rule**:
  - `UNAUTHORIZED` $\implies +25\text{ points}$
  - `UNKNOWN` $\implies 0\text{ points}$ (Neutral handling with explicit operator warning)
  - `AUTHORIZED` $\implies 0\text{ points}$ (Verified friendly flight plan)
- **Score Normalization**: Clamped between $0$ and $100$.
- **Categorization Tiers**:
  - `0 – 24`: **LOW**
  - `25 – 49`: **MEDIUM**
  - `50 – 74`: **HIGH**
  - `75 – 100`: **CRITICAL**

---

## Test Scenario Matrix

| Test ID | Scenario Description | Inputs (Dist, Speed, Reps, Auth) | Expected Score & Category | Triggered Factors | Expected Operational Action |
|---|---|---|---|---|---|
| **TS-01** | Nominal Friendly Surveillance | Dist: `850m`, Spd: `35 km/h`, Rep: `1`, Auth: `AUTHORIZED` | **0 (LOW)** | None (0 rules triggered) | Routine surveillance log. No alert escalation. |
| **TS-02** | Proximity Rule Only (Authorized) | Dist: `180m`, Spd: `40 km/h`, Rep: `1`, Auth: `AUTHORIZED` | **35 (MEDIUM)** | Proximity Breach ($\le 300\text{m}$) | Score 35. Medium watch queue. |
| **TS-03** | Speed Rule Only (Authorized) | Dist: `750m`, Spd: `120 km/h`, Rep: `1`, Auth: `AUTHORIZED` | **20 (LOW)** | High Velocity ($\ge 100\text{ km/h}$) | Score 20. Low threat category. |
| **TS-04** | Repetition Rule Only (Authorized) | Dist: `600m`, Spd: `45 km/h`, Rep: `4`, Auth: `AUTHORIZED` | **20 (LOW)** | Persistent Incursion ($\ge 3$) | Score 20. Low threat category. |
| **TS-05** | Unauthorized Status Only (Distant) | Dist: `900m`, Spd: `30 km/h`, Rep: `1`, Auth: `UNAUTHORIZED` | **25 (MEDIUM)** | Unauthorized Asset | Score 25. Medium tier incursion. |
| **TS-06** | Unknown IFF Status Neutrality | Dist: `800m`, Spd: `40 km/h`, Rep: `1`, Auth: `UNKNOWN` | **0 (LOW)** | None | Neutral zero points. Explicit note: "IFF is UNKNOWN". |
| **TS-07** | Severe Unauthorized Incursion | Dist: `150m`, Spd: `130 km/h`, Rep: `1`, Auth: `UNAUTHORIZED` | **80 (CRITICAL)** | Proximity (35) + Speed (20) + Auth (25) | Score 80. Auto-triage Incident created. |
| **TS-08** | Full Multi-Factor Critical Threat | Dist: `100m`, Spd: `140 km/h`, Rep: `5`, Auth: `UNAUTHORIZED` | **100 (CRITICAL)** | All 4 rules triggered ($35+20+20+25=100$) | Immediate interdiction priority (#1 in queue). |
| **TS-09** | Persistent Close Unknown Contact | Dist: `220m`, Spd: `50 km/h`, Rep: `4`, Auth: `UNKNOWN` | **55 (HIGH)** | Proximity (35) + Repetition (20) | Score 55. High alert + Auto-incident. |
| **TS-10** | Fast Recurring Unknown Scout | Dist: `450m`, Spd: `115 km/h`, Rep: `3`, Auth: `UNKNOWN` | **40 (MEDIUM)** | Speed (20) + Repetition (20) | Score 40. Medium watch queue. |
| **TS-11** | Boundary Distance (Exact 300m) | Dist: `300.0m`, Spd: `50 km/h`, Rep: `1`, Auth: `AUTHORIZED` | **35 (MEDIUM)** | Proximity Breach ($\le 300\text{m}$) | Edge test: $\le 300\text{m}$ triggers successfully. |
| **TS-12** | Boundary Speed (Exact 100 km/h) | Dist: `500m`, Spd: `100.0 km/h`, Rep: `1`, Auth: `AUTHORIZED` | **20 (LOW)** | High Velocity ($\ge 100\text{ km/h}$) | Edge test: $\ge 100\text{ km/h}$ triggers successfully. |
| **TS-13** | Duplicate Event ID Submission | Resubmit `EVT-1001` with new distance `110m` | Updated Score | Replaces previous event record | No duplicate row created; original record updated in SQLite. |
| **TS-14** | Duplicate Incident Suppression | Resubmit high threat for existing open incident | High/Critical | Incident auto-triage policy | Suppressed duplicate incident; updates audit notes on existing incident. |
| **TS-15** | Follow-up Incident Post-Resolution | Resolve incident `INC-1001`, then submit new high threat for same event | High/Critical | Reopening / New ticket policy | New incident created (`INC-XXXX`) after previous was marked `RESOLVED`. |
| **TS-16** | Input Validation Error Handling | Dist: `-50`, Speed: `abc`, Rep: `0`, Event ID: ` ` | Rejection Dialog | ValidationUtils validation | Modal error message displayed. Entered fields preserved without crash. |

---

## Detailed Step-by-Step Test Execution Instructions

### Test 1: Testing Event Ingestion and Explainable Risk Scoring
1. Launch the application:
   ```powershell
   java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" Main
   ```
2. Click **➕ Ingest Detection** in the top navigation bar.
3. Click the preset button **"Critical Intruder"**. Notice that the fields automatically populate:
   - Distance: `150.0`
   - Speed: `125.0`
   - Repetitions: `4`
   - Authorization: `UNAUTHORIZED`
4. Click **⚡ Evaluate Risk & Save Event**.
5. **Verify**:
   - An explainability dialog opens showing:
     - Composite Score: `100 / 100 [CRITICAL]`
     - Triggered Rules: Proximity (+35), Speed (+20), Repetitions (+20), Unauthorized (+25)
     - Non-Triggered Rules: "All evaluation rules were triggered."
     - Tactical summary paragraph recommending interdiction readiness.
   - The alert appears at the top of the **Prioritized Alert Queue** with priority `#1`.
   - The **Summary Cards** update immediately to reflect the new detection.

### Test 2: Testing Duplicate Event ID Submission & Alert Queue Update
1. Open **➕ Ingest Detection**.
2. Set Event ID to an existing ID (e.g., `EVT-1001`).
3. Change Distance to `500.0m`, Speed to `40 km/h`, Repetitions to `1`, Authorization to `AUTHORIZED`.
4. Click **Evaluate Risk & Save Event**.
5. **Verify**:
   - The event record for `EVT-1001` is updated in SQLite rather than creating a second `EVT-1001` record.
   - The total detection count on the dashboard does **not** increase.
   - The priority ranking in the alert queue updates to reflect the new, lower score.

### Test 3: Testing Duplicate Incident Prevention Policy
1. Ingest an event with score $\ge 50$ (e.g., Distance `180m`, Speed `120 km/h`, `UNAUTHORIZED`).
2. Verify that an incident is automatically created in `OPEN` status.
3. Ingest the same Event ID again with high-risk telemetry.
4. Open **🚨 Manage Incidents**.
5. **Verify**:
   - No second unresolved ticket exists for that Event ID.
   - The existing incident's notes contain an appended timestamp note:
     `Re-detected: Score ... Duplicate incident suppressed.`

### Test 4: Testing Incident Resolution and Follow-up Ticket Creation
1. In **🚨 Manage Incidents**, select the incident from Test 3.
2. In the right editor panel, change **Status** to `RESOLVED` and click **💾 Save Incident Changes**.
3. Ingest another high-risk detection for that same Event ID.
4. **Verify**:
   - Because the previous incident was `RESOLVED`, a **new** incident ticket is legitimately created for the new incursion.

### Test 5: Testing Empirical Evaluation & Baseline Benchmark
1. Click **📊 Run Evaluation & Benchmark** in the bottom command bar.
2. Click **⚡ Run Evaluation Benchmark**.
3. **Verify**:
   - Proposed Accuracy: $\approx 100\%$
   - Baseline Accuracy: $\approx 66.7\%$ (due to naive classifier's false alarms and missed unauthorized loiterers)
   - Explainability Coverage: $100\%$ for Proposed vs $0\%$ for Baseline.
   - Confusion matrices render TP, FP, TN, FN clearly.
   - Batch Latency displays processing time for $1,000$ synthetic evaluations ($< 50\text{ ms}$).
4. Click **💾 Save Results to SQLite** to persist the experiment run.
