package com.aktarjabed.inbusiness.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktarjabed.inbusiness.data.local.dao.*
import com.aktarjabed.inbusiness.data.local.entities.*
import net.zetetic.database.sqlcipher.SupportFactory
import android.content.Context
import com.aktarjabed.inbusiness.data.util.KeyProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Database(
    entities = [
        BusinessData::class,
        Invoice::class,
        InvoiceItem::class,
        UserEntity::class,
        UserQuotaEntity::class,
        PaymentEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun userDao(): UserDao
    abstract fun userQuotaDao(): UserQuotaDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        private const val DATABASE_NAME = "inbusiness_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(@ApplicationContext context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .openHelperFactory(SupportFactory(KeyProvider.getDatabasePassword(context).toCharArray()))
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
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

        // Migration from version 3 to 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE invoices ADD COLUMN irn TEXT")
                database.execSQL("ALTER TABLE invoices ADD COLUMN qrcode TEXT")
            }
        }

        // Migration from version 4 to 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_data ADD COLUMN bankName TEXT")
                database.execSQL("ALTER TABLE business_data ADD COLUMN bankAccount TEXT")
                database.execSQL("ALTER TABLE business_data ADD COLUMN bankIfsc TEXT")
            }
        }
    }
}