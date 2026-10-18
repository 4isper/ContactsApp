package com.m4isper.contactsapp.data.datasource.write

import android.provider.ContactsContract.CommonDataKinds.Event
import com.m4isper.contactsapp.data.datasource.read.ExistingBirthday
import com.m4isper.contactsapp.data.datasource.read.ExistingEvent
import com.m4isper.contactsapp.data.datasource.read.customLabelOf
import com.m4isper.contactsapp.data.datasource.read.mapEventKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EditableEvent

/** Что делать со строкой события при сохранении формы — до превращения в операцию провайдера. */
internal sealed interface EventWriteOp {
    data class Insert(
        val event: EditableEvent,
        val date: Birthday,
    ) : EventWriteOp

    data class Update(
        val dataId: Long,
        val event: EditableEvent,
        val date: Birthday,
    ) : EventWriteOp

    /** Неразобранная дата: строка не удаляется, дата пишется обратно как есть, вид и подпись — из формы. */
    data class UpdatePreserved(
        val dataId: Long,
        val rawDate: String,
        val event: EditableEvent,
    ) : EventWriteOp

    data class Delete(
        val dataId: Long,
    ) : EventWriteOp
}

/**
 * План записи событий: строки с датой вставляются или обновляются, строки с неразобранной
 * датой сохраняются без потери (перезаписываются только при смене вида или подписи),
 * пропавшие из формы строки удаляются.
 */
internal fun planEventWriteOps(
    existing: List<ExistingEvent>,
    events: List<EditableEvent>,
): List<EventWriteOp> {
    val existingIds = existing.mapTo(mutableSetOf()) { it.dataId }
    val ops = mutableListOf<EventWriteOp>()
    events.forEach { event ->
        val date = event.date
        val dataId = event.dataId
        val unparsed = event.unparsedDate
        when {
            date != null && dataId != null && dataId in existingIds ->
                ops += EventWriteOp.Update(dataId, event, date)
            date != null -> ops += EventWriteOp.Insert(event, date)
            unparsed != null && dataId != null -> {
                val changed =
                    existing.firstOrNull { it.dataId == dataId }?.let { preservedChanged(it, event) } == true
                if (changed) ops += EventWriteOp.UpdatePreserved(dataId, unparsed, event)
            }
        }
    }
    val keptIds = events.mapNotNullTo(mutableSetOf()) { it.dataId }
    existing.forEach { row ->
        if (row.dataId !in keptIds) ops += EventWriteOp.Delete(row.dataId)
    }
    return ops
}

/** Менял ли пользователь вид или подпись события с неразобранной датой. */
private fun preservedChanged(
    row: ExistingEvent,
    event: EditableEvent,
): Boolean =
    mapEventKind(row.type) != event.kind ||
        customLabelOf(row.type, row.label, Event.TYPE_CUSTOM) != event.customLabel

/** Что делать со строкой дня рождения при сохранении формы. */
internal sealed interface BirthdayWriteOp {
    data object None : BirthdayWriteOp

    data class Insert(
        val value: Birthday,
    ) : BirthdayWriteOp

    data class Update(
        val dataId: Long,
        val value: Birthday,
    ) : BirthdayWriteOp

    data class Delete(
        val dataId: Long,
    ) : BirthdayWriteOp
}

/**
 * План записи дня рождения: пустое значение удаляет строку, но только если дата была разобрана —
 * неразобранное значение показать в редакторе нечем, а терять нельзя.
 */
internal fun planBirthdayWrite(
    existing: ExistingBirthday?,
    value: Birthday?,
): BirthdayWriteOp =
    when {
        value == null ->
            when {
                existing == null -> BirthdayWriteOp.None
                Birthday.parse(existing.raw) == null -> BirthdayWriteOp.None
                else -> BirthdayWriteOp.Delete(existing.dataId)
            }
        existing == null -> BirthdayWriteOp.Insert(value)
        existing.raw == value.toStorageString() -> BirthdayWriteOp.None
        else -> BirthdayWriteOp.Update(existing.dataId, value)
    }
