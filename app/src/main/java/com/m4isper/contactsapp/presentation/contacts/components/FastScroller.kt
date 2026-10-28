package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R

private val ThumbHeight = 32.dp
private val ThumbWidth = 4.dp
private val BubbleSize = 48.dp

/** Чистый маппинг Y-координаты касания в букву индекса. */
internal fun letterIndexAt(
    y: Float,
    height: Int,
    size: Int,
): Int? {
    if (height <= 0 || size <= 0) return null
    return ((y / height) * size).toInt().coerceIn(0, size - 1)
}

/** Верх большого пальца на дорожке: 0 — начало списка, 1 — конец. */
internal fun thumbTop(
    progress: Float,
    trackHeight: Float,
    thumbHeight: Float,
): Float {
    val travel = (trackHeight - thumbHeight).coerceAtLeast(0f)
    return travel * progress.coerceIn(0f, 1f)
}

/**
 * Полоса быстрой прокрутки в стиле Android: большой палец показывает позицию,
 * пузырь с буквой появляется только во время перетаскивания. Ширина 48 dp —
 * это и цель касания, и место для TalkBack-действий.
 *
 * @param activeLetter буква под пальцем (или после jump), нужна для пузыря.
 * @param currentLetter секция, в которой стоит список; от неё TalkBack шагает дальше.
 */
@Composable
internal fun FastScroller(
    letters: List<Char>,
    activeLetter: Char?,
    currentLetter: Char?,
    scrollProgress: Float,
    barDescription: String,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    val nextLabel = stringResource(R.string.a11y_next_section)
    val previousLabel = stringResource(R.string.a11y_previous_section)
    val stateText =
        currentLetter
            ?.let { letter ->
                val index = letters.indexOf(letter)
                stringResource(R.string.a11y_section_state, letter, index + 1, letters.size)
            }.orEmpty()

    BoxWithConstraints(
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
                }.fastScrollerGestures(
                    letters = letters,
                    onDraggingChange = { dragging = it },
                    onLetter = onLetter,
                    onRelease = onRelease,
                ),
    ) {
        val maxBubbleTop = (maxHeight - BubbleSize).coerceAtLeast(0.dp)
        val thumbTopDp = thumbTop(scrollProgress, maxHeight.value, ThumbHeight.value).dp
        val bubbleTop =
            (thumbTopDp - (BubbleSize - ThumbHeight) / 2)
                .coerceIn(0.dp, maxBubbleTop)

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = thumbTopDp)
                    .width(ThumbWidth)
                    .height(ThumbHeight)
                    .clip(CircleShape)
                    .background(
                        if (activeLetter != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                    ),
        )
        if (dragging && activeLetter != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -(BubbleSize + 8.dp), y = bubbleTop)
                        .size(BubbleSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("fast_scroller_bubble"),
            ) {
                Text(
                    text = activeLetter.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * Перетаскивание по дорожке: буква под пальцем меняется — сразу прыгаем.
 * Повторную букву в одном жесте не переспрашиваем, чтобы не плодить скроллы.
 */
private fun Modifier.fastScrollerGestures(
    letters: List<Char>,
    onDraggingChange: (Boolean) -> Unit,
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
                onDraggingChange(true)
                emit(down.position.y)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) break
                    emit(change.position.y)
                    change.consume()
                } while (true)
            } finally {
                onDraggingChange(false)
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
