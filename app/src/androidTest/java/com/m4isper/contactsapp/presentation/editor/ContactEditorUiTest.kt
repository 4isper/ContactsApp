package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactEvent
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.domain.model.contact.NameMode
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import com.m4isper.contactsapp.domain.usecase.contacts.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.SetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.UpdateContactUseCase
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ContactEditorUiTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    private fun viewModel(
        repository: FakeAndroidRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = ContactEditorViewModel(
        GetContactFormUseCase(repository),
        CreateContactUseCase(repository),
        UpdateContactUseCase(repository),
        DeleteContactsUseCase(repository, repository),
        GetGroupsUseCase(groups),
        CreateGroupUseCase(groups),
        GetContactAccountsUseCase(repository),
        GetContactAccountUseCase(repository),
        GetDefaultContactAccountUseCase(settings),
        GetContactsUseCase(repository),
        SetLastContactAccountUseCase(settings),
    )

    /**
     * Ввод в поле формы: клавиатуру закрываем, поле прокручиваем в видимую часть.
     * Иначе фокус вызывает перескролл списка, и узел поля успевает исчезнуть.
     */
    private fun typeInForm(
        tag: String,
        text: String,
    ) {
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    /** Ввод в поле вне списка формы, например в диалоге. */
    private fun typeText(
        tag: String,
        text: String,
    ) {
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    @Test
    fun createMode_savesToSelectedAccount() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val repository =
            FakeAndroidRepository().apply {
                accounts = listOf(ContactAccount.Local, google)
            }
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.accounts.size == 2 }

        compose.onNodeWithTag("editor_account_row").performClick()
        compose.onNodeWithTag("account_option_com.google/user@gmail.com").performClick()
        typeInForm("editor_name_field", "Anna")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_phone_field"))
        typeInForm("editor_phone_field", "111")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(listOf(google), repository.createdAccounts)
    }

    @Test
    fun createMode_savesValidForm() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }

        typeInForm("editor_name_field", "Anna")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_phone_field"))
        typeInForm("editor_phone_field", "111")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(true, done.get())
        assertEquals(1, repository.created.size)
        assertEquals(
            "Anna",
            repository.created
                .single()
                .name
                .display,
        )
        assertEquals(
            listOf("111"),
            repository.created
                .single()
                .phones
                .map { it.number },
        )
    }

    @Test
    fun createMode_emptySaveCreatesNothing() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitForIdle()

        assertEquals(false, viewModel.uiState.value.closeScreen)
        assertEquals(false, done.get())
        assertEquals(true, repository.created.isEmpty())
    }

    @Test
    fun addPhoneButton_addsSecondRow() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onAllNodesWithTag("editor_phone_field").assertCountEquals(1)
        compose.onNodeWithTag("editor_add_phone_button").performClick()
        compose.onAllNodesWithTag("editor_phone_field").assertCountEquals(2)
    }

    @Test
    fun editMode_prefillsAndUpdates() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = 7,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.displayName == "Anna"
        }

        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(true, done.get())
        assertEquals(1, repository.updated.size)
        assertEquals(7L, repository.updated.single().first)
        assertEquals(
            "Anna",
            repository.updated
                .single()
                .second.name.display,
        )
    }

    @Test
    fun addEmailButton_addsRowAndSavesEmail() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_email_button"))
        compose.onNodeWithTag("editor_add_email_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_email_field"))
        typeInForm("editor_email_field", "anna@example.com")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(
            listOf("anna@example.com"),
            repository.created
                .single()
                .emails
                .map { it.address },
        )
    }

    @Test
    fun createMode_savesStructuredAddressOrganizationSipAndWebsiteKind() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        typeInForm("editor_name_field", "Anna")
        typeInForm("editor_phone_field", "111")
        val phoneRowId =
            viewModel.uiState.value.phones[0]
                .rowId
        Espresso.closeSoftKeyboard()
        compose
            .onNodeWithTag("editor_form")
            .performScrollToNode(hasTestTag("editor_phone_primary_$phoneRowId"))
        compose.onNodeWithTag("editor_phone_primary_$phoneRowId").performClick()

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_address_button"))
        compose.onNodeWithTag("editor_add_address_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_address_field"))
        typeInForm("editor_address_field", "Lenina 1")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_address_city_field"))
        typeInForm("editor_address_city_field", "Moscow")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_address_country_field"))
        typeInForm("editor_address_country_field", "Russia")

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_sip_button"))
        compose.onNodeWithTag("editor_add_sip_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_sip_field"))
        typeInForm("editor_sip_field", "sip:anna@example.com")

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_website_button"))
        compose.onNodeWithTag("editor_add_website_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_website_field"))
        typeInForm("editor_website_field", "https://blog.example")
        compose.onNodeWithText("Homepage").performClick()
        compose.onNodeWithText("Blog").performClick()

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_company_field"))
        typeInForm("editor_company_field", "ACME")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_department_field"))
        typeInForm("editor_department_field", "Platform")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_job_description_field"))
        typeInForm("editor_job_description_field", "Leads engineering")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_office_location_field"))
        typeInForm("editor_office_location_field", "London")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.created.single()
        assertEquals(true, form.phones.single().isPrimary)
        assertEquals("Lenina 1", form.addresses.single().street)
        assertEquals("Moscow", form.addresses.single().city)
        assertEquals("Russia", form.addresses.single().country)
        assertEquals("sip:anna@example.com", form.sipAddresses.single().address)
        assertEquals(WebsiteKind.BLOG, form.websites.single().kind)
        assertEquals("ACME", form.organization.company)
        assertEquals("Platform", form.organization.department)
        assertEquals("Leads engineering", form.organization.jobDescription)
        assertEquals("London", form.organization.officeLocation)
    }

    @Test
    fun invalidEmail_showsErrorAndBlocksSaving() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_email_button"))
        compose.onNodeWithTag("editor_add_email_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_email_field"))
        typeInForm("editor_email_field", "not-an-email")
        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitForIdle()

        assertTrue(repository.created.isEmpty())
        compose.onNodeWithTag("editor_form").performScrollToNode(hasText("Check the email address"))
        compose.onNodeWithText("Check the email address").assertIsDisplayed()
    }

    @Test
    fun addImButton_addsRowAndSavesAccount() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_im_button"))
        compose.onNodeWithTag("editor_add_im_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_im_field"))
        typeInForm("editor_im_field", "tg_anna")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_im_service_field"))
        typeInForm("editor_im_service_field", "Telegram")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val im =
            repository.created
                .single()
                .instantMessages
                .single()
        assertEquals("tg_anna", im.address)
        assertEquals("Telegram", im.customProtocol)
    }

    @Test
    fun structuredMode_savesNameParts() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_toggle_name_mode").performClick()
        typeInForm("editor_name_given", "Иван")
        typeInForm("editor_name_family", "Петров")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val name = repository.created.single().name
        assertEquals(NameMode.STRUCTURED, name.mode)
        assertEquals("Иван", name.given)
        assertEquals("Петров", name.family)
    }

    @Test
    fun detailsSection_savesPhoneticName() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_toggle_name_mode").performClick()
        typeInForm("editor_name_given", "Иван")
        compose.onNodeWithTag("editor_toggle_name_details").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_name_phonetic_given"))
        typeInForm("editor_name_phonetic_given", "Ivan")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val name = repository.created.single().name
        assertEquals("Ivan", name.phoneticGiven)
    }

    @Test
    fun addRelationAndNickname_savesThem() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        typeInForm("editor_name_field", "Anna")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_nickname_field"))
        typeInForm("editor_nickname_field", "Нюша")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_add_relation_button"))
        compose.onNodeWithTag("editor_add_relation_button").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_relation_field"))
        typeInForm("editor_relation_field", "Bob")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.created.single()
        assertEquals("Нюша", form.nickname.nickname)
        assertEquals("Bob", form.relations.single().name)
    }

    @Test
    fun editMode_eventPickerOpensAndKeepsPrefilledData() {
        val details =
            testDetails(
                7,
                "Anna",
                "111",
                nickname = "Нюша",
                events =
                    listOf(
                        ContactEvent(
                            date = Birthday(day = 1, month = 6, year = 2015),
                            kind = EventKind.ANNIVERSARY,
                        ),
                    ),
            )
        val repository = FakeAndroidRepository(listOf(details))
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = 7,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.displayName == "Anna"
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_nickname_field"))
        compose.onNodeWithTag("editor_nickname_field").assertTextContains("Нюша")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_event_date_button"))
        compose.onNodeWithTag("editor_event_date_button").performClick()
        compose.onNodeWithTag("editor_event_date_confirm").assertIsDisplayed()
        Espresso.pressBack()
        compose.waitForIdle()

        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.updated.single().second
        assertEquals("Нюша", form.nickname.nickname)
        assertEquals(Birthday(day = 1, month = 6, year = 2015), form.events.single().date)
    }

    @Test
    fun editMode_deleteRemovesContact() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = 7,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.displayName == "Anna"
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_delete_button"))
        compose.onNodeWithTag("editor_delete_button").performClick()
        compose.onNodeWithTag("confirm_delete_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(true, done.get())
        assertEquals(listOf(7L), repository.deleted)
    }

    @Test
    fun createMode_filtersGroupsBySelectedAccount() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val repository =
            FakeAndroidRepository().apply {
                accounts = listOf(ContactAccount.Local, google)
            }
        val groups =
            FakeGroupsRepository(
                groups =
                    listOf(
                        ContactGroup(id = 100, title = "Семья"),
                        ContactGroup(
                            id = 200,
                            title = "Работа",
                            accountName = google.name,
                            accountType = google.type,
                        ),
                    ),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.accounts.size == 2 && viewModel.uiState.value.allGroups.size == 2
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_check_row_Семья").assertIsDisplayed()
        compose.onNodeWithTag("group_check_row_Работа").assertDoesNotExist()
        Espresso.pressBack()

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_account_row"))
        compose.onNodeWithTag("editor_account_row").performClick()
        compose.onNodeWithTag("account_option_com.google/user@gmail.com").performClick()

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_check_row_Работа").assertIsDisplayed()
        compose.onNodeWithTag("group_check_row_Семья").assertDoesNotExist()
    }

    @Test
    fun editMode_showsAccountAndFiltersGroups() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val repository =
            FakeAndroidRepository(
                listOf(testDetails(7, "Anna", "111").copy(account = google)),
            )
        val groups =
            FakeGroupsRepository(
                groups =
                    listOf(
                        ContactGroup(id = 100, title = "Семья"),
                        ContactGroup(
                            id = 200,
                            title = "Работа",
                            accountName = google.name,
                            accountType = google.type,
                        ),
                    ),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            ContactEditorScreen(
                contactId = 7,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.displayName == "Anna" &&
                viewModel.uiState.value.selectedAccount == google
        }

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_account_row"))
        compose.onNodeWithTag("editor_account_row").assertIsDisplayed()
        compose.onNodeWithText("user@gmail.com").assertIsDisplayed()

        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_check_row_Работа").assertIsDisplayed()
        compose.onNodeWithTag("group_check_row_Семья").assertDoesNotExist()
    }

    @Test
    fun createMode_createsGroupInSelectedAccount() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val repository =
            FakeAndroidRepository().apply {
                accounts = listOf(ContactAccount.Local, google)
            }
        val groups = FakeGroupsRepository()
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.accounts.size == 2 }

        compose.onNodeWithTag("editor_account_row").performClick()
        compose.onNodeWithTag("account_option_com.google/user@gmail.com").performClick()
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        typeText("new_group_field", "Проект")
        compose.onNodeWithTag("new_group_confirm").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { groups.groups.size == 1 }
        assertEquals(listOf(google), groups.createdGroupAccounts)
        compose.onNodeWithTag("group_check_row_Проект").assertIsDisplayed()
    }

    @Test
    fun createMode_selectsGroupFromPickerAndSaves() {
        val repository = FakeAndroidRepository()
        val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.allGroups
                .isNotEmpty()
        }

        typeInForm("editor_name_field", "Anna")
        compose.onNodeWithTag("editor_form").performScrollToNode(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_check_row_Семья").performClick()
        compose.onNodeWithTag("group_picker_confirm").performClick()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        assertEquals(listOf(100L), repository.created.single().groups)
    }
}
