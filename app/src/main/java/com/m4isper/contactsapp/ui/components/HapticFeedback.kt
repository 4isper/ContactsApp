@file:Suppress("MatchingDeclarationName")
package com.m4isper.contactsapp.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Степень интенсивности тактильного отклика. */
enum class HapticImpact {
    Light,
    Medium,
}

/** Произвести тактический отклик, если устройство поддерживает вибрацию. */
fun performHapticFeedback(context: Context, impact: HapticImpact) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        ?: return
    if (!vibrator.hasVibrator()) return

    val effect = when (impact) {
        HapticImpact.Light -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
        }
        HapticImpact.Medium -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            } else {
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            }
        }
    }
    vibrator.vibrate(effect)
}
