package com.m4isper.contactsapp.data.datasource.write

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** План слияния групп: в целевую группу попадают только новые участники. */
class GroupMergePlanTest {
    @Test
    fun `members already in the target group are skipped`() {
        val plan =
            planGroupMerge(
                sourceGroupId = 1,
                targetGroupId = 2,
                members = listOf(10, 11),
                rawContacts = mapOf(10L to 100L, 11L to 101L),
                alreadyInTarget = setOf(10L),
            )

        assertEquals(listOf(101L), plan.inserts.map { it.rawContactId })
    }

    @Test
    fun `members without a raw contact row are skipped`() {
        val plan =
            planGroupMerge(
                sourceGroupId = 1,
                targetGroupId = 2,
                members = listOf(10, 11),
                rawContacts = mapOf(10L to 100L),
                alreadyInTarget = emptySet(),
            )

        assertEquals(listOf(100L), plan.inserts.map { it.rawContactId })
    }

    @Test
    fun `repeated members are inserted once`() {
        val plan =
            planGroupMerge(
                sourceGroupId = 1,
                targetGroupId = 2,
                members = listOf(10, 10, 11),
                rawContacts = mapOf(10L to 100L, 11L to 101L),
                alreadyInTarget = emptySet(),
            )

        assertEquals(listOf(100L, 101L), plan.inserts.map { it.rawContactId })
    }

    @Test
    fun `an empty group produces a plan with only the deletion`() {
        val plan =
            planGroupMerge(
                sourceGroupId = 1,
                targetGroupId = 2,
                members = emptyList(),
                rawContacts = emptyMap(),
                alreadyInTarget = emptySet(),
            )

        assertTrue(plan.inserts.isEmpty())
        assertEquals(1L, plan.sourceGroupId)
        assertEquals(2L, plan.targetGroupId)
    }
}
