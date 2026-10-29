package com.m4isper.contactsapp.presentation.contacts.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Границы сжатия панели: дельты скролла нормируются измеренной высотой. */
class HeaderCollapseStateTest {
    @Test
    fun consumeCollapsesOnNegativeDeltaAndClamps() {
        val state = HeaderCollapseState()
        state.onMeasured(200)

        state.consume(-150f)
        assertEquals(150f, state.offsetPx)

        state.consume(-150f)
        assertEquals(200f, state.offsetPx)

        state.consume(-50f)
        assertEquals(200f, state.offsetPx)
    }

    @Test
    fun consumeExpandsOnPositiveDeltaAndClamps() {
        val state = HeaderCollapseState()
        state.onMeasured(200)
        state.consume(-140f)

        state.consume(60f)
        assertEquals(80f, state.offsetPx)

        state.consume(200f)
        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun measureShrinkClampsOffsetToNewHeight() {
        val state = HeaderCollapseState()
        state.onMeasured(200)
        state.consume(-200f)
        assertEquals(200f, state.offsetPx)

        state.onMeasured(100)
        assertEquals(100f, state.offsetPx)
    }

    @Test
    fun measuredBeforeConsumingKeepsOffsetAtZero() {
        val state = HeaderCollapseState()
        state.consume(-80f)
        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun canConsumeFollowsCollapseBoundaries() {
        val state = HeaderCollapseState()
        assertEquals(false, state.canConsume(-10f))

        state.onMeasured(200)
        assertEquals(true, state.canConsume(-10f))
        assertEquals(false, state.canConsume(10f))

        state.consume(-200f)
        assertEquals(false, state.canConsume(-10f))
        assertEquals(true, state.canConsume(10f))
    }

    @Test
    fun zeroHeightPanelConsumesNothing() {
        val state = HeaderCollapseState()
        assertEquals(false, state.canConsume(-10f))
        assertEquals(false, state.canConsume(10f))

        state.consume(-120f)
        assertEquals(0f, state.offsetPx)
    }

    @Test
    fun expandIsIdempotent() {
        val state = HeaderCollapseState()
        state.onMeasured(200)
        state.consume(-90f)

        state.expand()
        state.expand()
        assertEquals(0f, state.offsetPx)
    }
}
