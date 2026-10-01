package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SusuDatabase
import com.example.data.model.AuditLog
import com.example.data.model.ContributionCycle
import com.example.data.model.CycleFinancialSummary
import com.example.data.model.Member
import com.example.data.model.MemberWithPosition
import com.example.data.model.NotificationItem
import com.example.data.model.Payment
import com.example.data.model.Payout
import com.example.data.model.Receipt
import com.example.data.model.SusuGroup
import com.example.data.model.User
import com.example.data.repository.MemberRepository
import com.example.data.repository.OperationResult
import com.example.data.repository.SusuRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiFeedback(
    val message: String,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SusuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SusuRepository
    private val memberRepository: MemberRepository

    init {
        val database = SusuDatabase.getDatabase(application, viewModelScope)
        repository = SusuRepository(database.susuDao())
        memberRepository = MemberRepository(database.memberDao())

        // Initialize Firebase Cross-Device Sync & Real-time Listener for Prince Gbolon Admin
        com.example.data.remote.FirebaseSyncManager.initialize(application)
        com.example.data.remote.FirebaseSyncManager.startRealtimePendingUsersListener(database.susuDao(), viewModelScope)
    }

    // Active logged-in user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Currently selected group
    private val _selectedGroup = MutableStateFlow<SusuGroup?>(null)
    val selectedGroup: StateFlow<SusuGroup?> = _selectedGroup.asStateFlow()

    // Selected cycle number within current group
    private val _selectedCycleNumber = MutableStateFlow(1)
    val selectedCycleNumber: StateFlow<Int> = _selectedCycleNumber.asStateFlow()

    // Cycle financial summary
    private val _cycleSummary = MutableStateFlow<CycleFinancialSummary?>(null)
    val cycleSummary: StateFlow<CycleFinancialSummary?> = _cycleSummary.asStateFlow()

    // UI Feedback (Toast / SnackBar message)
    private val _uiFeedback = MutableSharedFlow<UiFeedback>()
    val uiFeedback: SharedFlow<UiFeedback> = _uiFeedback.asSharedFlow()

    val membersList: StateFlow<List<Member>> = _selectedGroup.flatMapLatest { group ->
        if (group != null) memberRepository.getMembersForGroup(group.id) else memberRepository.allMembers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<SusuGroup>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceipts: StateFlow<List<Receipt>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingUsers: StateFlow<List<User>> = repository.pendingUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically select first group when available
        viewModelScope.launch {
            repository.allGroups.collect { groups ->
                if (_selectedGroup.value == null && groups.isNotEmpty()) {
                    val group = groups.first()
                    _selectedGroup.value = group
                    _selectedCycleNumber.value = group.currentCycle
                    refreshCycleSummary(group.id, group.currentCycle)
                }
            }
        }
    }

    // Active user notifications
    val userNotifications: StateFlow<List<NotificationItem>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getNotificationsForUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active user receipts
    val userReceipts: StateFlow<List<Receipt>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getReceiptsForUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active user payment submissions
    val userPayments: StateFlow<List<Payment>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getPaymentsForUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group cycles
    val groupCycles: StateFlow<List<ContributionCycle>> = _selectedGroup.flatMapLatest { group ->
        if (group != null) repository.getCyclesForGroup(group.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group payouts
    val groupPayouts: StateFlow<List<Payout>> = _selectedGroup.flatMapLatest { group ->
        if (group != null) repository.getPayoutsForGroup(group.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group pending payments awaiting verification
    val pendingPayments: StateFlow<List<Payment>> = _selectedGroup.flatMapLatest { group ->
        if (group != null) repository.getPendingPayments(group.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Members list with their position and current cycle status
    val membersWithDetails: StateFlow<List<MemberWithPosition>> = combine(
        _selectedGroup,
        _selectedCycleNumber
    ) { group, cycleNum ->
        Pair(group, cycleNum)
    }.flatMapLatest { (group, cycleNum) ->
        if (group != null) {
            repository.getMembersWithDetails(group.id, cycleNum)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchUser(user: User) {
        _currentUser.value = user
        viewModelScope.launch {
            _uiFeedback.emit(UiFeedback("Switched user to ${user.fullName} (${user.role})"))
        }
    }

    fun selectGroup(group: SusuGroup) {
        _selectedGroup.value = group
        _selectedCycleNumber.value = group.currentCycle
        refreshCycleSummary(group.id, group.currentCycle)
    }

    fun selectCycleNumber(cycleNum: Int) {
        _selectedCycleNumber.value = cycleNum
        _selectedGroup.value?.let { group ->
            refreshCycleSummary(group.id, cycleNum)
        }
    }

    fun refreshCycleSummary(groupId: Long, cycleNumber: Int) {
        viewModelScope.launch {
            val summary = repository.getCycleSummary(groupId, cycleNumber)
            _cycleSummary.value = summary
        }
    }

    // Submit a payment
    fun submitPayment(
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        note: String?,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        val group = _selectedGroup.value ?: return
        val cycles = groupCycles.value
        val activeCycle = cycles.find { it.cycleNumber == _selectedCycleNumber.value }
            ?: cycles.find { it.status == "ACTIVE" }
            ?: return

        viewModelScope.launch {
            when (val result = repository.submitPayment(
                memberUserId = user.id,
                groupId = group.id,
                cycleId = activeCycle.id,
                amount = amount,
                paymentMethod = paymentMethod,
                reference = transactionRef,
                note = note
            )) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Payment submitted!"))
                    refreshCycleSummary(group.id, _selectedCycleNumber.value)
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Process automated payment through MTN MoMo or Orange Money Developer API
    fun processMobileMoneyPayment(
        amount: Double,
        paymentMethod: String,
        phoneNumber: String,
        note: String?,
        onSuccess: (String) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        val group = _selectedGroup.value ?: return
        val cycles = groupCycles.value
        val activeCycle = cycles.find { it.cycleNumber == _selectedCycleNumber.value }
            ?: cycles.find { it.status == "ACTIVE" }
            ?: return

        viewModelScope.launch {
            _uiFeedback.emit(UiFeedback("Contacting $paymentMethod Gateway for $phoneNumber..."))
            val result = if (paymentMethod.contains("Orange", ignoreCase = true)) {
                com.example.data.remote.MobileMoneyGateway.processOrangeMoneyPayment(phoneNumber, amount)
            } else {
                com.example.data.remote.MobileMoneyGateway.processMtnMoMoPayment(phoneNumber, amount)
            }

            if (result.isSuccess) {
                when (val repoResult = repository.submitPayment(
                    memberUserId = user.id,
                    groupId = group.id,
                    cycleId = activeCycle.id,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    reference = result.transactionReference,
                    note = (note ?: "") + " [API Verified: ${result.transactionReference}]"
                )) {
                    is OperationResult.Success -> {
                        _uiFeedback.emit(UiFeedback("Mobile Money Paid! Ref: ${result.transactionReference}"))
                        refreshCycleSummary(group.id, _selectedCycleNumber.value)
                        onSuccess(result.transactionReference)
                    }
                    is OperationResult.Error -> {
                        _uiFeedback.emit(UiFeedback(repoResult.message, isError = true))
                    }
                }
            } else {
                _uiFeedback.emit(UiFeedback(result.message, isError = true))
            }
        }
    }

    // Verify or Reject payment
    fun verifyPayment(paymentId: Long, isApproved: Boolean, rejectionReason: String? = null) {
        val organizer = _currentUser.value ?: return
        val group = _selectedGroup.value ?: return

        viewModelScope.launch {
            when (val result = repository.verifyPayment(
                paymentId = paymentId,
                organizerUserId = organizer.id,
                isApproved = isApproved,
                rejectionReason = rejectionReason
            )) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Payment updated!"))
                    refreshCycleSummary(group.id, _selectedCycleNumber.value)
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Complete payout and advance cycle
    fun completePayout(transactionRef: String, notes: String?, onSuccess: () -> Unit = {}) {
        val organizer = _currentUser.value ?: return
        val group = _selectedGroup.value ?: return
        val cycles = groupCycles.value
        val cycle = cycles.find { it.cycleNumber == _selectedCycleNumber.value } ?: return

        viewModelScope.launch {
            when (val result = repository.completePayoutAndAdvanceCycle(
                groupId = group.id,
                cycleId = cycle.id,
                organizerUserId = organizer.id,
                transactionRef = transactionRef,
                notes = notes
            )) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Payout completed!"))
                    // Update selected group
                    val updated = repository.getGroup(group.id)
                    updated.collect { g ->
                        if (g != null) {
                            _selectedGroup.value = g
                            _selectedCycleNumber.value = g.currentCycle
                            refreshCycleSummary(g.id, g.currentCycle)
                        }
                    }
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Create group
    fun createGroup(
        name: String,
        code: String,
        description: String,
        contributionAmount: Double,
        frequency: String,
        numberOfCycles: Int,
        onSuccess: (Long) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.createGroup(
                name = name,
                code = code,
                description = description,
                organizerId = user.id,
                contributionAmount = contributionAmount,
                frequency = frequency,
                numberOfCycles = numberOfCycles
            )) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Group created!"))
                    onSuccess(result.data)
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Create a new contribution cycle
    fun createCycle(
        cycleNumber: Int,
        daysDue: Int = 7,
        status: String = "ACTIVE",
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        val group = _selectedGroup.value ?: return
        val now = System.currentTimeMillis()
        val dueDate = now + (daysDue * 24L * 60L * 60L * 1000L)

        viewModelScope.launch {
            when (val result = repository.createCycle(
                groupId = group.id,
                cycleNumber = cycleNumber,
                startDate = now,
                dueDate = dueDate,
                status = status,
                organizerUserId = user.id
            )) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Cycle $cycleNumber created!"))
                    _selectedCycleNumber.value = cycleNumber
                    refreshCycleSummary(group.id, cycleNumber)
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Join group via code
    fun joinGroup(groupCode: String, onSuccess: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.joinGroup(groupCode, user.id)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Joined group!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Update rotation order
    fun updateMemberPosition(memberId: Long, newPos: Int) {
        val organizer = _currentUser.value ?: return
        val group = _selectedGroup.value ?: return
        viewModelScope.launch {
            when (val result = repository.updateMemberPosition(group.id, memberId, newPos, organizer.id)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback("Rotation position updated!"))
                    refreshCycleSummary(group.id, _selectedCycleNumber.value)
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Login authentication
    fun login(identifier: String, password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.login(identifier, password)) {
                is OperationResult.Success -> {
                    _currentUser.value = result.data
                    _uiFeedback.emit(UiFeedback("Signed in as ${result.data.fullName}"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Logout
    fun logout() {
        _currentUser.value = null
        viewModelScope.launch {
            _uiFeedback.emit(UiFeedback("Signed out"))
        }
    }

    // Refresh current user status (e.g. check if approved)
    fun refreshCurrentUser() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.getUser(user.id).collect { updated ->
                if (updated != null) {
                    _currentUser.value = updated
                }
            }
        }
    }

    // Approve member account
    fun approveUser(userId: Long) {
        val approver = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.approveUser(userId, approver.id)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member account approved!"))
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Reject member account
    fun rejectUser(userId: Long, reason: String? = null) {
        val approver = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.rejectUser(userId, approver.id, reason)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Application rejected."))
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Suspend or Activate user
    fun suspendUser(userId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.suspendUser(userId, admin.id)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "User status updated."))
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Delete user
    fun deleteUser(userId: Long) {
        val admin = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.deleteUser(userId, admin.id)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "User removed."))
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Register user
    fun registerUser(
        fullName: String,
        phone: String,
        email: String,
        role: String,
        password: String = "123456",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = repository.registerUser(fullName, phone, email, role, password)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "User registered!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    // Update user profile information
    fun updateProfile(
        fullName: String,
        phone: String,
        email: String,
        newPassword: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val result = repository.updateProfile(user.id, fullName, phone, email, newPassword)) {
                is OperationResult.Success -> {
                    _currentUser.value = result.data
                    _uiFeedback.emit(UiFeedback(result.message ?: "Profile updated!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(user.id)
            _uiFeedback.emit(UiFeedback("All notifications marked as read"))
        }
    }

    // --- Member CRUD Operations ---
    fun addGroupMember(
        fullName: String,
        phone: String,
        email: String = "",
        payoutPosition: Int = 0,
        status: String = "ACTIVE",
        role: String = "MEMBER",
        emergencyContact: String? = null,
        notes: String? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        val group = _selectedGroup.value ?: return
        viewModelScope.launch {
            val newMember = Member(
                groupId = group.id,
                fullName = fullName,
                phone = phone,
                email = email,
                payoutPosition = payoutPosition,
                status = status,
                role = role,
                emergencyContact = emergencyContact,
                notes = notes
            )
            when (val result = memberRepository.addMember(newMember)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member added successfully!"))
                    onSuccess(result.data)
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    fun updateGroupMember(member: Member, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = memberRepository.updateMember(member)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member updated successfully!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    fun deleteGroupMember(memberId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = memberRepository.deleteMember(memberId)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member removed from group!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    fun updateMemberTurnPosition(memberId: Long, newPosition: Int) {
        val group = _selectedGroup.value ?: return
        viewModelScope.launch {
            when (val result = memberRepository.updateMemberPosition(group.id, memberId, newPosition)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Turn position updated!"))
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }
}
