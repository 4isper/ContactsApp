package com.m4isper.contactsapp.data.datasource.write

import com.m4isper.contactsapp.data.datasource.read.ExistingGroupMembership
import com.m4isper.contactsapp.data.datasource.write.groups.MembershipWriteOp
import com.m4isper.contactsapp.data.datasource.write.groups.planMembershipWrite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MembershipWriteOpTest {
    private fun membership(
        dataId: Long,
        groupId: Long,
        rawContactId: Long = 10,
    ) = ExistingGroupMembership(dataId = dataId, rawContactId = rawContactId, groupId = groupId)

    @Test
    fun `unselected group is deleted`() {
        val ops = planMembershipWrite(existing = listOf(membership(1, 100)), selectedGroupIds = emptySet())

        assertEquals(listOf(MembershipWriteOp.Delete(dataId = 1)), ops)
    }

    @Test
    fun `selected group missing from rows is inserted`() {
        val ops = planMembershipWrite(existing = emptyList(), selectedGroupIds = setOf(100))

        assertEquals(listOf(MembershipWriteOp.Insert(groupId = 100)), ops)
    }

    @Test
    fun `selected group rows are kept`() {
        val ops =
            planMembershipWrite(
                existing = listOf(membership(1, 100), membership(2, 200)),
                selectedGroupIds = setOf(100, 200),
            )

        assertTrue(ops.isEmpty())
    }

    @Test
    fun `all rows of an unselected group are deleted across raw contacts`() {
        // Контакт с двумя raw contacts: обе строки снятой группы должны уйти.
        val ops =
            planMembershipWrite(
                existing =
                    listOf(
                        membership(dataId = 1, groupId = 100, rawContactId = 10),
                        membership(dataId = 2, groupId = 100, rawContactId = 11),
                    ),
                selectedGroupIds = emptySet(),
            )

        assertEquals(
            listOf(MembershipWriteOp.Delete(dataId = 1), MembershipWriteOp.Delete(dataId = 2)),
            ops,
        )
    }

    @Test
    fun `kept group rows are not rewritten even with duplicates`() {
        // Выбранная группа с двумя строками не трогается: список фильтрует по множеству.
        val ops =
            planMembershipWrite(
                existing =
                    listOf(
                        membership(dataId = 1, groupId = 100, rawContactId = 10),
                        membership(dataId = 2, groupId = 100, rawContactId = 11),
                    ),
                selectedGroupIds = setOf(100),
            )

        assertTrue(ops.isEmpty())
    }

    @Test
    fun `mixed selection deletes removed and inserts added`() {
        val ops =
            planMembershipWrite(
                existing = listOf(membership(1, 100), membership(2, 200)),
                selectedGroupIds = setOf(200, 300),
            )

        assertEquals(listOf(MembershipWriteOp.Delete(dataId = 1), MembershipWriteOp.Insert(groupId = 300)), ops)
    }

    @Test
    fun `empty rows and empty selection produce no operations`() {
        assertTrue(planMembershipWrite(existing = emptyList(), selectedGroupIds = emptySet()).isEmpty())
    }
}
