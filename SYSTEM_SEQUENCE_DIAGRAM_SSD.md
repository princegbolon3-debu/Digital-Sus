# Digital Susu Management Platform
## System Sequence Diagram (SSD) & System Specification Document

**Author & Administrator:** Prince Gbolon (`princegbolon3@gmail.com`)  
**Platform:** Android Jetpack Compose & Room Local Persistence  
**Domain:** Liberian Rotating Savings and Credit Association (ROSCA / Traditional Susu)

---

## 1. System Overview & Architecture Boundary

The **Digital Susu** platform digitizes the traditional Liberian Susu savings model while preserving community trust, transparency, and accountability. The system enforces strict role-based separation:

1. **System Administrator (`ADMIN`):** Root authority held by Prince Gbolon. Responsible for reviewing and approving all newly registered organizers and members, auditing platform volume, inspecting immutable audit logs, and overseeing all Susu circles.
2. **Group Organizer (`ORGANIZER`):** Creates savings circles, establishes contribution rates and rotation cycles, reviews pending member applications for their circle, verifies Mobile Money (Lonestar MTN / Orange Money) and Cash payments, issues digital receipts, and completes lump sum payouts.
3. **Group Member (`MEMBER`):** Joins savings circles using verified group invite codes, tracks personal savings progress, views their assigned turn in the rotation schedule, submits contributions with transaction references, and accesses digital receipts.

### Device Isolation Policy
* Each newly initialized device launches with a clean local database containing **only** the root Administrator account (`princegbolon3@gmail.com` / `2004prada22`).
* No dummy or mock accounts exist.
* Any organizer or member installing the application must register on first launch. Their registration is held in `PENDING_APPROVAL` until explicitly approved by the Administrator.
* Once approved, users authenticate with their registered credentials and their personal data appears on their dashboard.

---

## 2. System Sequence Diagrams (SSD)

### SSD-01: First-Time User Registration & Administrator Approval Flow
This diagram illustrates the registration lifecycle for both Members and Organizers, requiring administrative approval before portal access is granted.

```
Member / Organizer                    System Boundary                  Administrator (Prince Gbolon)
       |                                     |                                       |
       | 1. Submit Registration Details      |                                       |
       |    (Name, Phone, Email, Role, Pwd)  |                                       |
       |------------------------------------>|                                       |
       |                                     |                                       |
       |                                     | 2. Create User Record                 |
       |                                     |    (Status = PENDING_APPROVAL)        |
       |                                     | 3. Create AuditLog (USER_REGISTERED)  |
       |                                     | 4. Create Notification for Admin      |
       |                                     |-------------------------------------->|
       |                                     |                                       |
       | 5. Return Registration Awaiting     |                                       |
       |    Admin Approval Notice            |                                       |
       |<------------------------------------|                                       |
       |                                     |                                       |
       | [User Attempts Login Before Approval]                                       |
       | 6. Submit Login Credentials         |                                       |
       |------------------------------------>|                                       |
       | 7. Return "Account Pending Approval:|                                       |
       |    Admin must accept account"       |                                       |
       |<------------------------------------|                                       |
       |                                     |                                       |
       |                                     | 8. Log into Admin Portal              |
       |                                     |    (princegbolon3@gmail.com /         |
       |                                     |     2004prada22)                      |
       |                                     |                                       |
       |                                     | 9. View "Pending Account Approvals"   |
       |                                     |    in Users Directory                 |
       |                                     |                                       |
       |                                     | 10. Click "Approve Account ✓"         |
       |                                     |-------------------------------------->|
       |                                     | 11. Update User Status = "ACTIVE"     |
       |                                     | 12. Create AuditLog (USER_APPROVED)   |
       |                                     | 13. Create Notification for User      |
       |                                     |                                       |
       | 14. Member/Organizer Signs In with  |                                       |
       |     Registered Credentials          |                                       |
       |------------------------------------>|                                       |
       |                                     | 15. Verify Hash/Password & Status     |
       | 16. Grant Access to Dedicated App   |                                       |
       |     (Member or Organizer Dashboard) |                                       |
       |<------------------------------------|                                       |
```

---

### SSD-02: Authentication & Role-Based App Routing
```
User (Member / Organizer / Admin)       Auth Gateway (System)              Application Shell
       |                                         |                                  |
       | 1. Select Portal (User or Admin)        |                                  |
       | 2. Enter Phone/Email + Password         |                                  |
       | 3. Click "Sign In"                      |                                  |
       |---------------------------------------->|                                  |
       |                                         |                                  |
       |                                         | 4. Query SusuDao for Identifier  |
       |                                         | 5. Match Exact Password          |
       |                                         | 6. Check Account Status:         |
       |                                         |    - PENDING_APPROVAL -> Reject  |
       |                                         |    - SUSPENDED -> Reject         |
       |                                         |    - ACTIVE -> Proceed           |
       |                                         |                                  |
       |                                         | 7. Emit Active User Session      |
       |                                         |--------------------------------->|
       |                                         |                                  |
       | 8. Render Specific Application View:    |                                  |
       |    - Role "ADMIN"     -> System Admin Dashboard                            |
       |    - Role "ORGANIZER" -> Susu Organizer Dashboard                          |
       |    - Role "MEMBER"    -> Personal Member Dashboard                         |
       |<---------------------------------------------------------------------------|
```

---

### SSD-03: Susu Savings Circle Creation & Member Join Flow
```
Organizer                               System Boundary                          Member
    |                                          |                                    |
    | 1. Create Susu Group                     |                                    |
    |    (Name, Amount, Frequency, Cycles)     |                                    |
    |----------------------------------------->|                                    |
    |                                          | 2. Generate Unique Group Code      |
    |                                          |    (e.g., "MON-SUSU-26")           |
    |                                          | 3. Initialize Cycle 1 (ACTIVE)     |
    |                                          | 4. Record Audit Log Entry          |
    | 5. Display Circle & Shareable Code       |                                    |
    |<-----------------------------------------|                                    |
    |                                          |                                    |
    | 6. Shares Code via SMS / WhatsApp        |                                    |
    |------------------------------------------------------------------------------>|
    |                                          |                                    |
    |                                          | 7. Enter Group Code                |
    |                                          |<-----------------------------------|
    |                                          | 8. Verify Group Code Exists        |
    |                                          | 9. Assign Member Turn Position     |
    |                                          | 10. Record Group Membership in DB  |
    |                                          | 11. Return Circle Dashboard & Turn |
    |                                          |----------------------------------->|
```

---

### SSD-04: Contribution Payment Submission & Organizer Verification Flow
```
Member                                  System Boundary                           Organizer
   |                                           |                                      |
   | 1. Open "Submit Payment" Dialog           |                                      |
   | 2. Select Payment Method:                 |                                      |
   |    - Lonestar MTN Mobile Money            |                                      |
   |    - Orange Money                         |                                      |
   |    - Cash Deposit                         |                                      |
   | 3. Enter Reference (e.g. MTN-839201)      |                                      |
   | 4. Click "Submit Payment"                 |                                      |
   |------------------------------------------>|                                      |
   |                                           | 5. Insert Payment Record             |
   |                                           |    (Status = SUBMITTED)              |
   |                                           | 6. Send Alert to Organizer           |
   |                                           |------------------------------------->|
   | 7. Update Member Contribution Card to     |                                      |
   |    "SUBMITTED - Awaiting Verification"    |                                      |
   |<------------------------------------------|                                      |
   |                                           |                                      |
   |                                           | 8. Organizer inspects Pending Queue  |
   |                                           | 9. Cross-references MoMo SMS / Cash  |
   |                                           | 10. Click "Verify Payment ✓"         |
   |                                           |<-------------------------------------|
   |                                           | 11. Update Payment = "VERIFIED"      |
   |                                           | 12. Generate Digital Receipt Record  |
   |                                           |     (Format: SUSU-2026-XXXXXX)       |
   |                                           | 13. Record Immutable Audit Log       |
   | 14. Deliver Confirmation & Digital Receipt| 14. Update Total Pot Collected       |
   |<------------------------------------------|------------------------------------->|
```

---

### SSD-05: Lump Sum Payout & Cycle Advancement Flow
```
Organizer                               System Boundary                          Recipient Member
    |                                          |                                         |
    | 1. Inspect Active Cycle Status           |                                         |
    |    (Confirm 100% Contributions Settled)  |                                         |
    | 2. Identify Recipient for Active Cycle   |                                         |
    | 3. Click "Release Payout & Advance Cycle"|                                         |
    | 4. Input MoMo/Cash Transfer Reference    |                                         |
    |----------------------------------------->|                                         |
    |                                          | 5. Generate Payout Record               |
    |                                          | 6. Generate Official Payout Receipt     |
    |                                          | 7. Advance Group to Next Cycle (N + 1)  |
    |                                          | 8. Record Immutable Audit Trail Entry   |
    | 9. Display Payout Success & New Cycle    | 10. Send Payout Notification & Receipt  |
    |<-----------------------------------------|---------------------------------------->|
```

---

## 3. Data Entities & State Transition Matrix

### User Account States (`User.status`)
| State | Description | Allowed Actions |
|---|---|---|
| `PENDING_APPROVAL` | Initial state upon Member/Organizer registration | Can only view wait message; cannot sign in |
| `ACTIVE` | Approved by Administrator Prince Gbolon | Full role-specific portal access |
| `SUSPENDED` | Temporarily blocked by Administrator | Access denied until re-activated |
| `REJECTED` | Declined by Administrator | Access denied |

### Payment States (`Payment.status`)
| State | Trigger | Next State |
|---|---|---|
| `SUBMITTED` | Member submits MoMo/Cash transaction reference | `VERIFIED` or `REJECTED` |
| `VERIFIED` | Organizer confirms receipt of funds | Terminal (Generates digital receipt) |
| `REJECTED` | Organizer flags invalid reference or unverified deposit | Member re-submits payment |

---

## 4. Multi-Device Clean Deployment Specifications

1. **Root Admin Seed:** Only `princegbolon3@gmail.com` with password `2004prada22` is provisioned during database creation (`version = 4`).
2. **Zero Pre-Seeded Dummy Data:** All members, groups, and transactions must be organically registered and verified.
3. **Audit Trail Accountability:** Every state change (registration, approval, payment, verification, payout) logs the actor's user ID, name, timestamp, and entity reference.
