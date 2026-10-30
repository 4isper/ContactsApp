package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import kotlin.math.abs

/** Сколько шагов перемещения накопил жест: каждый пройденный порог — одна строка. */
internal fun dragSteps(
    offsetPx: Float,
    thresholdPx: Float,
): Int = if (thresholdPx <= 0f) 0 else (offsetPx / thresholdPx).toInt()

/**
 * Видимая ручка порядка: перетаскивание двигает строку на строку,
 * а для TalkBack те же перемещения доступны пользовательскими действиями.
 */
@Composable
internal fun ReorderHandle(
    testTag: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val reorderLabel = stringResource(R.string.editor_reorder_handle)
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)
    val moveUp by rememberUpdatedState(onMoveUp)
    val moveDown by rememberUpdatedState(onMoveDown)
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .testTag(testTag)
                .pointerInput(threshold) {
                    var accumulated = 0f
                    detectDragGestures(
                        onDragStart = { accumulated = 0f },
                        onDragEnd = { accumulated = 0f },
                        onDragCancel = { accumulated = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount.y
                        val steps = dragSteps(accumulated, threshold)
                        repeat(abs(steps)) {
                            if (steps > 0) moveDown() else moveUp()
                        }
                        accumulated -= steps * threshold
                    }
                }.semantics {
                    contentDescription = reorderLabel
                    customActions =
                        buildList {
                            if (canMoveUp) {
                                add(
                                    CustomAccessibilityAction(moveUpLabel) {
                                        onMoveUp()
                                        true
                                    },
                                )
                            }
                            if (canMoveDown) {
                                add(
                                    CustomAccessibilityAction(moveDownLabel) {
                                        onMoveDown()
                                        true
                                    },
                                )
                            }
                        }
                },
    ) {
        Icon(
            imageVector = Icons.Filled.DragIndicator,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
