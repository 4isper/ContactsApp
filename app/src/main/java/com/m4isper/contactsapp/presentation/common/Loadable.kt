package com.m4isper.contactsapp.presentation.common

import com.m4isper.contactsapp.domain.model.common.AppError

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data object Empty : Loadable<Nothing>

    data class Content<T>(
        val data: T,
    ) : Loadable<T>

    data class Error(
        val error: AppError?,
    ) : Loadable<Nothing>
}
