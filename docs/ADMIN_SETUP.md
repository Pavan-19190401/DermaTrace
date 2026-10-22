# DermaTrace – Admin Module & Staffing Rota Setup

## 1. Overview
The DermaTrace Admin Module provides clinical decision-support triage oversight, staffing rota management, model health monitoring, and user account management without compromising patient privacy.

### Clinical & Privacy Guardrails
- **Pseudonymous Review**: Admins only see pseudonymous IDs (`CASE-xxxx`, `LES-xxx`), change scores, classifier confidences, and ABCDE descriptors.
- **On-Device Clinical Isolation**: Raw patient lesion photos and identity details remain encrypted on patient devices and are **never** displayed in admin lists.
- **Clinical Triage Tool**: DermaTrace is a decision-support triage tool (Siamese CNN change score + classifier → *No Visit Needed* / *Visit a Doctor* / *Inconclusive*), not a definitive diagnostic system.

---

## 2. Authentication & Custom Claims
Admin authorization is governed by Firebase Authentication **Custom Claims**:
```json
{
  "role": "admin"
}
```

### Granting Admin Privileges
Run the provided Node.js CLI script:
```bash
# Grant admin claim
node tools/make_admin.js grant clinician@dermatrace.com

# Check claim status
node tools/make_admin.js status clinician@dermatrace.com

# Revoke admin claim
node tools/make_admin.js revoke clinician@dermatrace.com
```

### Session Persistence & Routing
- When an authenticated user opens the app or logs in:
  1. The token custom claims are inspected (`getIdToken(true)`).
  2. If `role == 'admin'` (or in demo mode, an email starting with `admin`), the app routes directly to `AdminActivity` (`AdminRoot()`).
  3. If not an admin, the app stays on the patient interface.
  4. If an account is suspended (`status == 'suspended'`), authentication is blocked with:
     > *"Account suspended. Please contact administrator."*

---

## 3. Seed Implementation & Offline Fallback
- `SeedAdminRepository` contains rich seed data matching `design/admin-mobile.html` (7-day demand vs capacity, shifts, candidate reviewers, model services, and users).
- When active, the footer note is clearly shown:
  > *"Illustrative figures only. Live values appear once real analysis and rota data are connected."*
- To connect to a live Firebase project, place `google-services.json` in the `app/` directory and configure Firestore sync.

---

## 4. Admin Feature Tour
1. **Staffing Board**:
   - "This week / Next week" segmented toggle.
   - 4 KPI cards (Forecast analyses, Over-capacity days, Reviewer coverage, Open gaps).
   - Interactive 7-day Demand vs Capacity bar chart with threshold indicators.
   - Coverage gaps with one-tap "Assign Reviewer" bottom sheet and instant capacity updates.
   - On-shift reviewer workload trackers with amber warning threshold (>90%).
2. **Review Queue**:
   - Filter by outcome (*Visit a Doctor*, *Inconclusive*, *Unassigned*).
   - Clinical note entry and one-tap sign-off.
3. **Overview & Model Health**:
   - Real-time stat tiles and decision distribution (Visit Doctor / No Visit / Inconclusive).
   - Model service status list with restart confirmation dialog and TFLite reinitialization.
   - Live inspection of `model_card.json` specifications and metrics.
   - Decision cut-off threshold adjustment with safety warnings and audit logging.
4. **Users**:
   - Live search filter by name or email.
   - One-tap bottom sheet to suspend or restore accounts.
5. **Profile & Audit**:
   - Alert notifications configuration.
   - Regulatory triage disclaimer.
   - Immutable security audit log.
   - Secure sign-out.
