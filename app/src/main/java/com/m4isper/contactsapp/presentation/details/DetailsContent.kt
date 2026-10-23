package com.m4isper.contactsapp.presentation.details

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.calendar.nextOccurrence
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.presentation.common.account.contactAccountLabel
import com.m4isper.contactsapp.presentation.common.calendar.openCalendarEvent
import com.m4isper.contactsapp.presentation.common.format.addressKindLabel
import com.m4isper.contactsapp.presentation.common.format.birthdayText
import com.m4isper.contactsapp.presentation.common.format.contactEventLabel
import com.m4isper.contactsapp.presentation.common.format.emailKindLabel
import com.m4isper.contactsapp.presentation.common.format.instantMessageLabel
import com.m4isper.contactsapp.presentation.common.format.relationLabel
import com.m4isper.contactsapp.presentation.common.format.sipKindLabel
import com.m4isper.contactsapp.presentation.common.format.websiteKindLabel
import com.m4isper.contactsapp.presentation.common.share.openEmail
import com.m4isper.contactsapp.presentation.common.share.openInstantMessage
import com.m4isper.contactsapp.presentation.common.share.openMap
import com.m4isper.contactsapp.presentation.common.share.openWebsite
import java.time.LocalDate

@Composable
internal fun DetailsContent(
    details: ContactDetails,
    onCallClick: (PhoneNumber) -> Unit,
    onSmsClick: (PhoneNumber) -> Unit,
    onCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    groups: List<ContactGroup> = emptyList(),
) {
    val context = LocalContext.current
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("details_list"),
    ) {
        item(key = "header") {
            DetailsHeader(
                details = details,
                onCallPrimary = {
                    details.primaryPhone?.let(onCallClick)
                },
                onSmsPrimary = {
                    details.primaryPhone?.let(onSmsClick)
                },
            )
        }
        item(key = "account") {
            DataRow(
                primary = contactAccountLabel(details.account),
                secondary = stringResource(R.string.editor_section_account),
                actions = emptyList(),
                modifier = Modifier.testTag("details_account_row"),
            )
        }
        details.nickname?.let { nickname ->
            item(key = "nickname") {
                DataRow(
                    primary = nickname,
                    secondary = stringResource(R.string.editor_section_nickname),
                    actions =
                        listOf(
                            RowAction(
                                Icons.Filled.ContentCopy,
                                stringResource(R.string.action_copy),
                            ) { onCopyClick(nickname) },
                        ),
                    modifier = Modifier.testTag("nickname_row"),
                )
            }
        }
        items(
            items = groups,
            key = { "group_${it.id}" },
        ) { group ->
            DataRow(
                primary = group.title,
                secondary =
                    listOf(
                        stringResource(R.string.group_label),
                        contactAccountLabel(group.account),
                    ).joinToString(" · "),
                actions = emptyList(),
                modifier = Modifier.testTag("group_row_${group.title}"),
            )
        }
        items(
            items = details.phones.sortedByDescending { it.isPrimary },
            key = { "phone_${it.number}" },
        ) { phone ->
            PhoneRow(
                phone = phone,
                onClick = { onCallClick(phone) },
                onSmsClick = { onSmsClick(phone) },
                onCopyClick = { onCopyClick(phone.number) },
            )
        }
        items(
            items = details.emails.sortedByDescending { it.isPrimary },
            key = { "email_${it.address}" },
        ) { email ->
            DataRow(
                primary = email.address,
                secondary = valueKindLabel(email.customLabel ?: emailKindLabel(email.kind), email.isPrimary),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.Email,
                            stringResource(R.string.action_send_email),
                        ) { openEmail(context, email.address) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(email.address) },
                    ),
                modifier = Modifier.testTag("email_row_${email.address}"),
            )
        }
        items(
            items = details.instantMessages,
            key = { "im_${it.address}" },
        ) { im ->
            DataRow(
                primary = im.address,
                secondary = instantMessageLabel(im),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.Chat,
                            stringResource(R.string.action_open_messenger),
                        ) {
                            // Нет приложения для протокола — копируем адрес, как раньше.
                            if (!openInstantMessage(context, im)) onCopyClick(im.address)
                        },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(im.address) },
                    ),
                modifier = Modifier.testTag("im_row_${im.address}"),
            )
        }
        items(
            items = details.sipAddresses,
            key = { "sip_${it.address}" },
        ) { sip ->
            DataRow(
                primary = sip.address,
                secondary = sip.customLabel ?: sipKindLabel(sip.kind),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(sip.address) },
                    ),
                modifier = Modifier.testTag("sip_row_${sip.address}"),
            )
        }
        items(
            items = details.addresses,
            key = { "address_${it.displayAddress}" },
        ) { address ->
            DataRow(
                primary = address.displayAddress,
                secondary = address.customLabel ?: addressKindLabel(address.kind),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.LocationOn,
                            stringResource(R.string.action_open_map),
                        ) { openMap(context, address.displayAddress) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(address.displayAddress) },
                    ),
                modifier = Modifier.testTag("address_row_${address.displayAddress}"),
            )
        }
        items(
            items = details.websites,
            key = { "website_${it.url}" },
        ) { website ->
            DataRow(
                primary = website.url,
                secondary = website.customLabel ?: websiteKindLabel(website.kind),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.Public,
                            stringResource(R.string.action_open_website),
                        ) { openWebsite(context, website.url) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(website.url) },
                    ),
                modifier = Modifier.testTag("website_row_${website.url}"),
            )
        }
        details.organization?.let { organization ->
            item(key = "organization") {
                val primary =
                    listOf(
                        organization.company,
                        organization.title,
                        organization.department,
                        organization.officeLocation,
                    ).firstOrNull { it.isNotBlank() }.orEmpty()
                val secondary =
                    listOf(
                        organization.title.takeIf { organization.company.isNotBlank() },
                        organization.department,
                        organization.jobDescription,
                        organization.officeLocation,
                    ).filterNotNull()
                        .filter { it.isNotBlank() && it != primary }
                        .joinToString(" · ")
                        .takeIf { it.isNotBlank() }
                DataRow(
                    primary = primary,
                    secondary = secondary,
                    actions = emptyList(),
                    modifier = Modifier.testTag("organization_row"),
                )
            }
        }
        details.birthday?.let { birthday ->
            item(key = "birthday") {
                DataRow(
                    primary = birthdayText(birthday),
                    secondary = stringResource(R.string.editor_section_birthday),
                    actions =
                        listOf(
                            calendarAction(
                                context = context,
                                title = stringResource(R.string.calendar_birthday_title, details.name),
                                day = birthday.day,
                                month = birthday.month,
                            ),
                        ),
                    modifier = Modifier.testTag("birthday_row"),
                )
            }
        }
        items(
            items = details.events,
            key = { "event_${birthdayKey(it.date)}_${it.kind.name}" },
        ) { event ->
            DataRow(
                primary = birthdayText(event.date),
                secondary = contactEventLabel(event),
                actions =
                    listOf(
                        calendarAction(
                            context = context,
                            title = stringResource(R.string.calendar_event_title, details.name),
                            day = event.date.day,
                            month = event.date.month,
                        ),
                    ),
                modifier = Modifier.testTag("event_row_${birthdayKey(event.date)}"),
            )
        }
        items(
            items = details.relations,
            key = { "relation_${it.name}" },
        ) { relation ->
            DataRow(
                primary = relation.name,
                secondary = relationLabel(relation),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(relation.name) },
                    ),
                modifier = Modifier.testTag("relation_row_${relation.name}"),
            )
        }
        details.note?.let { note ->
            item(key = "note") {
                DataRow(
                    primary = note,
                    secondary = stringResource(R.string.editor_section_note),
                    actions = emptyList(),
                    modifier = Modifier.testTag("note_row"),
                )
            }
        }
    }
}

@Composable
private fun valueKindLabel(
    label: String,
    isPrimary: Boolean,
): String = if (isPrimary) "$label · ${stringResource(R.string.primary_value)}" else label

/** Ключ строки события для списка и testTag; формат не связан с форматом хранения. */
private fun birthdayKey(date: Birthday): String =
    buildString {
        append(date.year?.toString()?.padStart(YEAR_DIGITS, '0') ?: "--")
        append('-')
        append(date.month.toString().padStart(DATE_PART_DIGITS, '0'))
        append('-')
        append(date.day.toString().padStart(DATE_PART_DIGITS, '0'))
    }

private const val YEAR_DIGITS = 4
private const val DATE_PART_DIGITS = 2

/** Действие «в календарь»: ближайшее наступление даты уходит в календарь. */
@Composable
private fun calendarAction(
    context: Context,
    title: String,
    day: Int,
    month: Int,
): RowAction =
    RowAction(
        Icons.Filled.Event,
        stringResource(R.string.action_add_to_calendar),
    ) {
        val date = nextOccurrence(day, month, LocalDate.now())
        if (date != null) {
            openCalendarEvent(context = context, title = title, date = date)
        }
    }
