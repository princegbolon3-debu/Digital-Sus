# Software Design Document (SDD)
## Digital Susu — Android System Architecture & Technical Design

**Version:** 4.0  
**Date:** October 2026  
**Lead Architect & Administrator:** Prince Gbolon (`princegbolon3@gmail.com`)  
**Tech Stack:** Kotlin 2.0, Jetpack Compose (Material 3), AndroidX Room SQLite (`version = 4`), Kotlin Coroutines & `StateFlow`

---

## 1. Architectural Overview

Digital Susu follows a strict **Clean Model-View-ViewModel (MVVM)** reactive architecture with unidirectional data flow (UDF). All UI components are stateless or state-hoisted Jetpack Compose functions that observe immutable state streams from `SusuViewModel`, which in turn coordinates business rules and multi-table transactions through `SusuRepository` and `SusuDatabase`.

```
+-----------------------------------------------------------------------+
|                        UI LAYER (Jetpack Compose)                     |
|  AuthScreen | AdminDashboardScreen | OrganizerDashboardScreen         |
|  MemberDashboardScreen | TurnsScreen | ReceiptsScreen | ReportsScreen |
+-----------------------------------+-----------------------------------+
                                    |  Observes StateFlow<T> / Emits Events
                                    v
+-----------------------------------------------------------------------+
|                      PRESENTATION LAYER (ViewModel)                   |
|  SusuViewModel (Session State, Group Selection, UI Event Dispatch)    |
+-----------------------------------+-----------------------------------+
                                    |  Suspend Calls & Reactive Flows
                                    v
+-----------------------------------------------------------------------+
|                        DOMAIN & DATA REPOSITORY                       |
|  SusuRepository (Business Logic, Approval Workflows, Cycle Math,      |
|                  Receipt Generation, Immutable Audit Logging)         |
+-----------------+-----------------------------------+-----------------+
                  |                                   |
                  v                                   v
+-----------------------------------+   +-------------------------------+
|    LOCAL PERSISTENCE (Room DB)    |   |   REMOTE GATEWAY (REST/OAuth) |
|  SusuDatabase (SQLite Version 4)  |   |  MobileMoneyGateway           |
|  SusuDao & MemberDao (11 Tables)  |   |  - Lonestar Cell MTN MoMo API |
+-----------------------------------+   |  - Orange Money Liberia API   |
                                        +-------------------------------+
```

---

## 2. Package & Module Structure

```
com.example
├── MainActivity.kt                  # App entry point, navigation shell, top/bottom bars, dialog host
├── data
│   ├── local
│   │   ├── SusuDatabase.kt          # Room Database singleton (v4) & root Admin seed callback
│   │   ├── SusuDao.kt               # Primary DAO covering Users, Groups, Cycles, Payments, Receipts, Audits
│   │   └── MemberDao.kt             # Auxiliary DAO for legacy/member directory operations
│   ├── model
│   │   ├── User.kt                  # @Entity(tableName = "users")
│   │   ├── SusuGroup.kt             # @Entity(tableName = "susu_groups")
│   │   ├── GroupMember.kt           # @Entity(tableName = "group_members")
│   │   ├── ContributionCycle.kt     # @Entity(tableName = "contribution_cycles")
│   │   ├── Contribution.kt          # @Entity(tableName = "contributions")
│   │   ├── Payment.kt               # @Entity(tableName = "payments")
│   │   ├── Payout.kt                # @Entity(tableName = "payouts")
│   │   ├── Receipt.kt               # @Entity(tableName = "receipts")
│   │   ├── NotificationItem.kt      # @Entity(tableName = "notifications")
│   │   ├── AuditLog.kt              # @Entity(tableName = "audit_logs")
│   │   └── Models.kt                # Domain projections (MemberWithPosition, CycleFinancialSummary, etc.)
│   ├── remote
│   │   └── MobileMoneyGateway.kt    # MTN MoMo & Orange Money Liberia OAuth2 / USSD Push client
│   └── repository
│       └── SusuRepository.kt        # Core domain engine & transactional state coordinator
└── ui
    ├── components
    │   ├── SusuComponents.kt        # Reusable cards, StatusBadge, CurrencyText, progress indicators
    │   └── SusuDialogs.kt           # Modals: ApproveAndAssign, CreateGroup, SubmitPayment, Payout, etc.
    ├── screens
    │   ├── AuthScreen.kt
    │   ├── AccountPendingApprovalScreen.kt
    │   ├── AdminDashboardScreen.kt
    │   ├── OrganizerDashboardScreen.kt
    │   ├── MemberDashboardScreen.kt
    │   ├── TurnsScreen.kt
    │   ├── GroupMembersScreen.kt
    │   ├── ReceiptsScreen.kt
    │   ├── ReportsScreen.kt
    │   ├── NotificationsScreen.kt
    │   └── ManageHubScreen.kt
    ├── theme
    │   ├── Color.kt                 # Emerald Green (#006B3F), Susu Gold (#D4AF37), status colors
    │   ├── Theme.kt                 # Material 3 color scheme & typography binding
    │   └── Type.kt
    └── viewmodel
        └── SusuViewModel.kt         # Reactive state holder & coroutine command dispatcher
```

---

## 3. Relational Database Schema Design (`SusuDatabase` v4)

### 3.1 Entity-Relationship Diagram (ERD)

```
+---------------+       1 : N (organizerId)      +-------------------+
|     users     |------------------------------->|    susu_groups    |
+---------------+                                +-------------------+
| PK id         |                                | PK id             |
|    fullName   |       N : M (via group_members)|    name           |
|    phone      |<------------------------------>|    groupCode (UQ) |
|    email      |                                |    organizerId    |
|    role       |                                |    contribAmount  |
|    password   |                                |    frequency      |
|    status     |                                |    numberOfCycles |
+-------+-------+                                |    currentCycle   |
        |                                        +---------+---------+
        | 1 : N                                            | 1 : N
        v                                                  v
+-------------------+                            +------------------------+
|   group_members   |                            |  contribution_cycles   |
+-------------------+                            +------------------------+
| PK id             |                            | PK id                  |
| FK groupId        |                            | FK groupId             |
| FK userId         |                            |    cycleNumber         |
|    payoutPosition |                            |    recipientUserId     |
|    status         |                            |    payoutAmount        |
+-------------------+                            |    status              |
                                                 +-----------+------------+
                                                             | 1 : N
        +----------------------------------------------------+
        |
        v
+-------------------+       1 : N       +--------------------+       1 : 1       +-------------------+
|   contributions   |------------------>|      payments      |------------------>|     receipts      |
+-------------------+                   +--------------------+                   +-------------------+
| PK id             |                   | PK id              |                   | PK id             |
| FK groupId        |                   | FK contributionId  |                   |    receiptNo (UQ) |
| FK cycleId        |                   | FK groupId         |                   | FK paymentId      |
| FK memberUserId   |                   | FK cycleId         |                   | FK groupId        |
|    cycleNumber    |                   | FK memberUserId    |                   | FK memberUserId   |
|    amount         |                   |    amount          |                   |    amount         |
|    status         |                   |    paymentMethod   |                   |    paymentMethod  |
+-------------------+                   |    txReference(UQ) |                   |    txReference    |
                                        |    status          |                   +-------------------+
                                        +--------------------+
```

### 3.2 Table Specifications

1. **`users` (`User.kt`):**
   * `id` (`Long`, PK, AutoGenerate), `fullName` (`String`), `phone` (`String`), `email` (`String`), `role` (`ADMIN` | `ORGANIZER` | `MEMBER`), `password` (`String`), `status` (`ACTIVE` | `PENDING_APPROVAL` | `SUSPENDED` | `REJECTED`), `createdAt` (`Long`).
2. **`susu_groups` (`SusuGroup.kt`):**
   * `id` (`Long`, PK), `name` (`String`), `groupCode` (`String`), `description` (`String`), `organizerId` (`Long`), `contributionAmount` (`Double`), `frequency` (`DAILY` | `WEEKLY` | `BIWEEKLY` | `MONTHLY`), `startDate` (`Long`), `numberOfCycles` (`Int`), `currentCycle` (`Int`), `payoutMethod` (`ROTATION_ORDER`), `status` (`ACTIVE` | `COMPLETED` | `CLOSED`).
3. **`group_members` (`GroupMember.kt`):**
   * `id` (`Long`, PK), `groupId` (`Long`), `userId` (`Long`), `payoutPosition` (`Int`), `status` (`ACTIVE` | `PENDING_APPROVAL` | `REMOVED`), `joinedAt` (`Long`).
   * **Indices:** Unique composite index on `(groupId, userId)`; index on `(groupId, payoutPosition)`.
4. **`contribution_cycles` (`ContributionCycle.kt`):**
   * `id` (`Long`, PK), `groupId` (`Long`), `cycleNumber` (`Int`), `startDate` (`Long`), `endDate` (`Long`), `recipientUserId` (`Long?`), `payoutAmount` (`Double`), `status` (`ACTIVE` | `COMPLETED` | `UPCOMING`).
5. **`contributions` (`Contribution.kt`):**
   * `id` (`Long`, PK), `groupId` (`Long`), `cycleId` (`Long`), `cycleNumber` (`Int`), `memberUserId` (`Long`), `amount` (`Double`), `dueDate` (`Long`), `status` (`UNPAID` | `PENDING` | `PAID` | `LATE`), `paidAt` (`Long?`).
6. **`payments` (`Payment.kt`):**
   * `id` (`Long`, PK), `contributionId` (`Long`), `groupId` (`Long`), `cycleId` (`Long`), `memberUserId` (`Long`), `amount` (`Double`), `paymentMethod` (`String`), `transactionReference` (`String`, Unique Index), `status` (`SUBMITTED` | `VERIFIED` | `REJECTED`), `note` (`String?`), `rejectionReason` (`String?`), `submittedAt` (`Long`), `verifiedAt` (`Long?`), `verifiedByUserId` (`Long?`).
7. **`payouts` (`Payout.kt`):**
   * `id` (`Long`, PK), `groupId` (`Long`), `cycleId` (`Long`), `cycleNumber` (`Int`), `recipientUserId` (`Long`), `amount` (`Double`), `status` (`PENDING` | `APPROVED` | `COMPLETED` | `FAILED`), `paymentMethod` (`String`), `transactionReference` (`String?`), `completedAt` (`Long?`).
8. **`receipts` (`Receipt.kt`):**
   * `id` (`Long`, PK), `receiptNumber` (`String`, Unique Index), `paymentId` (`Long`), `groupId` (`Long`), `groupName` (`String`), `memberUserId` (`Long`), `memberName` (`String`), `amount` (`Double`), `cycleNumber` (`Int`), `paymentMethod` (`String`), `transactionReference` (`String`), `status` (`VERIFIED`), `issuedAt` (`Long`).
9. **`notifications` (`NotificationItem.kt`):**
   * `id` (`Long`, PK), `userId` (`Long`), `title` (`String`), `message` (`String`), `type` (`PAYMENT` | `PAYOUT` | `REMINDER` | `SYSTEM`), `isRead` (`Boolean`), `createdAt` (`Long`).
10. **`audit_logs` (`AuditLog.kt`):**
    * `id` (`Long`, PK), `userId` (`Long`), `userName` (`String`), `action` (`String`), `entityType` (`USER` | `GROUP` | `PAYMENT` | `PAYOUT` | `SYSTEM`), `entityId` (`Long`), `description` (`String`), `timestamp` (`Long`).

---

## 4. Core Component & Algorithm Design

### 4.1 Targeted Account Approval & Group Assignment Algorithm (`SusuRepository.approveUser`)
1. Fetch target `User` by `userId`. Update `User.status = "ACTIVE"` and optionally update `User.role` if promoted to `"ORGANIZER"`.
2. Write `USER_APPROVED` entry to `audit_logs` and insert a welcome `NotificationItem` for the user.
3. If `assignGroupId != null`:
   * Invoke `addUserToGroup(groupId = assignGroupId, userId = userId, roleInGroup = roleInGroup, desiredPosition = payoutPosition, approverId = approverId)`.
   * If `roleInGroup == "ORGANIZER"`, execute `dao.updateGroupOrganizer(assignGroupId, userId)` so the group's `organizerId` points to the newly assigned Organizer.
   * Assign the requested `payoutPosition` (or next sequential slot `maxPosition + 1`), insert `GroupMember`, and initialize a `Contribution` record for the group's active cycle.

### 4.2 Payment Verification & Serialized Receipt Engine (`SusuRepository.verifyPayment`)
1. Validate that the `Payment` exists and is in `SUBMITTED` state.
2. Transition `Payment.status = "VERIFIED"`, recording `verifiedAt = System.currentTimeMillis()` and `verifiedByUserId`.
3. Transition the parent `Contribution.status = "PAID"` and set `paidAt`.
4. Generate a unique receipt number formatted as `"SUSU-2026-%06d".format(payment.id * 100 + (System.currentTimeMillis() % 100))` and insert into `receipts`.
5. Insert `NotificationItem` for the paying member and write `PAYMENT_VERIFIED` to `audit_logs`.

### 4.3 Lump-Sum Payout & Automatic Cycle Rollover (`SusuRepository.completeCyclePayout`)
1. Identify the active `ContributionCycle` for `groupId` and the member holding `payoutPosition == group.currentCycle`.
2. Insert a `Payout` record (`status = "COMPLETED"`) and generate a corresponding `Receipt` (`paymentMethod`, `transactionReference`).
3. Mark the current `ContributionCycle` as `status = "COMPLETED"`.
4. Check if `group.currentCycle < group.numberOfCycles`:
   * **True:** Increment `group.currentCycle += 1`, create the next `ContributionCycle(cycleNumber = nextCycle, status = "ACTIVE")`, and pre-populate `UNPAID` `Contribution` rows for all active `GroupMember`s.
   * **False:** Mark `SusuGroup.status = "COMPLETED"`.
5. Record `PAYOUT_COMPLETED` in `audit_logs` and notify all group members.

---

## 5. Security, Privacy & Fault Tolerance Design

* **Zero Pre-Seeded Vulnerabilities:** Clean initialization (`SusuDatabaseCallback`) seeds only the administrator (`princegbolon3@gmail.com`) and verifies baseline integrity on every launch (`ensureInitialData()`).
* **Role-Based Access Control (RBAC):** UI navigation and repository actions validate the active user's role (`ADMIN`, `ORGANIZER`, `MEMBER`) and group ownership (`group.organizerId == currentUser.id`).
* **Idempotent Operations & Unique Constraints:** Database indices on `(groupId, userId)`, `transactionReference`, and `receiptNumber` guarantee referential integrity and prevent duplicate financial records.
