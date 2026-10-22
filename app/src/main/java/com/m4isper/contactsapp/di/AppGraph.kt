package com.m4isper.contactsapp.di

import android.app.Application
import android.content.Context
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.presentation.reminders.BirthdayReminderScheduler
import com.m4isper.contactsapp.presentation.reminders.ReminderScheduler
import com.m4isper.contactsapp.presentation.settings.AppLanguageStore
import com.m4isper.contactsapp.presentation.settings.SharedPrefsAppLanguageStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(AppScope::class)
interface AppGraph :
    ViewModelGraph,
    UseCaseProviders {
    /**
     * Use cases для точек входа Android: получатель напоминаний и виджет берут
     * их здесь, а не собирают вручную - иначе composition root дублируется.
     */
    val getContacts: GetContactsUseCase
    val getContactDetails: GetContactDetailsUseCase
    val observeSettings: ObserveSettingsUseCase
    val observeFavorites: ObserveFavoritesUseCase

    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    fun provideAppLanguageStore(application: Application): AppLanguageStore = SharedPrefsAppLanguageStore(application)

    @Provides
    fun provideReminderScheduler(application: Application): ReminderScheduler = BirthdayReminderScheduler(application)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): AppGraph
    }
}
