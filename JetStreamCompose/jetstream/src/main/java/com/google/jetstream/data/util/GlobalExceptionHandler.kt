package com.google.jetstream.data.util

import android.content.Context
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

class GlobalExceptionHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 1. Log the crash (Crucial for debugging)
        Log.e("Bulletproof", "CRITICAL CRASH DETECTED: ${throwable.message}", throwable)

        // 2. Here you would typically send this to Firebase Crashlytics or Sentry
        // Example: FirebaseCrashlytics.getInstance().recordException(throwable)

        // 3. Delegate to default handler to let the OS handle the actual process death
        // This ensures the standard "App has stopped" dialog might appear if in foreground,
        // or system logs are written correctly.
        // If you want to suppress the dialog completely, you can skip this and just kill the process.
        // For "smoothness", usually we kill it silently or restart.
        
        // Let's try to kill it cleanly to avoid a stuck state.
        defaultHandler?.uncaughtException(thread, throwable)
        
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}
