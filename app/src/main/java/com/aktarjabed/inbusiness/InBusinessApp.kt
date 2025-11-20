package com.aktarjabed.inbusiness

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * INBusiness Application Class
 *
 * Responsibilities:
 * - Initialize Hilt for dependency injection
 * - Register BouncyCastle security provider for NIC encryption
 * - Configure app-wide settings and logging
 */
@HiltAndroidApp
class InBusinessApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize security providers FIRST (critical for NIC encryption)
        initializeBouncyCastle()

        // Initialize app-wide configurations
        initializeLogging()

        Log.i(TAG, "═══════════════════════════════════════════════════════════")
        Log.i(TAG, "  INBusiness Application Initialized Successfully")
        Log.i(TAG, "  Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Log.i(TAG, "═══════════════════════════════════════════════════════════")
    }

    /**
     * Initialize BouncyCastle security provider
     * CRITICAL: Required for NIC e-invoicing API encryption
     */
    private fun initializeBouncyCastle() {
        try {
            // Remove existing BouncyCastle provider if present to avoid conflicts
            val existingProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            if (existingProvider != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
                Log.d(TAG, "Removed existing BouncyCastle provider")
            }

            // Add BouncyCastle as the FIRST security provider (position 1)
            // This ensures it's used for all cryptographic operations
            val provider = BouncyCastleProvider()
            Security.insertProviderAt(provider, 1)

            Log.i(TAG, "✓ BouncyCastle Security Provider registered successfully")
            Log.d(TAG, "  Provider: ${provider.name}")
            Log.d(TAG, "  Version: ${provider.version}")

            // Verify registration
            val registeredProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            if (registeredProvider != null) {
                Log.d(TAG, "  Position: ${Security.getProviders().indexOf(registeredProvider) + 1}")
            } else {
                Log.e(TAG, "✗ BouncyCastle provider registration verification FAILED")
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ CRITICAL: Failed to initialize BouncyCastle Security Provider", e)
            Log.e(TAG, "  NIC e-invoicing encryption will NOT work without BouncyCastle")
            // In production, you might want to crash the app here if BouncyCastle is critical
            // throw RuntimeException("BouncyCastle initialization failed", e)
        }
    }

    /**
     * Initialize app-wide logging configuration
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            // Debug mode - verbose logging
            Log.d(TAG, "═══════════════════════════════════════════════════════════")
            Log.d(TAG, "  DEBUG MODE ENABLED")
            Log.d(TAG, "═══════════════════════════════════════════════════════════")
            Log.d(TAG, "  Build Type: DEBUG")
            Log.d(TAG, "  NIC Base URL: ${BuildConfig.NIC_BASE_URL}")
            Log.d(TAG, "  NIC Integration: ${if (BuildConfig.ENABLE_NIC) "ENABLED" else "DISABLED"}")
            Log.d(TAG, "  Package: ${BuildConfig.APPLICATION_ID}")
            Log.d(TAG, "═══════════════════════════════════════════════════════════")
        } else {
            // Production mode - minimal logging
            Log.i(TAG, "Production mode - Release build")
            Log.i(TAG, "NIC Integration: ${if (BuildConfig.ENABLE_NIC) "ENABLED" else "DISABLED"}")
        }
    }

    override fun onTerminate() {
        Log.i(TAG, "INBusiness Application terminating")
        super.onTerminate()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "⚠️ Low memory warning received")
        // You could implement memory cleanup here if needed
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "⚠️ Memory pressure: Level $level - App running but memory low")
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "UI hidden - Good time to release cached resources")
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE -> {
                Log.w(TAG, "⚠️ App in background - System memory pressure")
            }
            TRIM_MEMORY_COMPLETE -> {
                Log.e(TAG, "⚠️ CRITICAL: System extremely low on memory")
            }
        }
    }

    companion object {
        private const val TAG = "InBusinessApp"
    }
}