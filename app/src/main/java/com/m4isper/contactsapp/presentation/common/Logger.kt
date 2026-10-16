package com.m4isper.contactsapp.presentation.common

import android.util.Log
import com.m4isper.contactsapp.BuildConfig

internal inline fun logd(
    tag: String,
    message: () -> String,
) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, message())
    }
}

internal fun logw(
    tag: String,
    message: String,
    error: Throwable,
) {
    if (BuildConfig.DEBUG) {
        Log.w(tag, message, error)
    }
}
