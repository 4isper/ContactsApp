package com.m4isper.contactsapp.presentation.settings

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.m4isper.contactsapp.R

/**
 * Биометрия доступна: на устройстве зарегистрированы отпечатки или лицо.
 * Хэш PIN остаётся корнем доверия — биометрия только снимает блокировку UI.
 */
internal fun biometricsAvailable(context: Context): Boolean =
    BiometricManager
        .from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

/**
 * Показывает системный BiometricPrompt и переводит его ответы: успех — в разблокировку,
 * любое другое завершение (отмена, таймаут, ошибка, кнопка «PIN») — на ввод PIN.
 * Неудачные попытки биометрии не расходуют бюджет попыток PIN: у промпта
 * своя блокировка на стороне системы.
 */
internal class BiometricUnlock(
    private val activity: FragmentActivity,
) {
    fun authenticate(
        onUnlock: () -> Unit,
        onPinFallback: () -> Unit,
    ) {
        val prompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlock()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        onPinFallback()
                    }
                },
            )
        val info =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(activity.getString(R.string.app_lock_title))
                .setNegativeButtonText(activity.getString(R.string.app_lock_use_pin))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        prompt.authenticate(info)
    }

    companion object {
        /** Промпт строится только от FragmentActivity; иначе — PIN-экран без биометрии. */
        fun from(activity: Activity?): BiometricUnlock? = (activity as? FragmentActivity)?.let(::BiometricUnlock)
    }
}
