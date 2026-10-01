package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.SusuDao
import com.example.data.model.NotificationItem
import com.example.data.model.SusuGroup
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Manages real-time cloud synchronization between multiple devices (Phone A & Phone B)
 * using Firebase Cloud Firestore.
 *
 * Enables:
 * 1. Member/Organizer registers on Phone A -> Pushed to Cloud Firestore.
 * 2. Admin Prince Gbolon on Phone B -> Real-time listener receives pending account and alerts Admin.
 * 3. Admin approves on Phone B -> Status updated to ACTIVE in Cloud Firestore.
 * 4. Member on Phone A -> Cloud status verified, and user logs in immediately!
 */
object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val USERS_COLLECTION = "susu_users"
    private const val GROUPS_COLLECTION = "susu_groups"

    private var firestore: FirebaseFirestore? = null
    private var isInitialized = false
    private var pendingUsersListener: ListenerRegistration? = null

    /**
     * Initializes Firebase safely. Works with google-services.json or programmatic fallback.
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("digital-susu-liberia")
                    .setApplicationId(context.packageName)
                    .setApiKey("AIzaSyD-SusuLiberiaCommunityAppKeyPlaceholder")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Firebase initialized programmatically with project digital-susu-liberia")
            }
            firestore = FirebaseFirestore.getInstance()
            isInitialized = true
            Log.d(TAG, "Firebase Firestore initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}. Offline Room database active.")
        }
    }

    /**
     * Uploads new user registration from Phone A to Cloud Firestore.
     */
    fun syncUserToCloud(user: User, onComplete: ((Boolean) -> Unit)? = null) {
        val db = firestore ?: run {
            onComplete?.invoke(false)
            return
        }

        try {
            val docId = sanitizeKey(if (user.phone.isNotBlank()) user.phone else user.email)
            val userMap = hashMapOf(
                "fullName" to user.fullName,
                "phone" to user.phone,
                "email" to user.email,
                "role" to user.role,
                "password" to user.password,
                "status" to user.status,
                "createdAt" to user.createdAt
            )

            db.collection(USERS_COLLECTION).document(docId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "User ${user.fullName} synced to Cloud Firestore successfully.")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync user to Firestore: ${e.message}")
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncUserToCloud: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * Starts listening in real-time for pending registrations created on other devices.
     * When Phone A registers, Phone B (Admin) catches the event and adds it to local Room.
     */
    fun startRealtimePendingUsersListener(
        dao: SusuDao,
        scope: CoroutineScope
    ) {
        val db = firestore ?: return
        pendingUsersListener?.remove()

        try {
            pendingUsersListener = db.collection(USERS_COLLECTION)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed on susu_users: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documents) {
                                val fullName = doc.getString("fullName") ?: continue
                                val phone = doc.getString("phone") ?: ""
                                val email = doc.getString("email") ?: ""
                                val role = doc.getString("role") ?: "MEMBER"
                                val password = doc.getString("password") ?: "123456"
                                val status = doc.getString("status") ?: "PENDING_APPROVAL"
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                                val existing = dao.getUserByCredentials(phone)
                                    ?: if (email.isNotBlank()) dao.getUserByCredentials(email) else null

                                if (existing == null) {
                                    // New user registered on another device!
                                    val newUser = User(
                                        fullName = fullName,
                                        phone = phone,
                                        email = email,
                                        role = role,
                                        password = password,
                                        status = status,
                                        createdAt = createdAt
                                    )
                                    val newId = dao.insertUser(newUser)

                                    if (status == "PENDING_APPROVAL") {
                                        dao.insertNotification(
                                            NotificationItem(
                                                userId = 1, // Admin Prince Gbolon
                                                title = "New $role Registration!",
                                                message = "$fullName ($phone) registered from another device and is waiting for your approval.",
                                                type = "ALERT"
                                            )
                                        )
                                    }
                                    Log.d(TAG, "Inserted cross-device user $fullName into local Room database (ID: $newId).")
                                } else if (existing.status != status) {
                                    // Status changed in the cloud (e.g. approved by Admin)
                                    dao.updateUser(existing.copy(status = status))
                                    Log.d(TAG, "Updated status for ${existing.fullName} to $status from Cloud.")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting real-time pending listener: ${e.message}")
        }
    }

    /**
     * Admin on Phone B approves the user. Updates the document in Cloud Firestore to ACTIVE.
     */
    fun updateCloudUserStatus(phoneOrEmail: String, newStatus: String, onComplete: ((Boolean) -> Unit)? = null) {
        val db = firestore ?: run {
            onComplete?.invoke(false)
            return
        }

        try {
            val docId = sanitizeKey(phoneOrEmail)
            db.collection(USERS_COLLECTION).document(docId)
                .update("status", newStatus)
                .addOnSuccessListener {
                    Log.d(TAG, "Cloud user status updated to $newStatus for $phoneOrEmail.")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update cloud user status: ${e.message}")
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateCloudUserStatus: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * Checks Cloud Firestore for user status when signing in.
     * If Admin approved on Phone B, Phone A checks the cloud and updates local state.
     */
    suspend fun checkCloudUserStatus(identifier: String): User? = suspendCoroutine { continuation ->
        val db = firestore
        if (db == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        try {
            val docId = sanitizeKey(identifier)
            db.collection(USERS_COLLECTION).document(docId).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val user = User(
                            fullName = doc.getString("fullName") ?: "",
                            phone = doc.getString("phone") ?: "",
                            email = doc.getString("email") ?: "",
                            role = doc.getString("role") ?: "MEMBER",
                            password = doc.getString("password") ?: "123456",
                            status = doc.getString("status") ?: "PENDING_APPROVAL",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                        continuation.resume(user)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    /**
     * Uploads a newly created Susu group to Cloud Firestore.
     */
    fun syncGroupToCloud(group: SusuGroup) {
        val db = firestore ?: return
        try {
            val docId = sanitizeKey(group.groupCode)
            val groupMap = hashMapOf(
                "name" to group.name,
                "groupCode" to group.groupCode,
                "description" to group.description,
                "organizerId" to group.organizerId,
                "contributionAmount" to group.contributionAmount,
                "frequency" to group.frequency,
                "startDate" to group.startDate,
                "numberOfCycles" to group.numberOfCycles,
                "currentCycle" to group.currentCycle,
                "status" to group.status,
                "createdAt" to group.createdAt
            )
            db.collection(GROUPS_COLLECTION).document(docId)
                .set(groupMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Group ${group.name} (${group.groupCode}) synced to Cloud Firestore.")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing group: ${e.message}")
        }
    }

    /**
     * Fetches a group from Cloud Firestore by invite code.
     */
    suspend fun fetchGroupByCodeFromCloud(groupCode: String): SusuGroup? = suspendCoroutine { continuation ->
        val db = firestore
        if (db == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        try {
            val docId = sanitizeKey(groupCode)
            db.collection(GROUPS_COLLECTION).document(docId).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val group = SusuGroup(
                            name = doc.getString("name") ?: "",
                            groupCode = doc.getString("groupCode") ?: groupCode,
                            description = doc.getString("description") ?: "",
                            organizerId = doc.getLong("organizerId") ?: 1L,
                            contributionAmount = doc.getDouble("contributionAmount") ?: 1000.0,
                            frequency = doc.getString("frequency") ?: "WEEKLY",
                            startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                            numberOfCycles = (doc.getLong("numberOfCycles") ?: 10L).toInt(),
                            currentCycle = (doc.getLong("currentCycle") ?: 1L).toInt(),
                            status = doc.getString("status") ?: "ACTIVE",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                        continuation.resume(group)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
    }
}
