package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
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
import com.m4isper.contactsapp.presentation.editor.rows.PhotoEditor
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
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
        scrollFormTo(hasTestTag(tag))
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    /**
     * Прокрутка формы к узлу. На узком окне `editor_form` — это сам список,
     * на широком `editor_form` — `Row` из двух колонок, поэтому скроллим
     * `editor_form_left` и `editor_form_right`.
     */
    private fun scrollFormTo(matcher: SemanticsMatcher) {
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        val columns =
            if (compose.onAllNodesWithTag("editor_form_left").fetchSemanticsNodes().isNotEmpty()) {
                listOf("editor_form_left", "editor_form_right")
            } else {
                listOf("editor_form")
            }
        var failure: AssertionError? = null
        for (column in columns) {
            try {
                compose.onNodeWithTag(column).performScrollToNode(matcher)
                return
            } catch (scrollFailure: AssertionError) {
                // Узел мог остаться в другой колонке: пробуем следующий контейнер.
                failure = scrollFailure
            }
        }
        throw failure!!
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
        scrollFormTo(hasTestTag("editor_phone_field"))
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
        scrollFormTo(hasTestTag("editor_phone_field"))
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

        scrollFormTo(hasTestTag("editor_add_email_button"))
        compose.onNodeWithTag("editor_add_email_button").performClick()
        scrollFormTo(hasTestTag("editor_email_field"))
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
        scrollFormTo(hasTestTag("editor_phone_more_$phoneRowId"))
        compose.onNodeWithTag("editor_phone_more_$phoneRowId").performClick()
        compose.onNodeWithTag("editor_phone_primary_$phoneRowId").assertTextContains("Set as primary value")
        compose.onNodeWithTag("editor_phone_primary_$phoneRowId").performClick()

        scrollFormTo(hasTestTag("editor_add_address_button"))
        compose.onNodeWithTag("editor_add_address_button").performClick()
        scrollFormTo(hasTestTag("editor_address_field"))
        typeInForm("editor_address_field", "Lenina 1")
        scrollFormTo(hasTestTag("editor_address_city_field"))
        typeInForm("editor_address_city_field", "Moscow")
        scrollFormTo(hasTestTag("editor_address_country_field"))
        typeInForm("editor_address_country_field", "Russia")

        scrollFormTo(hasTestTag("editor_add_sip_button"))
        compose.onNodeWithTag("editor_add_sip_button").performClick()
        scrollFormTo(hasTestTag("editor_sip_field"))
        typeInForm("editor_sip_field", "sip:anna@example.com")

        scrollFormTo(hasTestTag("editor_add_website_button"))
        compose.onNodeWithTag("editor_add_website_button").performClick()
        scrollFormTo(hasTestTag("editor_website_field"))
        typeInForm("editor_website_field", "https://blog.example")
        compose.onNodeWithText("Homepage").performClick()
        compose.onNodeWithText("Blog").performClick()

        scrollFormTo(hasTestTag("editor_company_field"))
        typeInForm("editor_company_field", "ACME")
        scrollFormTo(hasTestTag("editor_department_field"))
        typeInForm("editor_department_field", "Platform")
        scrollFormTo(hasTestTag("editor_job_description_field"))
        typeInForm("editor_job_description_field", "Leads engineering")
        scrollFormTo(hasTestTag("editor_office_location_field"))
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

        scrollFormTo(hasTestTag("editor_add_email_button"))
        compose.onNodeWithTag("editor_add_email_button").performClick()
        scrollFormTo(hasTestTag("editor_email_field"))
        typeInForm("editor_email_field", "not-an-email")
        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitForIdle()

        assertTrue(repository.created.isEmpty())
        scrollFormTo(hasText("Check the email address"))
        compose.onNodeWithText("Check the email address").assertExists()
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

        scrollFormTo(hasTestTag("editor_add_im_button"))
        compose.onNodeWithTag("editor_add_im_button").performClick()
        scrollFormTo(hasTestTag("editor_im_field"))
        typeInForm("editor_im_field", "tg_anna")
        scrollFormTo(hasTestTag("editor_im_service_field"))
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
        scrollFormTo(hasTestTag("editor_name_phonetic_given"))
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
        scrollFormTo(hasTestTag("editor_nickname_field"))
        typeInForm("editor_nickname_field", "Нюша")
        scrollFormTo(hasTestTag("editor_add_relation_button"))
        compose.onNodeWithTag("editor_add_relation_button").performClick()
        scrollFormTo(hasTestTag("editor_relation_field"))
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

        scrollFormTo(hasTestTag("editor_nickname_field"))
        compose.onNodeWithTag("editor_nickname_field").assertTextContains("Нюша")
        scrollFormTo(hasTestTag("editor_event_date_button"))
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

        scrollFormTo(hasTestTag("editor_delete_button"))
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

        scrollFormTo(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_check_row_Семья").assertIsDisplayed()
        compose.onNodeWithTag("group_check_row_Работа").assertDoesNotExist()
        Espresso.pressBack()

        scrollFormTo(hasTestTag("editor_account_row"))
        compose.onNodeWithTag("editor_account_row").performClick()
        compose.onNodeWithTag("account_option_com.google/user@gmail.com").performClick()

        scrollFormTo(hasTestTag("editor_groups_row"))
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

        scrollFormTo(hasTestTag("editor_account_row"))
        compose.onNodeWithTag("editor_account_row").assertIsDisplayed()
        compose.onNodeWithText("user@gmail.com").assertIsDisplayed()

        scrollFormTo(hasTestTag("editor_groups_row"))
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
        scrollFormTo(hasTestTag("editor_groups_row"))
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
        scrollFormTo(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_check_row_Семья").performClick()
        compose.onNodeWithTag("group_picker_confirm").performClick()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        assertEquals(listOf(100L), repository.created.single().groups)
    }

    /** Открывает редактор контакта [details] и ждёт загрузки формы. */
    private fun openEditor(
        details: ContactDetails,
        viewModel: ContactEditorViewModel,
    ) {
        compose.setContent {
            ContactEditorScreen(
                contactId = details.contactId,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.displayName == details.name
        }
    }

    @Test
    fun birthdayNoYear_survivesDialogRoundTrip() {
        val details =
            testDetails(7, "Anna", "111", birthday = Birthday(day = 21, month = 7, year = null))
        val repository = FakeAndroidRepository(listOf(details))
        val viewModel = viewModel(repository)
        openEditor(details, viewModel)

        scrollFormTo(hasTestTag("editor_birthday_button"))
        compose.onNodeWithTag("editor_birthday_button").performClick()
        compose.onNodeWithTag("editor_no_year_dialog_switch").assertIsDisplayed()
        compose.onNodeWithTag("editor_birthday_confirm").performClick()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.updated.single().second
        assertEquals(Birthday(day = 21, month = 7, year = null), form.birthday.value)
    }

    @Test
    fun birthdaySwitchOff_opensPickerAndStoresChosenYear() {
        val details =
            testDetails(7, "Anna", "111", birthday = Birthday(day = 21, month = 7, year = null))
        val repository = FakeAndroidRepository(listOf(details))
        val viewModel = viewModel(repository)
        openEditor(details, viewModel)

        scrollFormTo(hasTestTag("editor_birthday_switch"))
        compose.onNodeWithTag("editor_birthday_switch").performClick()
        compose.onNodeWithTag("editor_birthday_confirm").assertIsDisplayed()
        compose.onNodeWithTag("editor_birthday_confirm").performClick()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.updated.single().second
        assertEquals(Birthday(day = 21, month = 7, year = LocalDate.now().year), form.birthday.value)
    }

    @Test
    fun birthdayDialogSwitch_storesNoYear() {
        val details =
            testDetails(7, "Anna", "111", birthday = Birthday(day = 21, month = 7, year = 1985))
        val repository = FakeAndroidRepository(listOf(details))
        val viewModel = viewModel(repository)
        openEditor(details, viewModel)

        scrollFormTo(hasTestTag("editor_birthday_button"))
        compose.onNodeWithTag("editor_birthday_button").performClick()
        compose.onNodeWithTag("editor_no_year_dialog_switch").performClick()
        compose.onNodeWithTag("editor_birthday_confirm").performClick()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.updated.single().second
        assertEquals(Birthday(day = 21, month = 7, year = null), form.birthday.value)
    }

    @Test
    fun birthdayPickerCancel_keepsNoYearValue() {
        val details =
            testDetails(7, "Anna", "111", birthday = Birthday(day = 21, month = 7, year = null))
        val repository = FakeAndroidRepository(listOf(details))
        val viewModel = viewModel(repository)
        openEditor(details, viewModel)

        scrollFormTo(hasTestTag("editor_birthday_switch"))
        compose.onNodeWithTag("editor_birthday_switch").performClick()
        compose.onNodeWithTag("editor_birthday_confirm").assertIsDisplayed()
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.updated.single().second
        assertEquals(Birthday(day = 21, month = 7, year = null), form.birthday.value)
    }

    @Test
    fun eventSwitchOff_opensPickerAndStoresChosenYear() {
        val details =
            testDetails(
                7,
                "Anna",
                "111",
                events =
                    listOf(
                        ContactEvent(date = Birthday(day = 1, month = 6, year = null)),
                    ),
            )
        val repository = FakeAndroidRepository(listOf(details))
        val viewModel = viewModel(repository)
        openEditor(details, viewModel)

        scrollFormTo(hasTestTag("editor_event_switch"))
        compose.onNodeWithTag("editor_event_switch").performClick()
        compose.onNodeWithTag("editor_event_date_confirm").assertIsDisplayed()
        compose.onNodeWithTag("editor_event_date_confirm").performClick()
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        val form = repository.updated.single().second
        assertEquals(Birthday(day = 1, month = 6, year = LocalDate.now().year), form.events.single().date)
    }

    @Test
    fun blockedSave_scrollsToFirstInvalidRowAndShowsSummary() {
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
        typeInForm("editor_phone_field", "+79161234567")
        scrollFormTo(hasTestTag("editor_add_phone_button"))
        compose.onNodeWithTag("editor_add_phone_button").performClick()
        scrollFormTo(hasTestTag("editor_phone_field"))
        compose.waitForIdle()
        compose.onAllNodesWithTag("editor_phone_field")[1].performTextInput("abc")
        val invalidRowId =
            viewModel.uiState.value.phones[1]
                .rowId

        // Уводим невалидную строку за пределы экрана и нажимаем Save в топбаре.
        // Цель берём из той же колонки, что и телефоны: на широком окне заметка — в правой.
        scrollFormTo(hasTestTag("editor_add_sip_button"))
        compose.waitForIdle()
        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitForIdle()

        assertTrue(repository.created.isEmpty())
        compose.onNodeWithText("Check the highlighted fields").assertIsDisplayed()
        // Скролл проверяем по границам: узел в дереве, но assertIsDisplayed капризничает
        // рядом со снекбаром.
        val form = compose.onNodeWithTag("editor_form").fetchSemanticsNode()
        val formTop = form.positionInRoot.y.toInt()
        val formBottom = formTop + form.size.height
        val invalidRow = compose.onNodeWithTag("editor_phone_row_$invalidRowId").fetchSemanticsNode()
        val top = invalidRow.positionInRoot.y.toInt()
        val bottom = top + invalidRow.size.height
        assertTrue(
            "invalid phone row must be on screen: top=$top bottom=$bottom form=$formTop..$formBottom",
            top >= formTop && bottom <= formBottom,
        )
    }

    @Test
    fun photoActions_wrapInsteadOfClipping() {
        compose.setContent {
            // Двойная плотность: тексты занимают вдвое больше места — Row обрезал бы действия.
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                PhotoEditor(name = "Anna", photoUri = null, onPick = {}, onTakePhoto = {}, onRemove = {})
            }
        }

        compose.onNodeWithTag("editor_photo_camera").assertIsDisplayed()
        compose.onNodeWithTag("editor_photo_pick").assertIsDisplayed()
        compose.onNodeWithTag("editor_photo_remove").assertDoesNotExist()
    }

    @Test
    fun expandedWidth_splitsFormIntoTwoColumns() {
        val viewModel = viewModel(FakeAndroidRepository())
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1000.dp, 800.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }

        compose.onNodeWithTag("editor_form_left").assertIsDisplayed()
        compose.onNodeWithTag("editor_form_right").assertIsDisplayed()
        // Порядок полей сохранён: имя — в левой колонке, работа и даты — в правой.
        compose.onNodeWithTag("editor_name_field").assertIsDisplayed()
        compose.onNodeWithTag("editor_form_right").performScrollToNode(hasTestTag("editor_birthday_button"))
        compose.onNodeWithTag("editor_birthday_button").assertIsDisplayed()
    }

    @Test
    fun compactWidth_keepsSingleFormList() {
        val viewModel = viewModel(FakeAndroidRepository())
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(411.dp, 891.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }

        compose.onNodeWithTag("editor_form").assertIsDisplayed()
        compose.onNodeWithTag("editor_form_right").assertDoesNotExist()
    }

    @Test
    fun compactWidth_phoneRowStaysOneLineWithoutClipping() {
        val viewModel = viewModel(FakeAndroidRepository())
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 700.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }
        val phoneRowId =
            viewModel.uiState.value.phones
                .single()
                .rowId
        scrollFormTo(hasTestTag("editor_phone_row_$phoneRowId"))

        val row = compose.onNodeWithTag("editor_phone_row_$phoneRowId").getUnclippedBoundsInRoot()
        val rowHeight = row.bottom - row.top
        assertTrue("row is $rowHeight tall", rowHeight <= 64.5.dp)
        assertTrue("row ends at ${row.right}", row.right <= 320.dp)
        compose.onNodeWithTag("editor_phone_field").assertIsDisplayed()
        compose.onNodeWithTag("editor_phone_drag_$phoneRowId").assertIsDisplayed()
        compose.onNodeWithTag("editor_phone_more_$phoneRowId").assertIsDisplayed()
    }

    @Test
    fun draggingTheHandleMovesARowUp() {
        val viewModel = twoPhones()

        val secondRowId =
            viewModel.uiState.value.phones[1]
                .rowId
        scrollFormTo(hasTestTag("editor_phone_drag_$secondRowId"))
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("editor_phone_drag_$secondRowId").performTouchInput {
            swipe(start = center, end = center + Offset(0f, -80.dp.toPx()))
        }
        compose.waitForIdle()

        assertEquals(
            listOf("222", "111"),
            viewModel.uiState.value.phones
                .map { it.value.number },
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reorderHandleOffersMoveActionsToTalkBack() {
        val viewModel = twoPhones()

        val secondRowId =
            viewModel.uiState.value.phones[1]
                .rowId
        scrollFormTo(hasTestTag("editor_phone_drag_$secondRowId"))
        compose.waitForIdle()
        compose
            .onNodeWithTag("editor_phone_drag_$secondRowId")
            .performCustomAccessibilityActionWithLabel("Move up")
        compose.waitForIdle()

        assertEquals(
            listOf("222", "111"),
            viewModel.uiState.value.phones
                .map { it.value.number },
        )
    }

    private fun twoPhones(): ContactEditorViewModel {
        val viewModel = viewModel(FakeAndroidRepository())
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 700.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }
        typeInForm("editor_phone_field", "111")
        scrollFormTo(hasTestTag("editor_add_phone_button"))
        compose.onNodeWithTag("editor_add_phone_button").performClick()
        scrollFormTo(hasTestTag("editor_phone_field"))
        compose.waitForIdle()
        compose.onAllNodesWithTag("editor_phone_field")[1].performTextInput("222")
        compose.waitForIdle()
        return viewModel
    }

    @Test
    fun dismissingTheGroupsSheetKeepsFieldFocus() {
        val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
        val viewModel = viewModel(FakeAndroidRepository(), groups)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 800.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.allGroups
                .isNotEmpty()
        }

        scrollFormTo(hasTestTag("editor_groups_row"))
        scrollFormTo(hasTestTag("editor_nickname_field"))
        compose.onNodeWithTag("editor_nickname_field").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("editor_nickname_field").assertIsFocused()

        scrollFormTo(hasTestTag("editor_groups_row"))
        compose.onNodeWithTag("editor_groups_row").performClick()
        compose.onNodeWithTag("group_picker_confirm").assertIsDisplayed()
        Espresso.pressBack()
        compose.waitForIdle()

        compose.onNodeWithTag("editor_nickname_field").assertIsFocused()
    }

    @Test
    fun expandedWidth_invalidFieldBlocksSave() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1000.dp, 800.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }

        compose.onNodeWithTag("editor_form_left").performScrollToNode(hasTestTag("editor_add_email_button"))
        compose.onNodeWithTag("editor_add_email_button").performClick()
        compose.onNodeWithTag("editor_form_left").performScrollToNode(hasTestTag("editor_email_field"))
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("editor_email_field").performTextInput("not-an-email")
        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitForIdle()

        assertTrue(repository.created.isEmpty())
        compose.onNodeWithTag("editor_form_left").performScrollToNode(hasText("Check the email address"))
        compose.onNodeWithText("Check the email address").assertExists()
    }

    @Test
    fun rotation_restoresScrollAndUnsavedInput() {
        val viewModel = viewModel(FakeAndroidRepository())
        val restorer = StateRestorationTester(compose)
        // Поворот телефона: узкое окно, один список, а не две колонки планшета.
        restorer.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(411.dp, 891.dp))) {
                ContactEditorScreen(contactId = null, onDone = {}, onBack = {}, viewModel = viewModel)
            }
        }

        typeInForm("editor_nickname_field", "anna")
        Espresso.closeSoftKeyboard()
        scrollFormTo(hasTestTag("editor_birthday_button"))
        compose.onNodeWithTag("editor_birthday_button").assertIsDisplayed()

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("editor_birthday_button").assertIsDisplayed()
        scrollFormTo(hasTestTag("editor_nickname_field"))
        compose.onNodeWithTag("editor_nickname_field").assertTextContains("anna")
    }
}
