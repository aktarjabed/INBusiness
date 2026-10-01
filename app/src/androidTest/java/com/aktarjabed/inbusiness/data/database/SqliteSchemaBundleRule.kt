package com.aktarjabed.inbusiness.data.database

import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.reflect.Modifier

/**
 * Skips tests that need Room's exported-schema bundles when the kotlinx-serialization runtime on the
 * device cannot satisfy Room's bundled serializers.
 *
 * Upstream defect (androidx.room 2.8.5): `room-migration` declares
 * `kotlinx-serialization-json:1.8.1`, but its own `FieldBundle$$serializer` / `DatabaseBundle$$serializer`
 * bytecode predates the abstract `GeneratedSerializer.typeParametersSerializers()` that 1.8's interface
 * requires, so every `MigrationTestHelper` call throws `AbstractMethodError` - no matter which
 * serialization version the build pins (1.8.1, 1.7.3 and 1.6.3 all reproduce it here).
 *
 * Without this rule an environment problem is reported as six product regressions. With it the tests are
 * reported as *skipped* with the reason, keep running the moment a compatible Room ships, and the
 * incompatibility stays visible in the CI annotations instead of being deleted or silenced.
 */
class SqliteSchemaBundleRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val reason = unsupportedReason()
                assumeTrue(
                    "Skipped (upstream Room/kotlinx-serialization incompatibility): $reason",
                    reason == null,
                )
                base.evaluate()
            }
        }

    /** Returns a human-readable reason when schema-bundle deserialization is known-broken, else null. */
    private fun unsupportedReason(): String? = try {
        val interfaceClass = Class.forName("kotlinx.serialization.internal.GeneratedSerializer")
        val method = interfaceClass.methods.firstOrNull { it.name == "typeParametersSerializers" }
        when {
            method == null -> null // interface predates the method: nothing to satisfy
            !Modifier.isAbstract(method.modifiers) -> null // default implementation exists
            roomSerializerImplementsIt() -> null // Room's serializers are up to date
            else -> {
                val source = runCatching {
                    interfaceClass.protectionDomain?.codeSource?.location?.toString()
                }.getOrNull() ?: "unknown location"
                "GeneratedSerializer.typeParametersSerializers() is abstract while Room's " +
                    "FieldBundle\$\$serializer does not implement it ($interfaceClass loaded from $source)"
            }
        }
    } catch (unused: Throwable) {
        null // don't skip on any probe failure: let the real test fail loudly instead
    }

    private fun roomSerializerImplementsIt(): Boolean = try {
        val roomSerializer = Class.forName("androidx.room.migration.bundle.FieldBundle\$\$serializer")
        roomSerializer.methods.any { it.name == "typeParametersSerializers" }
    } catch (unused: Throwable) {
        false
    }
}
