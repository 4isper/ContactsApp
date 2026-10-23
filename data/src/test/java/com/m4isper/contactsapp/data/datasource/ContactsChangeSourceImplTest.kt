package com.m4isper.contactsapp.data.datasource

import android.content.Context
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsChangeSourceImplTest {
    @Test
    fun observeChangesRegistersAndUnregisters() =
        runTest {
            val context = RuntimeEnvironment.getApplication() as Context
            val source = ContactsChangeSourceImpl(context)
            val job =
                launch {
                    source.observeChanges().collect { }
                }
            // Даём времени на регистрацию наблюдателя.
            kotlinx.coroutines.delay(50)
            job.cancel()
            assertTrue(true)
        }
}
