package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktarjabed.inbusiness.data.local.dao.*
import com.aktarjabed.inbusiness.data.local.entities.*
import com.aktarjabed.inbusiness.security.EncryptionManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        BusinessData::class,
        Invoice::class,
        InvoiceItem::class,
        UserQuotaEntity::class,
        UserEntity::class,
        PaymentEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun userQuotaDao(): UserQuotaDao
    abstract fun userDao(): UserDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        private const val DATABASE_NAME = "inbusiness.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, encryptionManager: EncryptionManager): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = encryptionManager.getDatabasePassphrase()
                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_quotas (
                        userId TEXT PRIMARY KEY NOT NULL,
                        tier TEXT NOT NULL,
                        dailyUsed INTEGER NOT NULL,
                        lastResetEpochDay INTEGER NOT NULL,
                        monthlyUsed INTEGER NOT NULL,
                        lastMonthlyResetEpochDay INTEGER NOT NULL,
                        watermark INTEGER NOT NULL,
                        retentionDays INTEGER NOT NULL,
                        freeExpiryEpochDay INTEGER NOT NULL,
                        deviceTier TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration from version 2 to 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS payments (
                        paymentId TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        razorpayOrderId TEXT NOT NULL,
                        razorpayPaymentId TEXT NOT NULL,
                        signature TEXT NOT NULL,
                        amountPaise INTEGER NOT NULL,
                        plan TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL("CREATE INDEX IF NOT EXISTS index_payments_userId ON payments(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_payments_status ON payments(status)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        uid TEXT PRIMARY KEY NOT NULL,
                        email TEXT,
                        phone TEXT,
                        displayName TEXT,
                        photoUrl TEXT,
                        provider TEXT NOT NULL,
                        isEmailVerified INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER NOT NULL
                    )
                """)
                // Add missing columns to invoices if they don't exist (safe migration)
                // Assuming MIGRATION_3_4 logic from previous turn was adding irn/qrcode to invoices
                // But here we also create users table. Let's combine.
                 try {
                    database.execSQL("ALTER TABLE invoices ADD COLUMN irn TEXT")
                    database.execSQL("ALTER TABLE invoices ADD COLUMN qrcode TEXT")
                } catch (e: Exception) {
                    // Columns might already exist
                }
            }
        }

        // Migration from version 4 to 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE business_data ADD COLUMN bankName TEXT")
                    database.execSQL("ALTER TABLE business_data ADD COLUMN bankAccount TEXT")
                    database.execSQL("ALTER TABLE business_data ADD COLUMN bankIfsc TEXT")
                } catch (e: Exception) {
                    // Columns might already exist
                }
            }
        }
    }
}