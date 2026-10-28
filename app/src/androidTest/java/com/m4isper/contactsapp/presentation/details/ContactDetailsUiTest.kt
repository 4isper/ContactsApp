package com.m4isper.contactsapp.presentation.details

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactEvent
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.ContactRelation
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import com.m4isper.contactsapp.domain.model.contact.InstantMessage
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.contact.PostalAddress
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.domain.model.contact.SipAddress
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.Website
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import com.m4isper.contactsapp.domain.usecase.contacts.CopyContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.MoveContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ToggleFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
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

    // Переключение избранного идёт через гейт права на запись.
    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    private fun viewModel(
        repository: FakeAndroidRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
        codec: FakeVCardCodec = FakeVCardCodec(),
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
            codec = codec,
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

        // Пока шапка видна, имя живёт только в ней; топ-бар подхватит его после скролла.
        compose.onAllNodesWithText("Anna").assertCountEquals(1)
        compose.onNodeWithText("111").assertIsDisplayed()
        compose.onNodeWithText("112").assertIsDisplayed()
    }

    @Test
    fun titleAppearsInTheBarOnlyAfterTheHeroScrollsAway() {
        // Контент должен быть выше шапки, иначе карточку нельзя прокрутить до конца.
        val details =
            testDetails(
                7,
                "Anna",
                "111",
                "112",
                emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
                relations = listOf(ContactRelation(name = "Bob", kind = RelationKind.FRIEND)),
            ).copy(
                addresses = listOf(PostalAddress(address = "Lenina 1", kind = AddressKind.HOME)),
                websites = listOf(Website("https://blog.example", WebsiteKind.BLOG)),
                note = "Заметка, которая уводит шапку за пределы экрана.",
            )
        val viewModel = viewModel(FakeAndroidRepository(listOf(details)))
        compose.setContent {
            Box(modifier = Modifier.height(240.dp)) {
                ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
            }
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("details_title").assertDoesNotExist()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("note_row"))

        compose.onNodeWithTag("details_title").assertIsDisplayed()
        compose.onAllNodesWithText("Anna").assertCountEquals(1)
    }

    @Test
    fun titleAppearsEvenWhenTheListCannotScrollTheHeroAway() {
        // У короткого контакта шапка не уезжает целиком: имя должно перейти в топ-бар,
        // а не оставить экран с полосой без имени.
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            Box(modifier = Modifier.height(240.dp)) {
                ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
            }
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("details_title").assertDoesNotExist()
        compose.onNodeWithTag("details_list").performScrollToKey("phone_111")

        compose.onNodeWithTag("details_title").assertIsDisplayed()
    }

    @Test
    fun heroOffersCallSmsEmailAndFavorite() {
        val details =
            testDetails(
                7,
                "Anna",
                "111",
                emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
            )
        val viewModel = viewModel(FakeAndroidRepository(listOf(details)))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("hero_call").assertIsDisplayed()
        compose.onNodeWithTag("hero_sms").assertIsDisplayed()
        compose.onNodeWithTag("hero_email").assertIsDisplayed()
        compose.onNodeWithTag("favorite_button").assertIsDisplayed()
    }

    @Test
    fun groupsFieldsIntoSections() {
        val address =
            PostalAddress(
                address = "",
                kind = AddressKind.HOME,
                street = "Lenina 1",
                city = "Moscow",
            )
        val details =
            testDetails(
                7,
                "Anna",
                "111",
                emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
                birthday = Birthday(day = 1, month = 6, year = 2000),
            ).copy(
                addresses = listOf(address),
                note = "Call after 6",
            )
        val viewModel = viewModel(FakeAndroidRepository(listOf(details)))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("details_section_phones").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("details_section_email"))
        compose.onNodeWithTag("details_section_email").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("details_section_addresses"))
        compose.onNodeWithTag("details_section_addresses").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("details_section_dates"))
        compose.onNodeWithTag("details_section_dates").assertIsDisplayed()
        compose.onNodeWithTag("details_list").performScrollToNode(hasTestTag("details_section_note"))
        compose.onNodeWithTag("details_section_note").assertIsDisplayed()
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
    fun editIsReachableFromTheOverflow() {
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

        compose.onNodeWithTag("details_more_button").performClick()
        compose.onNodeWithTag("edit_menu_item").performClick()

        assertEquals(true, edited.get())
    }

    @Test
    fun showQrMenuItemOpensPreviewAndCloseDismissesIt() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("details_more_button").performClick()
        compose.onNodeWithTag("show_qr_menu_item").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("qr_preview_image").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("qr_preview_dialog").assertIsDisplayed()
        compose.onNodeWithText("Point another phone's camera at this code to add the contact").assertIsDisplayed()

        compose.onNodeWithTag("qr_preview_close").performClick()

        compose.onNodeWithTag("qr_preview_dialog").assertDoesNotExist()
    }

    @Test
    fun showQrDoesNotOpenPreviewWhenVCardIsBlank() {
        val viewModel =
            viewModel(
                FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))),
                codec = FakeVCardCodec(encoded = ""),
            )
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("details_more_button").performClick()
        compose.onNodeWithTag("show_qr_menu_item").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("show_qr_menu_item").fetchSemanticsNodes().isEmpty()
        }

        compose.onNodeWithTag("qr_preview_dialog").assertDoesNotExist()
    }
}
