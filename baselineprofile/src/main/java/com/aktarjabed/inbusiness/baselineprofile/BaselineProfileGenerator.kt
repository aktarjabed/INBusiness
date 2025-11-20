package com.aktarjabed.inbusiness.baselineprofile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile Generator for INBusiness
 *
 * Generates startup and runtime performance profiles
 * to optimize app launch time and critical user journeys.
 *
 * Run with: ./gradlew :baselineprofile:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 28)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @RequiresApi(Build.VERSION_CODES.P)
    @Test
    fun generate() {
        rule.collect(
            packageName = "com.aktarjabed.inbusiness",
            maxIterations = 15,
            stableIterations = 3
        ) {
            // ═══════════════════════════════════════════════════════════════
            // Startup Profile
            // ═══════════════════════════════════════════════════════════════
            pressHome()
            startActivityAndWait()

            // Wait for app to fully load
            device.waitForIdle()

            // ═══════════════════════════════════════════════════════════════
            // Critical User Journey 1: Dashboard View
            // ═══════════════════════════════════════════════════════════════
            device.waitForIdle()

            // ═══════════════════════════════════════════════════════════════
            // Critical User Journey 2: Create Invoice Flow
            // ═══════════════════════════════════════════════════════════════
            // Navigate to create invoice
            device.findObject(androidx.test.uiautomator.By.text("Create Invoice"))?.click()
            device.waitForIdle()

            // Fill invoice details
            Thread.sleep(1000)

            // Navigate back
            device.pressBack()
            device.waitForIdle()

            // ═══════════════════════════════════════════════════════════════
            // Critical User Journey 3: Calculator
            // ═══════════════════════════════════════════════════════════════
            device.findObject(androidx.test.uiautomator.By.text("Calculator"))?.click()
            device.waitForIdle()
            Thread.sleep(500)

            device.pressBack()
            device.waitForIdle()

            // ═══════════════════════════════════════════════════════════════
            // Critical User Journey 4: Invoice List
            // ═══════════════════════════════════════════════════════════════
            device.findObject(androidx.test.uiautomator.By.text("Invoices"))?.click()
            device.waitForIdle()
            Thread.sleep(500)

            device.pressBack()
            device.waitForIdle()
        }
    }
}