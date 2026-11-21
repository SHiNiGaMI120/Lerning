package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val VERIFIED_INTENT_TOKEN_FLAG = "lingoworld_verified_intent_debug_token"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

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
                Log.d(TAG, "App Check debug secret configured from launch Intent.")
            } else if (prefs.getBoolean(VERIFIED_INTENT_TOKEN_FLAG, false)) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed from verified stored token.")
            } else if (!installRecaptchaProvider(context)) {
                Log.w(TAG, "No debug token or reCAPTCHA key found; falling back to DebugAppCheckProviderFactory.")
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }
        } else {
            installRecaptchaProvider(context)
        }
    } catch (e: Exception) {
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
    val appCheck = FirebaseAppCheck.getInstance()
    appCheck.installAppCheckProviderFactory(
        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    )
    // Pre-warm the reCAPTCHA client and cache an App Check token asynchronously
    // so the user's first AI Tutor call doesn't stall on cold initialization.
    appCheck.getAppCheckToken(false)
        .addOnSuccessListener {
            Log.d(TAG, "RecaptchaAppCheckProviderFactory pre-warmed token successfully.")
        }
        .addOnFailureListener { error ->
            Log.w(TAG, "Recaptcha App Check pre-warm failed: ${error.localizedMessage}")
        }
    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
    return true
}
