package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.format.addressKindLabel
import com.m4isper.contactsapp.presentation.common.format.birthdayText
import com.m4isper.contactsapp.presentation.common.format.contactEventLabel
import com.m4isper.contactsapp.presentation.common.format.emailKindLabel
import com.m4isper.contactsapp.presentation.common.format.instantMessageLabel
import com.m4isper.contactsapp.presentation.common.format.relationLabel
import com.m4isper.contactsapp.presentation.common.share.openEmail
import com.m4isper.contactsapp.presentation.common.share.openMap
import com.m4isper.contactsapp.presentation.common.share.openWebsite

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
                    details.phones.firstOrNull()?.let(onCallClick)
                },
                onSmsPrimary = {
                    details.phones.firstOrNull()?.let(onSmsClick)
                },
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
                secondary = stringResource(R.string.group_label),
                actions = emptyList(),
                modifier = Modifier.testTag("group_row_${group.title}"),
            )
        }
        items(
            items = details.phones,
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
            items = details.emails,
            key = { "email_${it.address}" },
        ) { email ->
            DataRow(
                primary = email.address,
                secondary = email.customLabel ?: emailKindLabel(email.kind),
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
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(im.address) },
                    ),
                modifier = Modifier.testTag("im_row_${im.address}"),
            )
        }
        items(
            items = details.addresses,
            key = { "address_${it.address}" },
        ) { address ->
            DataRow(
                primary = address.address,
                secondary = address.customLabel ?: addressKindLabel(address.kind),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.LocationOn,
                            stringResource(R.string.action_open_map),
                        ) { openMap(context, address.address) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(address.address) },
                    ),
                modifier = Modifier.testTag("address_row_${address.address}"),
            )
        }
        items(
            items = details.websites,
            key = { "website_${it.url}" },
        ) { website ->
            DataRow(
                primary = website.url,
                secondary = null,
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
                DataRow(
                    primary = organization.company.ifBlank { organization.title },
                    secondary = organization.title.takeIf { organization.company.isNotBlank() },
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
                    actions = emptyList(),
                    modifier = Modifier.testTag("birthday_row"),
                )
            }
        }
        items(
            items = details.events,
            key = { "event_${it.date.toStorageString()}_${it.kind.name}" },
        ) { event ->
            DataRow(
                primary = birthdayText(event.date),
                secondary = contactEventLabel(event),
                actions = emptyList(),
                modifier = Modifier.testTag("event_row_${event.date.toStorageString()}"),
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
