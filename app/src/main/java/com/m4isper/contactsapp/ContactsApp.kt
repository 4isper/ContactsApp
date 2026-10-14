package com.m4isper.contactsapp

import android.app.Application
import com.m4isper.contactsapp.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

class ContactsApp : Application() {

    val appGraph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(this)
    }
}
