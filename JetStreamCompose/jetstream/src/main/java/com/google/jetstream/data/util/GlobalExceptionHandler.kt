/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
