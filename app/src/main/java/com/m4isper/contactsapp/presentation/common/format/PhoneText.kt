package com.m4isper.contactsapp.presentation.common.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber

/** Номер с подписью типа: используется в диалогах подтверждения звонка. */
@Composable
internal fun phoneText(phone: PhoneNumber): String =
    stringResource(
        R.string.phone_with_kind,
        phone.number,
        phone.customLabel ?: phoneKindLabel(phone.kind),
    )
