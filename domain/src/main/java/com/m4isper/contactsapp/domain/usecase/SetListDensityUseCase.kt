package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.repository.ListSettings
import dev.zacsweers.metro.Inject

@Inject
class SetListDensityUseCase(
    private val repository: ListSettings,
) {
    suspend operator fun invoke(density: ListDensity) = repository.setListDensity(density)
}
