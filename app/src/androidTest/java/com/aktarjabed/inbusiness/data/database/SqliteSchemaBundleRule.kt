package com.aktarjabed.inbusiness.data.database

import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.reflect.Modifier

/**
 * Reports tests that need Room's exported-schema bundles as *skipped* when the platform cannot
 * deserialize them, instead of failing them as product regressions.
 *
 * Upstream defect (androidx.room 2.8.5, still the latest release): `room-migration` declares
 * `kotlinx-serialization-json:1.8.1`, but its bundled `FieldBundle$$serializer` /
 * `DatabaseBundle$$serializer` bytecode cannot be dispatched through the
 * `GeneratedSerializer` interface the runtime provides - `MigrationTestHelper` therefore throws
 * `AbstractMethodError` on every `createDatabase` / `runMigrationsAndValidate` call. Verified with
 * serialization 1.8.1 (Room's own POM), 1.7.3 and 1.6.3 (Navigation's version) on the instrumented
 * classpath, and identically on the annotation-processor classpath during kapt.
 *
 * The detection is empirical on purpose: a static probe of `Modifier.isAbstract` disagrees with
 * what ART actually throws, so this rule observes the real failure. Anything other than the
 * serialization-bundle `AbstractMethodError` propagates and fails the test as usual.
 */
class SqliteSchemaBundleRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (error: AbstractMethodError) {
                    if (!isSchemaBundleSerializationFailure(error)) throw error
                    assumeTrue("Skipped (upstream Room/kotlinx-serialization incompatibility): $error${runtimeProbe()}", false)
                }
            }
        }

    private fun isSchemaBundleSerializationFailure(error: AbstractMethodError): Boolean {
        val message = error.message ?: return false
        return message.contains("GeneratedSerializer") || message.contains("typeParametersSerializers")
    }

    /** Extra evidence in the skip message: which interface layout the runtime actually provides. */
    private fun runtimeProbe(): String = try {
        val interfaceClass = Class.forName("kotlinx.serialization.internal.GeneratedSerializer")
        val method = interfaceClass.methods.firstOrNull { it.name == "typeParametersSerializers" }
        val source = runCatching {
            interfaceClass.protectionDomain?.codeSource?.location?.toString()
        }.getOrNull() ?: "unknown location"
        val abstract = when {
            method == null -> "absent"
            Modifier.isAbstract(method.modifiers) -> "abstract"
            else -> "concrete"
        }
        " [GeneratedSerializer.typeParametersSerializers=$abstract, loaded from $source]"
    } catch (unused: Throwable) {
        ""
    }
}
