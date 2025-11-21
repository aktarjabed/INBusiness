package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktarjabed.inbusiness.data.dao.*
import com.aktarjabed.inbusiness.data.entities.*
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
    version = 4,
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
                    .addMigrations(MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                instance
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

                database.execSQL("""
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
                """)
            }
        }
    }
}