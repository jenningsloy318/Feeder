package com.nononsenseapps.feeder.ui.compose.settings

import android.app.Application
import com.nononsenseapps.feeder.archmodel.MenuItemConfig
import com.nononsenseapps.feeder.archmodel.MenuItemType
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.archmodel.TextMenuConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

@OptIn(ExperimentalCoroutinesApi::class)
class TextSelectionMenuSettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: TextSelectionMenuSettingsViewModel
    private lateinit var repository: Repository
    private lateinit var appContext: Application
    private lateinit var di: DI

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        appContext = mockk(relaxed = true)

        val testConfig = TextMenuConfig(
            items = listOf(
                MenuItemConfig(MenuItemType.COPY, enabled = true, order = 0),
                MenuItemConfig(MenuItemType.PASTE, enabled = true, order = 1),
                MenuItemConfig(MenuItemType.CUT, enabled = true, order = 2),
            )
        )

        every { repository.textMenuConfig } returns MutableStateFlow(testConfig)

        di = DI {
            bind<Repository>() with singleton { repository }
            bind<Application>() with singleton { appContext }
        }

        viewModel = TextSelectionMenuSettingsViewModel(di)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun moveItem_withInvalidNegativeFromIndex_shouldNotCrash() = runTest {
        // This should not crash with bounds checking
        viewModel.moveItem(-1, 1)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }

    @Test
    fun moveItem_withInvalidLargeFromIndex_shouldNotCrash() = runTest {
        // This should not crash with bounds checking
        viewModel.moveItem(10, 1)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }

    @Test
    fun moveItem_withInvalidNegativeToIndex_shouldNotCrash() = runTest {
        // This should not crash with bounds checking
        viewModel.moveItem(0, -1)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }

    @Test
    fun moveItem_withInvalidLargeToIndex_shouldNotCrash() = runTest {
        // This should not crash with bounds checking
        viewModel.moveItem(0, 10)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }

    @Test
    fun moveItem_withSameFromAndToIndex_shouldNotMakeChanges() = runTest {
        // This should not make any changes
        viewModel.moveItem(1, 1)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }

    @Test
    fun moveItem_withValidIndices_shouldWorkCorrectly() = runTest {
        val expectedConfig = TextMenuConfig(
            items = listOf(
                MenuItemConfig(MenuItemType.PASTE, enabled = true, order = 0),
                MenuItemConfig(MenuItemType.COPY, enabled = true, order = 1),
                MenuItemConfig(MenuItemType.CUT, enabled = true, order = 2),
            )
        )

        coEvery { repository.setTextMenuConfig(any()) } returns Unit

        // Move item from index 1 to 0 (swap PASTE and COPY)
        viewModel.moveItem(1, 0)

        // Verify that setTextMenuConfig was called with the correct configuration
        verify(exactly = 1) { repository.setTextMenuConfig(expectedConfig) }
    }

    @Test
    fun moveItemUp_withInvalidIndex_shouldNotCrash() = runTest {
        // This should not crash with bounds checking
        viewModel.moveItemUp(10)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }

    @Test
    fun moveItemDown_withInvalidIndex_shouldNotCrash() = runTest {
        // This should not crash with bounds checking
        viewModel.moveItemDown(-1)

        // Verify that setTextMenuConfig was not called (no changes made)
        verify(exactly = 0) { repository.setTextMenuConfig(any()) }
    }
}