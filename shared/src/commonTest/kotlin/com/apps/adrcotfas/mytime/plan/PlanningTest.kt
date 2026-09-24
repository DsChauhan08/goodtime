/**
 *     MyTime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.mytime.plan

import com.apps.adrcotfas.mytime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.mytime.data.local.LocalLabel
import com.apps.adrcotfas.mytime.data.local.LocalPlannedTask
import com.apps.adrcotfas.mytime.data.model.Label
import com.apps.adrcotfas.mytime.data.model.PlannedTask
import com.apps.adrcotfas.mytime.fakes.FakeLabelDao
import com.apps.adrcotfas.mytime.fakes.FakePlannedTasksDao
import com.apps.adrcotfas.mytime.fakes.FakeSessionDao
import com.apps.adrcotfas.mytime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.mytime.fakes.FakeTimerProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlanningTest {

    private lateinit var plannedTasksDao: FakePlannedTasksDao
    private lateinit var labelDao: FakeLabelDao
    private lateinit var sessionDao: FakeSessionDao
    private lateinit var timerProfileDao: FakeTimerProfileDao
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var repository: LocalDataRepositoryImpl

    private val testDayEpoch = 20720L
    private val nextDayEpoch = 20721L

    @BeforeTest
    fun setup() {
        plannedTasksDao = FakePlannedTasksDao()
        labelDao = FakeLabelDao()
        sessionDao = FakeSessionDao()
        timerProfileDao = FakeTimerProfileDao()
        settingsRepo = FakeSettingsRepository()

        val dispatcher = UnconfinedTestDispatcher()
        repository = LocalDataRepositoryImpl(
            sessionDao = sessionDao,
            labelDao = labelDao,
            timerProfileDao = timerProfileDao,
            settingsRepo = settingsRepo,
            coroutineScope = kotlinx.coroutines.CoroutineScope(dispatcher),
            plannedTasksDao = plannedTasksDao,
        )
    }

    @Test
    fun testPlannedTasksDaoCrud() = runTest {
        val task = LocalPlannedTask(
            id = 1L,
            title = "Solve LeetCode",
            labelName = "code",
            startTime = 36000000L,
            endTime = 39600000L,
            targetDurationMinutes = 45,
            dayEpoch = testDayEpoch,
            isCompleted = false,
            orderIndex = 1L,
            notes = "Two pointer problems",
        )

        plannedTasksDao.insert(task)
        val dayTasks = plannedTasksDao.selectTasksForDay(testDayEpoch).first()
        assertEquals(1, dayTasks.size)
        assertEquals("Solve LeetCode", dayTasks[0].title)
        assertFalse(dayTasks[0].isCompleted)

        // Toggle completed
        plannedTasksDao.setTaskCompleted(1L, true)
        val updatedTasks = plannedTasksDao.selectTasksForDay(testDayEpoch).first()
        assertTrue(updatedTasks[0].isCompleted)

        // Delete
        plannedTasksDao.deleteById(1L)
        val remaining = plannedTasksDao.selectTasksForDay(testDayEpoch).first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun testRepositoryPlannedTasksQueryAndColorResolution() = runTest {
        // Insert custom label with colorIndex 5
        repository.insertLabel(Label(name = "study", colorIndex = 5))

        repository.insertPlannedTask(
            PlannedTask(
                title = "Read History",
                labelName = "study",
                startTime = 0L,
                endTime = 0L,
                targetDurationMinutes = 30,
                dayEpoch = testDayEpoch,
            ),
        )

        val tasks = repository.selectPlannedTasksForDay(testDayEpoch).first()
        assertEquals(1, tasks.size)
        assertEquals("Read History", tasks[0].title)
        assertEquals("study", tasks[0].labelName)
        // Verify dynamic colorIndex resolution from labelDao
        assertEquals(5, tasks[0].colorIndex)

        // Check task for different day
        val nextDayTasks = repository.selectPlannedTasksForDay(nextDayEpoch).first()
        assertTrue(nextDayTasks.isEmpty())
    }

    @Test
    fun testGamificationAwardFocusXpAndLevelUp() = runTest {
        // Initial state: Level 1, 0 XP
        val initialSettings = settingsRepo.settings.first()
        assertEquals(1, initialSettings.gamification.level)
        assertEquals(0L, initialSettings.gamification.xp)

        // Focus for 50 minutes: 50 XP
        settingsRepo.awardFocusXp(50, isTaskBonus = false)
        val stateAfter50 = settingsRepo.settings.first().gamification
        assertEquals(1, stateAfter50.level)
        assertEquals(50L, stateAfter50.xp)

        // Focus for another 60 minutes: total 110 XP -> Level 1 needs 100 XP, so levels up to Level 2 with 10 XP leftover
        settingsRepo.awardFocusXp(60, isTaskBonus = false)
        val stateAfterLevelUp = settingsRepo.settings.first().gamification
        assertEquals(2, stateAfterLevelUp.level)
        assertEquals(10L, stateAfterLevelUp.xp)
    }

    @Test
    fun testGamificationTaskBonusAndIntegrity() = runTest {
        // Start with degraded integrity for testing
        settingsRepo.updateGamification { it.copy(focusIntegrity = 80) }

        // Completing a planned task: 25 minutes + task bonus (+25 XP)
        settingsRepo.awardFocusXp(25, isTaskBonus = true)
        val updated = settingsRepo.settings.first().gamification
        assertEquals(50L, updated.xp)
        assertEquals(1, updated.tasksCompletedCount)
        // Integrity restored by 5%
        assertEquals(85, updated.focusIntegrity)
    }

    @Test
    fun testPlanningViewModelTabAndStateCalculation() = runTest {
        val vm = PlanningViewModel(repository, settingsRepo)

        // Insert task for today
        repository.insertPlannedTask(
            PlannedTask(
                title = "Task 1",
                labelName = Label.DEFAULT_LABEL_NAME,
                startTime = 0L,
                endTime = 0L,
                targetDurationMinutes = 25,
                dayEpoch = vm.uiState.value.todayEpoch,
                isCompleted = false,
            ),
        )

        // Insert completed task for today
        repository.insertPlannedTask(
            PlannedTask(
                title = "Task 2",
                labelName = Label.DEFAULT_LABEL_NAME,
                startTime = 0L,
                endTime = 0L,
                targetDurationMinutes = 35,
                dayEpoch = vm.uiState.value.todayEpoch,
                isCompleted = true,
            ),
        )

        val state = vm.uiState.first { it.tasks.size == 2 }
        assertEquals(PlanDayTab.TODAY, state.selectedDayTab)
        assertEquals(2, state.tasks.size)
        assertEquals(1, state.uncompletedCount)
        assertEquals(60, state.totalPlannedMinutes)
        assertEquals(35, state.completedMinutes)

        // Switch to tomorrow tab
        vm.selectDayTab(PlanDayTab.TOMORROW)
        val tomorrowState = vm.uiState.first { it.selectedDayTab == PlanDayTab.TOMORROW && it.tasks.isEmpty() }
        assertEquals(PlanDayTab.TOMORROW, tomorrowState.selectedDayTab)
        assertEquals(0, tomorrowState.tasks.size)
    }
}
