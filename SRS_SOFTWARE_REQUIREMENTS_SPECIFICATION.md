# Software Requirements Specification (SRS)
## Digital Susu — Rotating Savings and Credit Association (ROSCA) Management Platform

**Version:** 4.0  
**Date:** October 2026  
**Author & System Administrator:** Prince Gbolon (`princegbolon3@gmail.com`)  
**Target Platform:** Android (Kotlin, Jetpack Compose, Room SQLite Database, Material Design 3)  
**Target Market:** Republic of Liberia (LRD `L$` & USD Multi-Channel Mobile Money Ecosystem)

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) defines the functional and non-functional requirements for the **Digital Susu** mobile application. It serves as the authoritative technical contract for stakeholders, system architects, developers, and quality assurance testers regarding how the platform digitizes traditional Liberian *Susu* (Rotating Savings and Credit Association — ROSCA) operations.

### 1.2 Document Conventions
* **SHALL / MUST:** Indicates a mandatory functional or security requirement implemented in the production system.
* **SHOULD:** Indicates a recommended best practice or user experience enhancement.
* **LRD (`L$`):** Liberian Dollar, the primary currency unit displayed across ledgers, contributions, and payouts.

### 1.3 Intended Audience
* **Project Sponsor & Root Administrator:** Prince Gbolon (`princegbolon3@gmail.com`).
* **Software Engineering & Architecture Team:** Android developers maintaining Jetpack Compose UI, ViewModel state layers, and Room persistence.
* **Academic / Enterprise Evaluators & Auditors:** Reviewers inspecting SRS, PRD, SDD, and System Sequence Diagrams (SSD).

### 1.4 Product Scope
Traditional Susu savings circles in Liberia rely on paper ledgers, verbal agreements, and manual cash collection—exposing market women, small business owners, and salaried workers to ledger loss, contribution disputes, default risks, and opaque payout rotations. **Digital Susu** replaces manual bookkeeping with a role-governed, audit-logged Android platform featuring:
1. **Strict Administrative Governance:** All new accounts (`MEMBER` and `ORGANIZER`) enter a `PENDING_APPROVAL` state until vetted and assigned by the System Administrator.
2. **Targeted Susu Circle Assignment:** Administrators and Organizers create custom Susu groups, designate specific Group Organizers, and enroll members into explicit payout rotation slots (`#1, #2, ... #N`).
3. **Multi-Channel Payment Verification:** Integration with **Lonestar Cell MTN Mobile Money**, **Orange Money Liberia**, **Cash Deposit**, and **Direct Bank Transfer**, complete with USSD Push/OTP simulation and organizer verification queues.
4. **Automated Digital Receipts & Immutable Audit Trails:** Every verified payment and lump-sum payout generates a serialized digital receipt (`SUSU-2026-XXXXXX`) and an immutable `AuditLog` entry.

---

## 2. Overall Description

### 2.1 Product Perspective
Digital Susu is a standalone, offline-first Android application engineered with Clean MVVM Architecture, reactive Kotlin `StateFlow` streams, and a relational Room SQLite database (`SusuDatabase`, Schema Version 4), supplemented by a REST/OAuth2 Mobile Money Gateway (`MobileMoneyGateway`) for MTN MoMo and Orange Money Liberia.

### 2.2 User Classes and Characteristics

| User Class | Role Code | Responsibilities & Privileges |
| :--- | :--- | :--- |
| **System Administrator** | `ADMIN` | Root authority (`princegbolon3@gmail.com`). Reviews and approves/rejects pending user registrations, assigns users to specific Susu groups as `ORGANIZER` or `MEMBER`, creates new Susu circles, changes group organizers, suspends/deletes accounts, tests Mobile Money API gateways, and inspects the platform-wide immutable Audit Trail. |
| **Group Organizer** | `ORGANIZER` | Manages assigned Susu savings circles. Creates new Susu groups, generates shareable group invite codes, approves group join requests, verifies or rejects member contribution payments (`SUBMITTED` → `VERIFIED` / `REJECTED`), issues digital receipts, and executes lump-sum cycle payouts (`Release Payout & Advance Cycle`). |
| **Group Member** | `MEMBER` | Joins Susu circles via invite code or admin assignment, views personal savings metrics, tracks assigned payout turn position in the rotation schedule, submits cycle contributions with transaction references, views/shares verified digital receipts, and receives real-time in-app notifications. |

### 2.3 Operating Environment
* **Operating System:** Android 7.0 (API Level 24) through Android 15 (API Level 35).
* **Hardware:** Smartphones and tablets (supports Adaptive Material 3 layouts).
* **Database Engine:** Local SQLite via AndroidX Room 2.6+ (`susu_database`).
* **Network:** Works 100% offline for core ledger, receipt, and audit operations; uses HTTPS/REST when invoking live Lonestar MTN MoMo or Orange Money APIs.

### 2.4 Design and Implementation Constraints
* **Clean Device Initialization Policy:** Every fresh installation provisions **only** the root System Administrator (`princegbolon3@gmail.com` / `2004prada22`) and a `SYSTEM_INITIALIZED` audit entry. Zero fake or pre-seeded dummy users/groups are permitted on clean deployment.
* **No Default Group Hardcoding:** Newly approved users are never forced into a hardcoded default group; the Administrator explicitly selects which Susu group (or creates a new group) and assigns their role (`ORGANIZER` or `MEMBER`) and rotation slot.

---

## 3. System Features & Functional Requirements

### 3.1 Authentication & Account Lifecycle Management (`FR-AUTH`)
* **FR-001 (User Self-Registration):** The system SHALL allow prospective users to register by providing Full Name, Liberian Phone Number (`+231...`), Email Address, Account Role (`MEMBER` or `ORGANIZER`), and Password.
* **FR-002 (Pending Approval Gate):** Upon registration, the system SHALL set `User.status = "PENDING_APPROVAL"`, log a `USER_REGISTERED` audit event, notify the Administrator, and block portal login with a dedicated `AccountPendingApprovalScreen` until approved.
* **FR-003 (Dual Portal Login):** The Auth screen SHALL provide distinct login modes for **Member / Organizer** and **System Admin**, authenticating credentials against `SusuDao` and routing active users to their role-specific dashboard.
* **FR-004 (Admin Approval & Targeted Group Assignment):** When approving a pending user, the Administrator SHALL be presented with the `ApproveAndAssignDialog` to:
  1. Approve the account without immediate group assignment, OR
  2. Approve and immediately enroll the user into any existing Susu group with an explicit role (`MEMBER` or `ORGANIZER`) and payout position, OR
  3. Approve and launch `CreateGroupDialog` to create a brand-new Susu circle tailored for that user.
* **FR-005 (Account Suspension & Deletion):** The Administrator SHALL be able to suspend (`SUSPENDED`), reactivate (`ACTIVE`), or permanently delete non-admin user accounts.

### 3.2 Susu Circle & Membership Management (`FR-GROUP`)
* **FR-006 (Susu Circle Creation):** Administrators and Organizers SHALL be able to create Susu groups specifying Group Name, Unique Invite Code (auto-generated or custom), Description, Contribution Amount (`L$`), Frequency (`DAILY`, `WEEKLY`, `BIWEEKLY`, `MONTHLY`), Total Cycles, Designated Organizer, and Initial Enrolled Members.
* **FR-007 (Automatic Cycle 1 Initialization):** Creating a Susu group SHALL automatically initialize Cycle #1 (`ContributionCycle` with `status = "ACTIVE"`), enroll the designated Organizer at Payout Position `#1`, and enroll selected initial members in sequential rotation slots (`#2, #3, ...`).
* **FR-008 (Invite Code Join Flow):** Active users SHALL be able to join an existing Susu circle by entering its `groupCode`, which assigns them the next available `payoutPosition` and generates current-cycle contribution records.
* **FR-009 (Dynamic Organizer Assignment):** The Administrator SHALL be able to assign or reassign the Group Organizer of any Susu circle at any time via `AssignOrganizerDialog`.
* **FR-010 (Member Enrollment & Removal):** The Administrator and Group Organizer SHALL be able to enroll registered users into specific Susu groups (`AddMemberToGroupDialog` / `AssignUserToGroupDialog`) or remove members from a circle.

### 3.3 Contribution & Mobile Money Payment Processing (`FR-PAY`)
* **FR-011 (Cycle Contribution Tracking):** For every active cycle, the system SHALL track each enrolled member's contribution status (`UNPAID`, `PENDING`, `PAID`, `LATE`).
* **FR-012 (Multi-Channel Payment Submission):** Members SHALL submit cycle payments by selecting a payment channel (**Lonestar MTN Mobile Money**, **Orange Money Liberia**, **Cash Deposit**, or **Direct Bank Transfer**), entering or auto-generating a unique Transaction Reference, and adding an optional note.
* **FR-013 (Duplicate Reference Prevention):** The database SHALL enforce a unique index on `Payment.transactionReference` to prevent replay or double-submission of Mobile Money transaction IDs.
* **FR-014 (Mobile Money USSD Push & Sandbox/Live Gateway):** The system SHALL integrate `MobileMoneyGateway` supporting automated USSD Push collection prompts and sandbox/live credential detection for Lonestar Cell MTN (`momodeveloper.mtn.com`) and Orange Money Liberia (`developer.orange.com`).
* **FR-015 (Organizer Payment Verification & Rejection):** Group Organizers SHALL review `SUBMITTED` payments in their verification queue and either:
  * **Verify (`VERIFIED`):** Marks the member's cycle contribution as `PAID`, increments total verified pot collection, issues an official Digital Receipt, and notifies the member.
  * **Reject (`REJECTED`):** Records a mandatory rejection reason, resets the contribution status so the member can resubmit, and notifies the member.

### 3.4 Rotation Schedule, Lump-Sum Payouts & Cycle Advancement (`FR-PAYOUT`)
* **FR-016 (Transparent Rotation Schedule):** The `TurnsScreen` SHALL display the ordered payout schedule (`payoutPosition #1 .. #N`), highlighting past recipients (`COMPLETED`), the current cycle's recipient (`ACTIVE`), and upcoming turns.
* **FR-017 (Lump-Sum Payout Execution):** When cycle contributions are collected, the Organizer SHALL execute `Release Payout & Advance Cycle`, recording the recipient (`recipientUserId`), total pot amount (`contributionAmount × memberCount`), disbursement method, and transfer reference.
* **FR-018 (Automatic Cycle Advancement):** Upon payout completion, the system SHALL mark the current `ContributionCycle` as `COMPLETED`, issue a Payout Receipt, and either advance the group to Cycle `N + 1` (initializing fresh contribution rows for all members) or mark the Susu group `COMPLETED` if the final cycle has finished.

### 3.5 Digital Receipts, Financial Reports & Audit Trail (`FR-AUDIT`)
* **FR-019 (Serialized Digital Receipts):** Every verified contribution and payout SHALL generate a `Receipt` entity with a unique serial number (`SUSU-2026-XXXXXX`), timestamp, group name, member name, cycle number, payment channel, and reference, viewable and shareable from `ReceiptsScreen`.
* **FR-020 (Financial Analytics & CSV/Text Export):** The `ReportsScreen` SHALL calculate real-time collection progress, group completion percentages, verified volume vs. outstanding balances, and allow exporting formatted financial ledger reports via the Android Share Sheet.
* **FR-021 (Immutable System Audit Trail):** All state-mutating operations (`SYSTEM_INITIALIZED`, `USER_REGISTERED`, `USER_APPROVED`, `GROUP_CREATED`, `MEMBER_ASSIGNED`, `ORGANIZER_ASSIGNED`, `PAYMENT_SUBMITTED`, `PAYMENT_VERIFIED`, `PAYMENT_REJECTED`, `PAYOUT_COMPLETED`) SHALL write an immutable record to the `audit_logs` table, searchable and filterable by the Administrator.

---

## 4. External Interface Requirements

### 4.1 User Interfaces (Jetpack Compose Material 3)
* **Color Palette:** Liberian Emerald Green (`#006B3F`), Susu Gold (`#D4AF37`), Deep Slate Surface (`#102018`), and high-contrast status badges.
* **Core Screens:**
  1. `AuthScreen`: Dual-tab login (Member/Organizer vs. Admin) and New Account Registration form.
  2. `AccountPendingApprovalScreen`: Displayed when a user attempts login prior to Admin approval.
  3. `AdminDashboardScreen`: 4-tab command center (*Audit Trail*, *Users Directory & Approvals*, *Groups Oversight*, *MoMo APIs*).
  4. `OrganizerDashboardScreen`: Group selector, pot collection progress card, Pending Payment Verification Queue, Member Applicant Queue, and One-Tap Payout Release card.
  5. `MemberDashboardScreen`: Personal savings summary, Active Susu Group card, Cycle Payment Status card, and Quick Actions (*Pay Cycle*, *Join Group*, *Receipts*, *Rotation*).
  6. `TurnsScreen`, `GroupMembersScreen`, `ReceiptsScreen`, `ReportsScreen`, `NotificationsScreen`, and `ManageHubScreen`.

### 4.2 Software & API Interfaces
* **Lonestar Cell MTN MoMo Open API:** OAuth 2.0 Bearer Token + `POST /collection/v1_0/requesttopay` (`X-Reference-Id`, `Ocp-Apim-Subscription-Key`).
* **Orange Money Liberia Web/M-Payment API:** OAuth 2.0 Client Credentials + `POST /orange-money-webpay/lr/v1/webpayment`.
* **Secret Configuration:** API keys (`MTN_MOMO_SUBSCRIPTION_KEY`, `MTN_MOMO_API_USER`, `MTN_MOMO_API_KEY`, `ORANGE_MONEY_CLIENT_ID`, `ORANGE_MONEY_CLIENT_SECRET`, `ORANGE_MONEY_MERCHANT_KEY`) are injected via `BuildConfig` from `.env` / AI Studio Secrets Panel.

---

## 5. Non-Functional Requirements

* **NFR-001 (Data Integrity & ACID Transactions):** All multi-step financial mutations (payment verification + receipt generation + cycle update + audit logging) SHALL execute atomically on background IO coroutines (`Dispatchers.IO`).
* **NFR-002 (Offline-First Availability):** 100% of ledger, group management, receipt generation, and audit trail inspection SHALL function with zero internet connectivity using Room SQLite persistence.
* **NFR-003 (Responsiveness):** UI state updates driven by Kotlin `StateFlow` and Room reactive `Flow` queries SHALL render within `< 100ms` of database mutation.
* **NFR-004 (Accessibility & Touch Targets):** Interactive controls SHALL maintain a minimum touch target of `48dp × 48dp`, semantic `contentDescription` labels, and `Modifier.testTag(...)` identifiers for automated verification.
