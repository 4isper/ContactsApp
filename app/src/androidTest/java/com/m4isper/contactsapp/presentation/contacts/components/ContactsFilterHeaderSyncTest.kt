package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
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
    fun selectedSyncedAccountHidesStatusUntilDropdownOpened() {
        setHeader(
            selectedAccount = google,
            syncInfos = listOf(AccountSyncInfo(google, AccountSyncStatus.SYNCED, lastSyncTimeMs = null)),
        )

        compose.onNodeWithText("Synced").assertDoesNotExist()

        compose.onNodeWithTag("account_chip").performClick()

        compose.onNodeWithText("Google · Synced").assertIsDisplayed()
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
    fun narrowWidthKeepsFiltersInOneScrollableRow() {
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(240.dp).testTag("narrow_header")) {
                    FilterHeader(
                        showFavoritesOnly = false,
                        onFavoritesOnlyChange = {},
                        accounts = listOf(ContactAccount.Local, google),
                        selectedAccount = google,
                    )
                }
            }
        }

        val headerHeight =
            compose
                .onNodeWithTag("narrow_header")
                .fetchSemanticsNode()
                .boundsInRoot.height
        val chipHeight =
            compose
                .onNodeWithTag("favorites_chip")
                .fetchSemanticsNode()
                .boundsInRoot.height
        assertTrue("Header height: $headerHeight, chip height: $chipHeight", headerHeight < chipHeight * 2)

        compose.onNodeWithTag("account_chip").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun errorAccountShowsWarningAndOffersRetryInDropdown() {
        val retried = AtomicBoolean(false)
        setHeader(
            selectedAccount = google,
            syncInfos = listOf(AccountSyncInfo(google, AccountSyncStatus.ERROR, lastSyncTimeMs = null)),
            onRetrySync = { retried.set(true) },
        )

        compose.onNodeWithContentDescription("Sync error", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("account_sync_retry").assertDoesNotExist()

        compose.onNodeWithTag("account_chip").performClick()
        compose.onNodeWithTag("account_sync_retry_com.google_user@gmail.com").performClick()

        assertTrue(retried.get())
    }
}
