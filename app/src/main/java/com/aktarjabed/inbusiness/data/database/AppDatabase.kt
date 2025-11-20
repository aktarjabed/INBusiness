package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktarjabed.inbusiness.data.converters.Converters
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.security.KeyProvider
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        BusinessData::class,
        Invoice::class,
        InvoiceItem::class
    ],
    version = 2, // Phase 2 version
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        private const val DATABASE_NAME = "inbusiness_ultra.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get encrypted database instance
         *
         * @param context Application context
         * @param keyProvider Provides encryption passphrase
         */
        fun getDatabase(context: Context, keyProvider: KeyProvider): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, keyProvider)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context, keyProvider: KeyProvider): AppDatabase {
            // Get passphrase from KeyProvider
            val passphrase = keyProvider.getDatabasePassphrase()
            val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())

            // Create SQLCipher support factory
            val factory = SupportFactory(passphraseBytes)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory) // Enable encryption
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration() // For development only - remove for production
                .build()
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initialize default data if needed
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
    }
}