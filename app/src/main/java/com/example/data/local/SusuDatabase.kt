package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.ContributionCycle
import com.example.data.model.GroupMember
import com.example.data.model.Member
import com.example.data.model.NotificationItem
import com.example.data.model.Payment
import com.example.data.model.Payout
import com.example.data.model.Receipt
import com.example.data.model.SusuGroup
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        SusuGroup::class,
        GroupMember::class,
        Member::class,
        ContributionCycle::class,
        Contribution::class,
        Payment::class,
        Payout::class,
        Receipt::class,
        NotificationItem::class,
        AuditLog::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SusuDatabase : RoomDatabase() {
    abstract fun susuDao(): SusuDao
    abstract fun memberDao(): MemberDao

    companion object {
        @Volatile
        private var INSTANCE: SusuDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): SusuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SusuDatabase::class.java,
                    "susu_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(SusuDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SusuDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.susuDao(), database.memberDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: SusuDao, memberDao: MemberDao) {
            // ONLY the System Administrator account with the credentials specified:
            // Email: princegbolon3@gmail.com
            // Password: 2004prada22
            val adminUser = User(
                id = 1,
                fullName = "Prince Gbolon",
                phone = "+231 77 000 0001",
                email = "princegbolon3@gmail.com",
                role = "ADMIN",
                password = "2004prada22",
                status = "ACTIVE"
            )
            dao.insertUser(adminUser)

            // Initial System Audit Log
            dao.insertAuditLog(
                AuditLog(
                    id = 1,
                    userId = 1,
                    userName = "Prince Gbolon",
                    action = "SYSTEM_INITIALIZED",
                    entityType = "SYSTEM",
                    entityId = 1,
                    description = "Digital Susu platform initialized. System Administrator provisioned. Clean environment ready for user registration and administrative approval."
                )
            )
        }
    }
}
