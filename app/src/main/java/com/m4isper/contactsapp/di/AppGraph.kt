package com.m4isper.contactsapp.di

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): AppGraph
    }
}
