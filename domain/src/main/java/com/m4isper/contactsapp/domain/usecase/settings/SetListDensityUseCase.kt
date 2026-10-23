package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.repository.settings.ListSettings

class SetListDensityUseCase(
    private val repository: ListSettings,
) {
    suspend operator fun invoke(density: ListDensity) = repository.setListDensity(density)
}
