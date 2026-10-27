package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m4isper.contactsapp.R

/** Чистый маппинг Y-координаты касания в букву индекса. */
internal fun letterIndexAt(
    y: Float,
    height: Int,
    size: Int,
): Int? {
    if (height <= 0 || size <= 0) return null
    return ((y / height) * size).toInt().coerceIn(0, size - 1)
}

@Composable
internal fun FastScroller(
    letters: List<Char>,
    activeLetter: Char?,
    barDescription: String,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun letterAt(
        y: Float,
        height: Int,
    ): Char? = letterIndexAt(y, height, letters.size)?.let { letters[it] }

    val nextLabel = stringResource(R.string.a11y_next_section)
    val previousLabel = stringResource(R.string.a11y_previous_section)
    val stateText =
        activeLetter
            ?.let { letter ->
                val index = letters.indexOf(letter)
                stringResource(R.string.a11y_section_state, letter, index + 1, letters.size)
            }.orEmpty()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .fillMaxHeight()
                .width(48.dp)
                .semantics {
                    contentDescription = barDescription
                    stateDescription = stateText
                    customActions =
                        sectionActions(letters, activeLetter, nextLabel, previousLabel, onLetter)
                }.pointerInput(letters) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        letterAt(down.position.y, size.height)?.let(onLetter)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            letterAt(change.position.y, size.height)?.let(onLetter)
                            change.consume()
                        } while (true)
                        onRelease()
                    }
                },
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Normal,
                color =
                    if (letter == activeLetter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

/** Кастомные действия TalkBack: перейти к следующей или предыдущей секции. */
private fun sectionActions(
    letters: List<Char>,
    activeLetter: Char?,
    nextLabel: String,
    previousLabel: String,
    onLetter: (Char) -> Unit,
): List<CustomAccessibilityAction> {
    val nextIndex = activeLetter?.let { letters.indexOf(it) + 1 } ?: 0
    val previousIndex = activeLetter?.let { letters.indexOf(it) - 1 } ?: letters.lastIndex
    return listOf(
        CustomAccessibilityAction(nextLabel) {
            letters.getOrNull(nextIndex)?.let(onLetter)
            true
        },
        CustomAccessibilityAction(previousLabel) {
            letters.getOrNull(previousIndex)?.let(onLetter)
            true
        },
    )
}
