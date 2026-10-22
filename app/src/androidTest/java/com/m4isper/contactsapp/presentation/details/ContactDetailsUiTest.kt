package com.m4isper.contactsapp.presentation.details

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactEvent
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.SipAddress
import com.m4isper.contactsapp.domain.model.SipKind
import com.m4isper.contactsapp.domain.model.Website
import com.m4isper.contactsapp.domain.model.WebsiteKind
import com.m4isper.contactsapp.domain.usecase.CopyContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.MoveContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ContactDetailsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(
        repository: FakeAndroidRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
    ) = ContactDetailsViewModel(
        GetContactDetailsUseCase(repository),
        GetContactGroupsUseCase(groups),
        ToggleFavoriteUseCase(repository),
        ObserveFavoritesUseCase(repository),
        ExportContactsUseCase(
            getContactDetails = GetContactDetailsUseCase(repository),
            loadContactPhoto = LoadContactPhotoUseCase(repository),
            getGroups = GetGroupsUseCase(groups),
            getGroupMembership = GetGroupMembershipUseCase(groups),
            codec = FakeVCardCodec(),
        ),
        GetContactAccountsUseCase(repository),
        CopyContactToAccountUseCase(repository, repository),
        MoveContactToAccountUseCase(CopyContactToAccountUseCase(repository, repository), repository),
    )

    private fun awaitContent(viewModel: ContactDetailsViewModel) {
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }
    }

    @Test
    fun showsNameAndNumbers() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111", "112"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        // Имя дублируется в топ-баре и шапке — проверяем оба узла.
        compose.onAllNodesWithText("Anna").assertCountEquals(2)
        compose.onNodeWithText("111").assertIsDisplayed()
        compose.onNodeWithText("112").assertIsDisplayed()
    }

    @Test
    fun showsStructuredAddressOrganizationWebsiteAndSip() {
        val address =
            PostalAddress(
                address = "",
                kind = AddressKind.WORK,
                poBox = "PO 5",
                street = "Lenina 1",
                city = "Moscow",
                region = "MO",
                postcode = "101000",
                country = "Russia",
            )
        val details =
            testDetails(7, "Anna", "111").copy(
                addresses = listOf(address),
                websites = listOf(Website("https://blog.example", WebsiteKind.BLOG)),
                sipAddresses = listOf(SipAddress("sip:anna@example.com", SipKind.WORK)),
                organization =
                    Organization(
                        company = "ACME",
                        title = "CTO",
                        department = "Platform",
                        jobDescription = "Leads engineering",
                        officeLocation = "London",
                    ),
            )
        val viewModel = viewModel(FakeAndroidRepository(listOf(details)))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        val addressTag = "address_row_${address.displayAddress}"
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag(addressTag))
        compose.onNodeWithTag(addressTag).assertIsDisplayed()
        compose.onNodeWithText(address.displayAddress).assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("sip_row_sip:anna@example.com"))
        compose.onNodeWithTag("sip_row_sip:anna@example.com").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("website_row_https://blog.example"))
        compose.onNodeWithTag("website_row_https://blog.example").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("organization_row"))
        compose.onNodeWithTag("organization_row").assertIsDisplayed()
        compose.onAllNodesWithText("Platform", substring = true).assertCountEquals(1)
        compose.onAllNodesWithText("Leads engineering", substring = true).assertCountEquals(1)
        compose.onAllNodesWithText("London", substring = true).assertCountEquals(1)
    }

    @Test
    fun showsContactAccount() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val repository =
            FakeAndroidRepository(
                listOf(testDetails(7, "Anna", "111").copy(account = google)),
            )
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("details_account_row").assertIsDisplayed()
        compose.onNodeWithText("user@gmail.com", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsContactGroups() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val groups =
            FakeGroupsRepository(
                groups = listOf(ContactGroup(id = 100, title = "Семья")),
                membership = mapOf(7L to setOf(100L)),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("group_row_Семья").assertIsDisplayed()
    }

    @Test
    fun showsInstantMessageWithServiceLabel() {
        val details =
            testDetails(
                7,
                "Anna",
                instantMessages = listOf(InstantMessage("skype_anna", ImProtocol.SKYPE)),
            )
        val viewModel = viewModel(FakeAndroidRepository(listOf(details)))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithText("skype_anna").assertIsDisplayed()
        compose.onNodeWithText("Skype").assertIsDisplayed()
    }

    @Test
    fun showsNicknameRelationsAndEvents() {
        val details =
            testDetails(
                7,
                "Anna",
                nickname = "Нюша",
                relations = listOf(ContactRelation(name = "Bob", kind = RelationKind.FRIEND)),
                events =
                    listOf(
                        ContactEvent(
                            date = Birthday(day = 1, month = 6, year = 2015),
                            kind = EventKind.ANNIVERSARY,
                        ),
                    ),
            )
        val viewModel = viewModel(FakeAndroidRepository(listOf(details)))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithText("Нюша").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("event_row_2015-06-01"))
        compose.onNodeWithTag("event_row_2015-06-01").assertIsDisplayed()
        compose.onNodeWithText("Anniversary").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("relation_row_Bob"))
        compose.onNodeWithTag("relation_row_Bob").assertIsDisplayed()
        compose.onNodeWithText("Friend").assertIsDisplayed()
    }

    @Test
    fun showsNotFoundForUnknownContact() {
        val viewModel = viewModel(FakeAndroidRepository())
        compose.setContent {
            ContactDetailsScreen(contactId = 42, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Empty
        }

        compose.onNodeWithText("Anna").assertDoesNotExist()
    }

    @Test
    fun phoneTapOpensConfirmDialogAndDismissClosesIt() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("phone_row_111").performClick()
        compose.onNodeWithTag("confirm_call_button").assertIsDisplayed()

        compose.onNodeWithTag("dismiss_call_button").performClick()
        compose.onNodeWithTag("confirm_call_button").assertDoesNotExist()
    }

    @Test
    fun starTogglesFavoriteState() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)
        assertEquals(false, viewModel.uiState.value.isFavorite)

        compose.onNodeWithTag("favorite_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isFavorite
        }

        compose.onNodeWithTag("favorite_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isFavorite
        }
    }

    @Test
    fun backButtonReportsOnBack() {
        val backed = AtomicBoolean(false)
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = { backed.set(true) }, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("back_button").performClick()

        assertEquals(true, backed.get())
    }

    @Test
    fun editButtonReportsOnEdit() {
        val edited = AtomicBoolean(false)
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(
                contactId = 7,
                onBack = {},
                onEdit = { edited.set(true) },
                viewModel = viewModel,
            )
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("edit_button").performClick()

        assertEquals(true, edited.get())
    }
}
