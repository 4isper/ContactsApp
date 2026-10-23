package com.m4isper.contactsapp.presentation.groups

import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.RenameGroupUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val family = ContactGroup(id = 100, title = "Семья")
private val work = ContactGroup(id = 200, title = "Работа")

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeGroupsRepository) =
        GroupsViewModel(
            GetGroupsUseCase(repository),
            GetGroupMembershipUseCase(repository),
            CreateGroupUseCase(repository),
            RenameGroupUseCase(repository),
            DeleteGroupUseCase(repository),
        )

    private fun items(viewModel: GroupsViewModel): List<GroupItem> =
        (viewModel.uiState.value.content as Loadable.Content).data

    @Test
    fun `load maps groups with contact counts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeGroupsRepository(
                    groups = listOf(family, work),
                    membership = mapOf(1L to setOf(100L, 200L), 2L to setOf(100L)),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load()
            advanceUntilIdle()

            assertEquals(listOf(GroupItem(family, 2), GroupItem(work, 1)), items(viewModel))
        }

    @Test
    fun `create reloads list with new group`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeGroupsRepository(groups = listOf(family))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onCreate("Друзья")
            advanceUntilIdle()

            assertEquals(
                listOf(GroupItem(family, 0), GroupItem(ContactGroup(id = 101, title = "Друзья"), 0)),
                items(viewModel),
            )
        }

    @Test
    fun `rename updates title and reloads`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeGroupsRepository(groups = listOf(family, work))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onRename(100, "Родные")
            advanceUntilIdle()

            assertEquals(listOf(100L to "Родные"), repository.renamed)
            assertEquals("Родные", items(viewModel).first().group.title)
        }

    @Test
    fun `delete removes group and reloads`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeGroupsRepository(groups = listOf(family, work))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onDelete(100)
            advanceUntilIdle()

            assertEquals(listOf(100L), repository.deleted)
            assertEquals(listOf(GroupItem(work, 0)), items(viewModel))
        }

    @Test
    fun `load with no groups emits empty state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(FakeGroupsRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Empty)
        }

    @Test
    fun `load failure shows error state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeGroupsRepository(failure = RuntimeException("boom"))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Error)
        }

    @Test
    fun `mutation failure sets error message which can be cleared`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeGroupsRepository(failure = RuntimeException("boom"))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onCreate("Друзья")
            advanceUntilIdle()
            assertEquals(AppError.UNKNOWN, viewModel.uiState.value.error)

            viewModel.onErrorHandled()
            advanceUntilIdle()
            assertEquals(null, viewModel.uiState.value.error)
        }
}
