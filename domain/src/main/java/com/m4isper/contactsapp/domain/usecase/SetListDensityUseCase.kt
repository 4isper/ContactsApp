package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

@Inject
class SetListDensityUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(density: ListDensity) = repository.setListDensity(density)
}
