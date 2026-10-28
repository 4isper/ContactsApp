package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Карточка контакта на широком окне остаётся читаемой колонкой, а не растягивается. */
@RunWith(AndroidJUnit4::class)
class DetailsContentWidthTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun contentIsCappedAndCenteredOnExpandedWidth() {
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1000.dp, 800.dp))) {
                    DetailsContent(
                        details = ContactDetails(contactId = 1, name = "Anna Ivanova"),
                        isFavorite = false,
                        listState = rememberLazyListState(),
                        onCallClick = {},
                        onSmsClick = {},
                        onEmailClick = {},
                        onCopyClick = {},
                        onToggleFavorite = {},
                        onHeroNameBottom = {},
                    )
                }
            }
        }

        val bounds = compose.onNodeWithTag("details_list").getUnclippedBoundsInRoot()
        assertEquals(640f, bounds.width.value, 0.5f)
        assertEquals(180f, bounds.left.value, 0.5f)
    }
}
