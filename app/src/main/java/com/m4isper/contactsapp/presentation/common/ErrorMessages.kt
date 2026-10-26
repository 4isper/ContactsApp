package com.m4isper.contactsapp.presentation.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.common.AppError

/**
 * Текст ошибки для пользователя: один словарь на все экраны, технические
 * подробности в него не попадают.
 */
@StringRes
internal fun AppError.messageRes(): Int =
    when (this) {
        AppError.PERMISSION_DENIED -> R.string.error_permission_denied
        AppError.PROVIDER_FAILED -> R.string.error_provider_failed
        AppError.SAVE_FAILED -> R.string.error_save_failed
        AppError.LOCAL_ACCOUNT_BLOCKED -> R.string.error_local_account_blocked
        AppError.DELETE_FAILED -> R.string.error_delete_failed
        AppError.IMPORT_FAILED -> R.string.error_import_failed
        AppError.SCHEDULE_FAILED -> R.string.error_schedule_failed
        AppError.UNKNOWN -> R.string.error_unknown
    }

/** Текст ошибки для пользователя. */
@Composable
internal fun AppError.message(): String = stringResource(messageRes())
