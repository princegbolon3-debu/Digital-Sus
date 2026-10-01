package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SusuDatabase
import com.example.data.model.Member
import com.example.data.repository.MemberRepository
import com.example.data.repository.OperationResult
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing Group Member CRUD operations in the Susu association.
 * Exposes reactive StateFlow streams for member listings, selection, and search,
 * and coroutine-based functions for adding, updating, viewing, and deleting members.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemberViewModel(application: Application) : AndroidViewModel(application) {

    private val memberRepository: MemberRepository

    init {
        val database = SusuDatabase.getDatabase(application, viewModelScope)
        memberRepository = MemberRepository(database.memberDao())
    }

    // Currently focused group ID (default 1 for West Point Susu)
    private val _currentGroupId = MutableStateFlow(1L)
    val currentGroupId: StateFlow<Long> = _currentGroupId.asStateFlow()

    // Live search query for member filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected member for detail view or editing
    private val _selectedMember = MutableStateFlow<Member?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMember.asStateFlow()

    // Loading indicator state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Transient UI feedback (snackbar / alerts)
    private val _uiFeedback = MutableSharedFlow<UiFeedback>()
    val uiFeedback: SharedFlow<UiFeedback> = _uiFeedback.asSharedFlow()

    // Reactive list of members, automatically updating when group or search changes
    val members: StateFlow<List<Member>> = combine(
        _currentGroupId,
        _searchQuery
    ) { groupId, query ->
        Pair(groupId, query)
    }.flatMapLatest { (groupId, query) ->
        memberRepository.searchMembers(groupId, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live count of members in current group
    val memberCount: StateFlow<Int> = _currentGroupId.flatMapLatest { groupId ->
        memberRepository.getMemberCount(groupId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setGroupId(groupId: Long) {
        _currentGroupId.value = groupId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMember(member: Member?) {
        _selectedMember.value = member
    }

    fun selectMemberById(memberId: Long) {
        viewModelScope.launch {
            val member = memberRepository.getMemberByIdDirect(memberId)
            _selectedMember.value = member
        }
    }

    /**
     * CRUD: Add a new member to the group
     */
    fun addMember(
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
        viewModelScope.launch {
            _isLoading.value = true
            val newMember = Member(
                groupId = _currentGroupId.value,
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
                    _isLoading.value = false
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member added successfully!"))
                    onSuccess(result.data)
                }
                is OperationResult.Error -> {
                    _isLoading.value = false
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    /**
     * CRUD: Update existing member information
     */
    fun updateMember(member: Member, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = memberRepository.updateMember(member)) {
                is OperationResult.Success -> {
                    _isLoading.value = false
                    if (_selectedMember.value?.id == member.id) {
                        _selectedMember.value = member
                    }
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member updated successfully!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _isLoading.value = false
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    /**
     * CRUD: Delete/remove a member from the group
     */
    fun deleteMember(memberId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = memberRepository.deleteMember(memberId)) {
                is OperationResult.Success -> {
                    _isLoading.value = false
                    if (_selectedMember.value?.id == memberId) {
                        _selectedMember.value = null
                    }
                    _uiFeedback.emit(UiFeedback(result.message ?: "Member deleted successfully!"))
                    onSuccess()
                }
                is OperationResult.Error -> {
                    _isLoading.value = false
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }

    /**
     * Reorder payout turn position
     */
    fun updateMemberPosition(memberId: Long, newPosition: Int) {
        viewModelScope.launch {
            when (val result = memberRepository.updateMemberPosition(_currentGroupId.value, memberId, newPosition)) {
                is OperationResult.Success -> {
                    _uiFeedback.emit(UiFeedback(result.message ?: "Position updated!"))
                }
                is OperationResult.Error -> {
                    _uiFeedback.emit(UiFeedback(result.message, isError = true))
                }
            }
        }
    }
}
