package com.aktarjabed.inbusiness

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class INBusinessApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Timber.d("Firebase initialized")
        } catch (e: Exception) {
            Timber.e(e, "Firebase initialization failed")
        }

        // Initialize Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }

        // Initialize Crashlytics
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
                setCustomKey("version_code", BuildConfig.VERSION_CODE)
                setCustomKey("version_name", BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            Timber.e(e, "Crashlytics initialization failed")
        }

        Timber.i("INBusiness Application started - v${BuildConfig.VERSION_NAME}")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
}

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) return

        try {
            FirebaseCrashlytics.getInstance().apply {
                log("$priority/$tag: $message")
                t?.let { recordException(it) }
            }
        } catch (e: Exception) {
            android.util.Log.e("CrashlyticsTree", "Failed to log", e)
        }
    }
}