# Product Requirements Document (PRD)
## Digital Susu — Community Savings & ROSCA Management App

**Product Name:** Digital Susu  
**Version:** 4.0  
**Product Owner & Administrator:** Prince Gbolon (`princegbolon3@gmail.com`)  
**Platform:** Android Mobile Application  
**Primary Market:** Liberia (Monrovia, West Point, Red Light, Duala, and nationwide community savings circles)

---

## 1. Executive Summary & Product Vision

In Liberia and across West Africa, **Susu** (a Rotating Savings and Credit Association, or ROSCA) is the foundational financial instrument for market women, entrepreneurs, community associations, and salaried workers. Members contribute a fixed amount (`L$`) at regular intervals (daily, weekly, biweekly, or monthly), and each cycle one member receives the entire lump-sum pot until everyone has had their turn.

Despite handling millions of Liberian Dollars daily, traditional Susu groups still rely on paper cards, physical passbooks, and informal cash collectors. This leads to lost records, disputed payments, delayed payouts, and lack of visibility.

**Digital Susu** transforms traditional Susu into a transparent, mobile-first financial platform that preserves community trust while introducing bank-grade accountability:
* **Verified Identity & Admin Vetting:** Every Organizer and Member is vetted by the System Administrator before accessing the platform.
* **Flexible Multi-Group Governance:** Administrators and Organizers can create distinct Susu circles, assign dedicated Group Organizers, and enroll members into clear payout rotation positions.
* **Mobile Money Native:** Built specifically for **Lonestar Cell MTN Mobile Money** and **Orange Money Liberia**, alongside traditional Cash Deposits.
* **Tamper-Proof Transparency:** Every contribution, verification, and lump-sum payout produces a serialized digital receipt and an immutable audit log.

---

## 2. Target User Personas

### Persona 1: Prince Gbolon — System Administrator (`ADMIN`)
* **Goals:** Maintain platform security, prevent fraudulent accounts, oversee all active Susu circles, assign approved users to their appropriate Susu groups as Organizers or Members, and monitor total platform transaction volume.
* **Pain Points Solved:** Eliminates unverified accounts and gives full visibility into every group, member slot, and transaction across the platform.

### Persona 2: Ma Korpo — Susu Mother / Group Organizer (`ORGANIZER`)
* **Background:** Leads a 12-member weekly market Susu circle in Monrovia (`L$ 5,000` per week).
* **Goals:** Easily track who has paid for the current cycle, verify MTN MoMo and Orange Money transfer references before marking a member paid, and disburse the lump-sum pot (`L$ 60,000`) to the scheduled recipient on time.
* **Pain Points Solved:** No more lost paper tally cards or arguments over whether a member sent their Mobile Money contribution.

### Persona 3: Moses Kollie — Small Business Owner / Group Member (`MEMBER`)
* **Background:** Participates in a Susu group to save for shop inventory.
* **Goals:** Know exactly which cycle is his payout turn, submit his weekly contribution via Lonestar MTN MoMo or Orange Money in seconds, and receive an official digital receipt he can save or share on WhatsApp.
* **Pain Points Solved:** Complete transparency into the group's rotation order and instant proof of payment.

---

## 3. Core Product Workflows & User Journeys

### Journey 1: Clean Onboarding & Admin Vetting with Targeted Group Assignment
1. **Self-Registration:** A new user installs Digital Susu, opens the **Create Account** tab, enters their name, phone number, email, desired role (`Group Member` or `Group Organizer`), and password.
2. **Pending Hold:** The user sees the **Account Pending Approval** screen explaining that the Administrator must review and accept their account.
3. **Admin Review & Group Assignment:** Administrator Prince Gbolon logs in (`princegbolon3@gmail.com`), sees the pending badge on the **Users** tab, taps **Review & Assign Group**, and chooses to:
   * Assign the user directly to an existing Susu group as `ORGANIZER` or `MEMBER` (with a specific turn slot),
   * Create a brand-new Susu group on the spot for that user, or
   * Approve the account for independent group joining via invite code.

### Journey 2: Creating & Managing a Susu Savings Circle
1. **Group Setup:** The Admin or Organizer opens the **Create Susu Group** dialog, defines the circle name, invite code, contribution amount (`L$`), frequency (`WEEKLY`, `MONTHLY`, etc.), total cycles, selects the **Group Organizer**, and checks off initial **Group Members** to enroll.
2. **Rotation Initialization:** The app automatically creates Cycle #1, assigns sequential rotation turns (`#1, #2, #3...`), and notifies all enrolled members.
3. **Ongoing Oversight:** From the Admin **Groups** tab or Organizer **Manage** hub, leadership can add new members, remove members, or change the Group Organizer at any time.

### Journey 3: Submitting & Verifying Cycle Contributions
1. **Member Payment:** On the Member Dashboard, the member taps **Pay Cycle**, selects **Lonestar MTN Mobile Money** (or **Orange Money Liberia** / **Cash Deposit**), triggers an instant USSD Push or enters their SMS transaction reference, and submits.
2. **Organizer Verification:** The Organizer sees the payment in the **Pending Verification Queue**, cross-checks the reference/USSD confirmation, and taps **Verify ✓**.
3. **Receipt Issuance:** The app immediately generates a serialized Digital Receipt (`SUSU-2026-XXXXXX`), updates the group pot progress bar, and sends a confirmation notification to the member.

### Journey 4: Lump-Sum Payout & Cycle Advancement
1. **Pot Readiness:** Once members have contributed to the active cycle, the Organizer reviews the scheduled recipient for that cycle on the Organizer Dashboard or **Turns** screen.
2. **Payout Release:** The Organizer taps **Release Payout & Advance Cycle**, records the disbursement method and transfer reference, and confirms.
3. **Cycle Roll-Over:** The recipient receives a Payout Notification and official receipt, the current cycle closes as `COMPLETED`, and the circle automatically advances to the next cycle (`Cycle N + 1`).

---

## 4. Feature Prioritization Matrix (MoSCoW)

| Priority | Feature Module | Status in v4.0 |
| :--- | :--- | :--- |
| **MUST HAVE** | Role-Based Portals (`ADMIN`, `ORGANIZER`, `MEMBER`) & Pending Account Approval Gate | ✅ Implemented |
| **MUST HAVE** | Targeted Group & Role Assignment (`ApproveAndAssignDialog`, `AssignOrganizerDialog`, `AddMemberToGroupDialog`) | ✅ Implemented |
| **MUST HAVE** | Multi-Channel Contributions (Lonestar MTN MoMo, Orange Money, Cash, Bank) with Organizer Verification | ✅ Implemented |
| **MUST HAVE** | Automated Rotation Schedule (`TurnsScreen`), Lump-Sum Payouts, and Cycle Advancement | ✅ Implemented |
| **MUST HAVE** | Serialized Digital Receipts (`SUSU-2026-XXXXXX`) & Immutable Admin Audit Trail | ✅ Implemented |
| **SHOULD HAVE** | Live & Sandbox Mobile Money API Gateways (`MobileMoneyGateway` in Admin Tab 4) | ✅ Implemented |
| **SHOULD HAVE** | Financial Reports & Shareable Ledger Export (`ReportsScreen` via Android Share Sheet) | ✅ Implemented |
| **SHOULD HAVE** | Built-in SRS, PRD, SDD & SSD Engineering Documentation Viewer & Exporter | ✅ Implemented |

---

## 5. Key Performance Indicators (KPIs) & Success Metrics

1. **Zero Unverified Access:** 100% of newly registered accounts require explicit administrative approval before accessing financial ledgers.
2. **100% Receipt Coverage:** Every `VERIFIED` payment and `COMPLETED` payout has a corresponding serialized `Receipt` and `AuditLog` record.
3. **Cycle Settlement Speed:** Organizers can verify a Mobile Money contribution and issue a digital receipt in under 3 taps (`< 5 seconds`).
4. **Offline Reliability:** Zero data loss across app restarts or network dropouts via local Room SQLite persistence.
