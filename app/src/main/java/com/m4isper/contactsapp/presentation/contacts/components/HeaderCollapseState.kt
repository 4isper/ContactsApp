package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * Скольжение домашней панели за прокруткой списка (issue 63, enter-always):
 * прокрутка списка вниз закрывает панель до нуля, прокрутка вверх раскрывает
 * её на месте, не давая списку прокрутиться раньше. Знаки — как в Compose
 * nested scroll и M3 TopAppBarScrollBehavior: положительная дельта = панель
 * раскрывается (негативное сжатие у M3), отрицательная — закрывается.
 * Состояние идемпотентно: границы — ноль и измеренная высота.
 */
internal class HeaderCollapseState {
    /** Высота содержимого панели по измерению; инсет статус-бара применяется снаружи. */
    internal var fullHeightPx by mutableFloatStateOf(0f)
        private set

    /** Насколько панель закрыта; 0 — раскрыта. */
    internal var offsetPx by mutableFloatStateOf(0f)
        private set

    internal fun onMeasured(heightPx: Int) {
        fullHeightPx = heightPx.toFloat()
        offsetPx = offsetPx.coerceIn(0f, fullHeightPx)
    }

    /** Сдвиг панели на дельту скролла: dy>0 раскрывает, dy<0 закрывает. */
    internal fun consume(scrollDeltaY: Float) {
        offsetPx = (offsetPx - scrollDeltaY).coerceIn(0f, fullHeightPx)
    }

    /**
     * Изменит ли дельта состояние панели: на границах нулевого или полного
     * сжатия одно направление уже ничего не съест.
     */
    internal fun canConsume(scrollDeltaY: Float): Boolean =
        fullHeightPx > 0f &&
            ((scrollDeltaY > 0f && offsetPx > 0f) || (scrollDeltaY < 0f && offsetPx < fullHeightPx))

    /** Полное раскрытие: смена режимов панели и программный скролл-прыжок. */
    fun expand() {
        offsetPx = 0f
    }
}

/**
 * Меняет измеренный размер панели на [HeaderCollapseState.offsetPx]:
 * контейнер уменьшается с верхнего края, содержимое уезжает вверх и
 * обрезается по верхней границе узла — панель уходит за эту границу
 * (под статус-бар, если его инсет применён снаружи узла), а список под
 * ней не дёргается. Внутри layout состояние только нормируется по
 * измеренной высоте.
 */
internal fun Modifier.collapsingHeader(state: HeaderCollapseState): Modifier =
    then(
        Modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                state.onMeasured(placeable.height)
                val naturalHeight = placeable.height.toFloat()
                val offset = state.offsetPx.coerceIn(0f, naturalHeight)
                val height = (naturalHeight - offset).roundToInt().coerceAtLeast(0)
                layout(placeable.width, height) {
                    placeable.placeRelative(0, -offset.roundToInt())
                }
            },
    )

/**
 * Nested scroll, который срабатывает раньше списка: панель реагирует на
 * дельты до того, как прокрутится сам список. [enabled] отключает сжатие,
 * когда панель заменена (поиск, выделение) или список не скроллится.
 */
@Composable
internal fun rememberHeaderCollapseConnection(
    state: HeaderCollapseState,
    enabled: Boolean,
): NestedScrollConnection =
    remember(state, enabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!enabled || !state.canConsume(available.y)) {
                    return Offset.Zero
                }
                val before = state.offsetPx
                state.consume(available.y)
                // Знак consumed совпадает со знаком дельты: из отрицательной
                // дельты съели 100px — вернули −100. Иначе список досвитится
                // дважды (available − consumed = −200).
                return Offset(0f, before - state.offsetPx)
            }
        }
    }
