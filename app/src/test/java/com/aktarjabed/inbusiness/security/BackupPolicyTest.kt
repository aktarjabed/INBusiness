package com.aktarjabed.inbusiness.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Regression tests for the app's backup/restore policy.
 *
 * The SQLCipher database and the Keystore-backed passphrase cannot be restored onto a
 * different device. Restoring only part of the app state (e.g. the DataStore business
 * identifiers) previously produced an app that pointed at a database it could not open,
 * so automatic backup must stay disabled and every backup domain must stay excluded.
 *
 * These tests read the source files directly; they are skipped if the working directory
 * does not contain the module (e.g. when run from a different root).
 */
class BackupPolicyTest {

    private fun moduleFile(relativePath: String): File? {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
            File(System.getProperty("user.dir") ?: ".", relativePath),
            File(System.getProperty("user.dir") ?: ".", "app/$relativePath")
        )
        return candidates.firstOrNull { it.isFile }
    }

    private fun parse(relativePath: String): Element {
        val file = moduleFile(relativePath)
        assumeTrue("Could not locate $relativePath from ${File(".").absolutePath}", file != null)
        val factory = DocumentBuilderFactory.newInstance().apply {
            // Namespaces are irrelevant here: attributes are matched by their literal
            // `android:` prefixed name, exactly as they appear in the manifest.
            isNamespaceAware = false
        }
        return factory.newDocumentBuilder().parse(file).documentElement
    }

    @Test
    fun automaticBackupIsDisabled() {
        val manifest = parse("src/main/AndroidManifest.xml")
        val application = manifest.getElementsByTagName("application").item(0) as? Element
        assumeTrue(application != null)

        assertEquals(
            "android:allowBackup must stay false: the encrypted database and its " +
                "Keystore-backed passphrase cannot be restored onto another device",
            "false",
            application!!.getAttribute("android:allowBackup")
        )
    }

    @Test
    fun backupRulesExcludeEveryDomain() {
        val root = parse("src/main/res/xml/backup_rules.xml")
        val exclusions = root.getElementsByTagName("exclude")
        assertTrue("Expected at least one exclusion", exclusions.length > 0)

        val excludedAll = mutableSetOf<String>()
        for (i in 0 until exclusions.length) {
            val element = exclusions.item(i) as Element
            if (element.getAttribute("path") == ".") {
                excludedAll.add(element.getAttribute("domain"))
            }
        }
        // Files, databases and preferences are the domains that carry app state.
        listOf("root", "file", "database", "sharedpref").forEach { domain ->
            assertTrue("Domain '$domain' must be excluded from full backup", excludedAll.contains(domain))
        }
    }

    @Test
    fun dataExtractionRulesExcludeCloudBackupAndDeviceTransfer() {
        val root = parse("src/main/res/xml/data_extraction_rules.xml")

        listOf("cloud-backup", "device-transfer").forEach { section ->
            val sections = root.getElementsByTagName(section)
            assertEquals("Expected exactly one <$section> block", 1, sections.length)
            val element = sections.item(0) as Element
            val exclusions = element.getElementsByTagName("exclude")

            val domains = mutableSetOf<String>()
            for (i in 0 until exclusions.length) {
                val exclude = exclusions.item(i) as Element
                if (exclude.getAttribute("path") == ".") {
                    domains.add(exclude.getAttribute("domain"))
                }
            }
            listOf("root", "file", "database", "sharedpref").forEach { domain ->
                assertTrue(
                    "<$section> must exclude domain '$domain'",
                    domains.contains(domain)
                )
            }
        }
    }

    @Test
    fun encryptedPreferencesAndDatabaseStayNamedInTheLegacyRules() {
        val rules = moduleFile("src/main/res/xml/backup_rules.xml")?.readText()
        assumeTrue(rules != null)
        assertTrue(rules!!.contains("inbusiness_secure_prefs.xml"))
        assertTrue(rules.contains("inbusiness_ultra.db"))
    }
}
