package com.m4isper.contactsapp.presentation.details

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.calendar.nextOccurrence
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.presentation.common.account.contactAccountLabel
import com.m4isper.contactsapp.presentation.common.calendar.openCalendarEvent
import com.m4isper.contactsapp.presentation.common.format.addressKindLabel
import com.m4isper.contactsapp.presentation.common.format.birthdayText
import com.m4isper.contactsapp.presentation.common.format.contactEventLabel
import com.m4isper.contactsapp.presentation.common.format.emailKindLabel
import com.m4isper.contactsapp.presentation.common.format.instantMessageLabel
import com.m4isper.contactsapp.presentation.common.format.phoneKindLabel
import com.m4isper.contactsapp.presentation.common.format.relationLabel
import com.m4isper.contactsapp.presentation.common.format.sipKindLabel
import com.m4isper.contactsapp.presentation.common.format.websiteKindLabel
import com.m4isper.contactsapp.presentation.common.share.openInstantMessage
import com.m4isper.contactsapp.presentation.common.share.openMap
import com.m4isper.contactsapp.presentation.common.share.openWebsite
import com.m4isper.contactsapp.ui.adaptive.ReadingColumn
import java.time.LocalDate

/** Поле карточки: всё, что нужно строке, собрано заранее — порядок и разделители задаёт секция. */
private data class DetailsField(
    val key: String,
    val icon: ImageVector,
    val primary: String,
    val secondary: String?,
    val actions: List<RowAction> = emptyList(),
    val onClick: (() -> Unit)? = null,
    val tag: String? = null,
)

/** Секция полей: подпись опциональна, строки идут плоским списком. */
private data class DetailsSection(
    val key: String,
    val title: String?,
    val fields: List<DetailsField>,
)

@Composable
internal fun DetailsContent(
    details: ContactDetails,
    isFavorite: Boolean,
    listState: LazyListState,
    onCallClick: (PhoneNumber) -> Unit,
    onSmsClick: (PhoneNumber) -> Unit,
    onEmailClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onHeroNameBottom: (Float) -> Unit,
    modifier: Modifier = Modifier,
    groups: List<ContactGroup> = emptyList(),
) {
    val context = LocalContext.current
    val sections =
        listOfNotNull(
            identitySection(details, groups, onCopyClick),
            phonesSection(details, onCallClick, onSmsClick, onCopyClick),
            emailSection(details, context, onEmailClick, onCopyClick),
            addressSection(details, context, onCopyClick),
            datesSection(details, context),
            relationsSection(details, onCopyClick),
            notesSection(details),
        )
    ReadingColumn(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("details_list"),
        ) {
            item(key = "hero") {
                DetailsHeader(
                    details = details,
                    isFavorite = isFavorite,
                    onCallPrimary = { details.primaryPhone?.let(onCallClick) },
                    onSmsPrimary = { details.primaryPhone?.let(onSmsClick) },
                    onEmailPrimary = { details.primaryEmail?.let { onEmailClick(it.address) } },
                    onToggleFavorite = onToggleFavorite,
                    onNameBottom = onHeroNameBottom,
                )
            }
            sections.forEach { section ->
                section.title?.let { title ->
                    item(key = "section_${section.key}") {
                        DetailsSectionHeader(
                            title = title,
                            modifier = Modifier.testTag("details_section_${section.key}"),
                        )
                    }
                }
                itemsIndexed(
                    items = section.fields,
                    key = { _, field -> field.key },
                ) { index, field ->
                    DetailsFieldRow(
                        icon = field.icon,
                        primary = field.primary,
                        secondary = field.secondary,
                        actions = field.actions,
                        onClick = field.onClick,
                        showDivider = index < section.fields.lastIndex,
                        modifier = field.tag?.let { Modifier.testTag(it) } ?: Modifier,
                    )
                }
            }
        }
    }
}

/** Учётка, организация, группы и ник: контекст контакта до его полей. */
@Composable
private fun identitySection(
    details: ContactDetails,
    groups: List<ContactGroup>,
    onCopyClick: (String) -> Unit,
): DetailsSection {
    val fields =
        buildList {
            add(
                DetailsField(
                    key = "account",
                    icon = Icons.Filled.AccountCircle,
                    primary = contactAccountLabel(details.account),
                    secondary = stringResource(R.string.editor_section_account),
                    tag = "details_account_row",
                ),
            )
            details.organization?.let { organization ->
                add(organizationField(organization))
            }
            groups.forEach { group ->
                add(
                    DetailsField(
                        key = "group_${group.id}",
                        icon = Icons.Filled.Group,
                        primary = group.title,
                        secondary =
                            listOf(
                                stringResource(R.string.group_label),
                                contactAccountLabel(group.account),
                            ).joinToString(" · "),
                        tag = "group_row_${group.title}",
                    ),
                )
            }
            details.nickname?.let { nickname ->
                add(
                    DetailsField(
                        key = "nickname",
                        icon = Icons.Filled.Badge,
                        primary = nickname,
                        secondary = stringResource(R.string.editor_section_nickname),
                        actions =
                            listOf(
                                RowAction(
                                    Icons.Filled.ContentCopy,
                                    stringResource(R.string.action_copy),
                                ) { onCopyClick(nickname) },
                            ),
                        tag = "nickname_row",
                    ),
                )
            }
        }
    return DetailsSection("about", title = null, fields = fields)
}

private fun organizationField(organization: Organization): DetailsField {
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
    return DetailsField(
        key = "organization",
        icon = Icons.Filled.Business,
        primary = primary,
        secondary = secondary,
        tag = "organization_row",
    )
}

/** Телефоны и SIP: звонок — тап по строке, SMS и копирование — кнопками. */
@Composable
private fun phonesSection(
    details: ContactDetails,
    onCallClick: (PhoneNumber) -> Unit,
    onSmsClick: (PhoneNumber) -> Unit,
    onCopyClick: (String) -> Unit,
): DetailsSection? {
    val fields =
        buildList {
            details.phones.sortedByDescending { it.isPrimary }.forEach { phone ->
                val kindLabel = phone.customLabel ?: phoneKindLabel(phone.kind)
                add(
                    DetailsField(
                        key = "phone_${phone.number}",
                        icon = Icons.Filled.Call,
                        primary = phone.number,
                        secondary =
                            if (phone.isPrimary) {
                                "$kindLabel · ${stringResource(R.string.primary_value)}"
                            } else {
                                kindLabel
                            },
                        actions =
                            listOf(
                                RowAction(
                                    Icons.AutoMirrored.Filled.Chat,
                                    stringResource(R.string.action_send_sms),
                                ) { onSmsClick(phone) },
                                RowAction(
                                    Icons.Filled.ContentCopy,
                                    stringResource(R.string.action_copy),
                                ) { onCopyClick(phone.number) },
                            ),
                        onClick = { onCallClick(phone) },
                        tag = "phone_row_${phone.number}",
                    ),
                )
            }
            details.sipAddresses.forEach { sip ->
                add(
                    DetailsField(
                        key = "sip_${sip.address}",
                        icon = Icons.Filled.Dialpad,
                        primary = sip.address,
                        secondary = sip.customLabel ?: sipKindLabel(sip.kind),
                        actions =
                            listOf(
                                RowAction(
                                    Icons.Filled.ContentCopy,
                                    stringResource(R.string.action_copy),
                                ) { onCopyClick(sip.address) },
                            ),
                        tag = "sip_row_${sip.address}",
                    ),
                )
            }
        }
    return fields.takeIf { it.isNotEmpty() }?.let {
        DetailsSection("phones", stringResource(R.string.editor_section_phones), it)
    }
}

/** Почта и мессенджеры: письмо и открытие чата остаются в строке. */
@Composable
private fun emailSection(
    details: ContactDetails,
    context: Context,
    onEmailClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
): DetailsSection? {
    val fields =
        buildList {
            details.emails.sortedByDescending { it.isPrimary }.forEach { email ->
                val kindLabel = email.customLabel ?: emailKindLabel(email.kind)
                add(
                    DetailsField(
                        key = "email_${email.address}",
                        icon = Icons.Filled.Email,
                        primary = email.address,
                        secondary =
                            if (email.isPrimary) {
                                "$kindLabel · ${stringResource(R.string.primary_value)}"
                            } else {
                                kindLabel
                            },
                        actions =
                            listOf(
                                RowAction(
                                    Icons.Filled.Email,
                                    stringResource(R.string.action_send_email),
                                ) { onEmailClick(email.address) },
                                RowAction(
                                    Icons.Filled.ContentCopy,
                                    stringResource(R.string.action_copy),
                                ) { onCopyClick(email.address) },
                            ),
                        tag = "email_row_${email.address}",
                    ),
                )
            }
            details.instantMessages.forEach { im ->
                add(
                    DetailsField(
                        key = "im_${im.address}",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        primary = im.address,
                        secondary = instantMessageLabel(im),
                        actions =
                            listOf(
                                RowAction(
                                    Icons.AutoMirrored.Filled.Chat,
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
                        tag = "im_row_${im.address}",
                    ),
                )
            }
        }
    return fields.takeIf { it.isNotEmpty() }?.let {
        DetailsSection("email", stringResource(R.string.details_section_email_messaging), it)
    }
}

/** Адреса и сайты: открыть карту или браузер и скопировать значение. */
@Composable
private fun addressSection(
    details: ContactDetails,
    context: Context,
    onCopyClick: (String) -> Unit,
): DetailsSection? {
    val fields =
        buildList {
            details.addresses.forEach { address ->
                add(
                    DetailsField(
                        key = "address_${address.displayAddress}",
                        icon = Icons.Filled.LocationOn,
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
                        tag = "address_row_${address.displayAddress}",
                    ),
                )
            }
            details.websites.forEach { website ->
                add(
                    DetailsField(
                        key = "website_${website.url}",
                        icon = Icons.Filled.Public,
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
                        tag = "website_row_${website.url}",
                    ),
                )
            }
        }
    return fields.takeIf { it.isNotEmpty() }?.let {
        DetailsSection("addresses", stringResource(R.string.details_section_address_web), it)
    }
}

/** Даты: день рождения и годовщины, каждая умеет уйти в календарь. */
@Composable
private fun datesSection(
    details: ContactDetails,
    context: Context,
): DetailsSection? {
    val fields =
        buildList {
            details.birthday?.let { birthday ->
                add(
                    DetailsField(
                        key = "birthday",
                        icon = Icons.Filled.Cake,
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
                        tag = "birthday_row",
                    ),
                )
            }
            details.events.forEach { event ->
                add(
                    DetailsField(
                        key = "event_${birthdayKey(event.date)}_${event.kind.name}",
                        icon = Icons.Filled.Event,
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
                        tag = "event_row_${birthdayKey(event.date)}",
                    ),
                )
            }
        }
    return fields.takeIf { it.isNotEmpty() }?.let {
        DetailsSection("dates", stringResource(R.string.details_section_dates), it)
    }
}

@Composable
private fun relationsSection(
    details: ContactDetails,
    onCopyClick: (String) -> Unit,
): DetailsSection? {
    val fields =
        details.relations.map { relation ->
            DetailsField(
                key = "relation_${relation.name}",
                icon = Icons.Filled.Person,
                primary = relation.name,
                secondary = relationLabel(relation),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(relation.name) },
                    ),
                tag = "relation_row_${relation.name}",
            )
        }
    return fields.takeIf { it.isNotEmpty() }?.let {
        DetailsSection("relations", stringResource(R.string.editor_section_relations), it)
    }
}

@Composable
private fun notesSection(details: ContactDetails): DetailsSection? =
    details.note?.let { note ->
        DetailsSection(
            key = "note",
            title = stringResource(R.string.editor_section_note),
            fields =
                listOf(
                    DetailsField(
                        key = "note",
                        icon = Icons.Filled.Description,
                        primary = note,
                        secondary = null,
                        tag = "note_row",
                    ),
                ),
        )
    }

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
