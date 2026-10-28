package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.testDetails
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FastScrollerUiTest {
    @get:Rule
    val compose = createComposeRule()

    // 72 строки на шесть секций — список заведомо длиннее экрана.
    private val contacts: List<ContactDetails> =
        names.flatMapIndexed { letterIndex, letter ->
            (1..12).map { number ->
                val id = letterIndex * 12L + number
                testDetails(id, "$letter name $number", "555")
            }
        }

    @Test
    fun bubbleAppearsWhileDraggingAndDisappearsOnRelease() {
        setList()

        compose.onNodeWithTag("fast_scroller_bubble").assertDoesNotExist()

        compose.onNodeWithTag("fast_scroller").performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
        }
        compose.onNodeWithTag("fast_scroller_bubble").assertIsDisplayed()

        compose.onNodeWithTag("fast_scroller").performTouchInput { up() }
        compose.onNodeWithTag("fast_scroller_bubble").assertDoesNotExist()
    }

    @Test
    fun talkBackNextSectionWalksForwardThroughSections() {
        setList()
        val nextLabel =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(R.string.a11y_next_section)

        // A → B → C → D → E → F; TalkBack перечитывает узел после каждого действия.
        repeat(5) {
            val next =
                compose
                    .onNodeWithTag("fast_scroller")
                    .fetchSemanticsNode()
                    .config[SemanticsActions.CustomActions]
                    .first { it.label == nextLabel }
            compose.runOnIdle { next.action() }
            compose.waitForIdle()
        }

        // Первая строка последней секции F — id 61.
        compose.onNodeWithTag("contact_row_61").assertIsDisplayed()
    }

    private fun setList() {
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                GroupedContacts(
                    contacts = contacts,
                    showFavoriteQuickAction = true,
                    favorites = emptySet(),
                    selection = emptySet(),
                    isSelectionMode = false,
                    onContactClick = {},
                    onContactLongClick = {},
                )
            }
        }
    }

    private companion object {
        val names = listOf("Anna", "Bob", "Cara", "Dan", "Eli", "Finn")
    }
}
