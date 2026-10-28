package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.AccountSyncStatus
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ContactsFilterHeaderSyncTest {
    @get:Rule
    val compose = createComposeRule()

    private val google = ContactAccount(name = "user@gmail.com", type = "com.google")

    private fun setHeader(
        selectedAccount: ContactAccount?,
        syncInfos: List<AccountSyncInfo>,
        onRetrySync: () -> Unit = {},
    ) {
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                FilterHeader(
                    showFavoritesOnly = false,
                    onFavoritesOnlyChange = {},
                    accounts = listOf(ContactAccount.Local, google),
                    selectedAccount = selectedAccount,
                    accountSyncInfos = syncInfos,
                    onRetrySync = onRetrySync,
                )
            }
        }
    }

    @Test
    fun selectedSyncedAccountShowsStatusUnderName() {
        setHeader(
            selectedAccount = google,
            syncInfos = listOf(AccountSyncInfo(google, AccountSyncStatus.SYNCED, lastSyncTimeMs = null)),
        )

        compose.onNodeWithText("Synced").assertIsDisplayed()
    }

    @Test
    fun selectedLocalAccountHasNoSyncStatus() {
        setHeader(
            selectedAccount = ContactAccount.Local,
            syncInfos = listOf(AccountSyncInfo(google, AccountSyncStatus.SYNCED, lastSyncTimeMs = null)),
        )

        compose.onNodeWithText("Synced").assertDoesNotExist()
    }

    @Test
    fun dropdownShowsStatusForEveryAccount() {
        setHeader(
            selectedAccount = null,
            syncInfos = listOf(AccountSyncInfo(google, AccountSyncStatus.SYNCED, lastSyncTimeMs = null)),
        )

        compose.onNodeWithText("Accounts").performClick()

        compose.onNodeWithText("Google · Synced").assertIsDisplayed()
    }

    @Test
    fun errorAccountOffersRetryAction() {
        val retried = AtomicBoolean(false)
        setHeader(
            selectedAccount = google,
            syncInfos = listOf(AccountSyncInfo(google, AccountSyncStatus.ERROR, lastSyncTimeMs = null)),
            onRetrySync = { retried.set(true) },
        )

        compose.onNodeWithTag("account_sync_retry").assertIsDisplayed().performClick()

        assertTrue(retried.get())
    }
}
