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
import androidx.compose.ui.platform.testTag
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

/**
 * Алфавитный индекс справа: каждая буква секции — видимая цель касания.
 * Ширина 48 dp — это и цель касания, и место для TalkBack-действий.
 *
 * @param activeLetter буква под пальцем (или после jump).
 * @param currentLetter секция, в которой стоит список; от неё TalkBack шагает дальше.
 */
@Composable
internal fun FastScroller(
    letters: List<Char>,
    activeLetter: Char?,
    currentLetter: Char?,
    barDescription: String,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlighted = activeLetter ?: currentLetter
    val nextLabel = stringResource(R.string.a11y_next_section)
    val previousLabel = stringResource(R.string.a11y_previous_section)
    val stateText =
        currentLetter
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
                .testTag("fast_scroller")
                .semantics {
                    contentDescription = barDescription
                    stateDescription = stateText
                    customActions =
                        sectionActions(letters, currentLetter, nextLabel, previousLabel, onLetter)
                }.letterDragGestures(letters, onLetter, onRelease),
    ) {
        letters.forEach { letter ->
            AlphabetLetter(letter = letter, highlighted = letter == highlighted)
        }
    }
}

@Composable
private fun AlphabetLetter(
    letter: Char,
    highlighted: Boolean,
) {
    Text(
        text = letter.toString(),
        fontSize = 10.sp,
        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
        color =
            if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        modifier = Modifier.testTag("fast_scroller_letter_$letter"),
    )
}

/**
 * Перетаскивание по алфавиту: буква под пальцем меняется — сразу прыгаем.
 * Повторную букву в одном жесте не переспрашиваем, чтобы не плодить скроллы.
 */
private fun Modifier.letterDragGestures(
    letters: List<Char>,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
): Modifier =
    pointerInput(letters) {
        awaitEachGesture {
            var lastLetter: Char? = null

            fun emit(y: Float) {
                val letter =
                    letterIndexAt(y, size.height, letters.size)?.let { letters[it] } ?: return
                if (letter == lastLetter) return
                lastLetter = letter
                onLetter(letter)
            }
            try {
                val down = awaitFirstDown(requireUnconsumed = false)
                emit(down.position.y)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) break
                    emit(change.position.y)
                    change.consume()
                } while (true)
            } finally {
                onRelease()
            }
        }
    }

/** Кастомные действия TalkBack: перейти к следующей или предыдущей секции. */
private fun sectionActions(
    letters: List<Char>,
    currentLetter: Char?,
    nextLabel: String,
    previousLabel: String,
    onLetter: (Char) -> Unit,
): List<CustomAccessibilityAction> {
    val nextIndex = currentLetter?.let { letters.indexOf(it) + 1 } ?: 0
    val previousIndex = currentLetter?.let { letters.indexOf(it) - 1 } ?: letters.lastIndex
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
