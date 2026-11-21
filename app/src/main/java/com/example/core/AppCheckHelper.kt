package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val VERIFIED_INTENT_TOKEN_FLAG = "lingoworld_verified_intent_debug_token"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

@Volatile
var activeAppCheckMode: String = "uninitialized"
    private set

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                // Synchronous commit() is mandatory before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .putBoolean(VERIFIED_INTENT_TOKEN_FLAG, true)
                    .commit()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                activeAppCheckMode = "debug_intent"
                Log.d(TAG, "App Check debug secret configured from launch Intent.")
            } else if (prefs.getBoolean(VERIFIED_INTENT_TOKEN_FLAG, false)) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                activeAppCheckMode = "debug_stored"
                Log.d(TAG, "DebugAppCheckProviderFactory installed from verified stored token.")
            } else if (!installRecaptchaProvider(context)) {
                Log.w(TAG, "No debug token or reCAPTCHA key found; falling back to DebugAppCheckProviderFactory.")
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                activeAppCheckMode = "debug_fallback"
            }
        } else {
            installRecaptchaProvider(context)
        }
    } catch (e: Exception) {
        activeAppCheckMode = "init_error:${e.javaClass.simpleName}"
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

private fun installRecaptchaProvider(context: Context): Boolean {
    val resId = context.resources.getIdentifier(
        RECAPTCHA_SITE_KEY_RES,
        "string",
        context.packageName
    )
    if (resId == 0) {
        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
        return false
    }
    val siteKey = context.getString(resId)
    if (siteKey.isBlank()) {
        Log.e(TAG, "recaptcha_site_key resource string is blank.")
        return false
    }
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    activeAppCheckMode = "recaptcha"
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
    return true
}

/**
 * Safely warms up the App Check token off the main thread after startup so
 * Firebase Installations never deadlocks MainActivity.onCreate().
 */
suspend fun warmUpAppCheckTokenOffMainThread() {
    withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(12_000L) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
            Log.d(TAG, "App Check token pre-warmed off main thread (mode=$activeAppCheckMode).")
        }.onFailure { err ->
            Log.w(TAG, "App Check background warm-up skipped/failed (mode=$activeAppCheckMode): ${err.localizedMessage}")
        }
    }
}
