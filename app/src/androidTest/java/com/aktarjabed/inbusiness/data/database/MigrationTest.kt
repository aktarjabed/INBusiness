package com.aktarjabed.inbusiness.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    /** Skips (with a precise reason) when Room's schema bundles cannot be deserialized. */
    @get:Rule
    val schemaBundleGuard = SqliteSchemaBundleRule()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )


    @Test
    fun migrate11To12() {
        val db = helper.createDatabase(TEST_DB, 11)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 12, true, AppDatabase.Companion.MIGRATION_11_12)
        migratedDb.close()
    }

    @Test
    fun migrate12To13() {
        val db = helper.createDatabase(TEST_DB, 12)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 13, true, AppDatabase.Companion.MIGRATION_12_13)
        migratedDb.close()
    }

    @Test
    fun migrate11To13() {
        val db = helper.createDatabase(TEST_DB, 11)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 13, true, AppDatabase.Companion.MIGRATION_11_12, AppDatabase.Companion.MIGRATION_12_13)
        migratedDb.close()
    }

}
