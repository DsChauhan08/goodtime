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
package com.apps.adrcotfas.mytime.bl

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.apps.adrcotfas.mytime.data.local.LocalDataRepository
import com.apps.adrcotfas.mytime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.mytime.data.model.Label
import com.apps.adrcotfas.mytime.data.model.TimerProfile
import com.apps.adrcotfas.mytime.data.settings.SettingsRepository
import com.apps.adrcotfas.mytime.fakes.FakeEventListener
import com.apps.adrcotfas.mytime.fakes.FakeLabelDao
import com.apps.adrcotfas.mytime.fakes.FakeSessionDao
import com.apps.adrcotfas.mytime.fakes.FakeSettingsRepository
import com.apps.adrcotfas.mytime.fakes.FakeTimeProvider
import com.apps.adrcotfas.mytime.fakes.FakeTimerProfileDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StrictFocusTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var localDataRepo: LocalDataRepository
    private lateinit var timerManager: TimerManager
    private val timeProvider = FakeTimeProvider()
    private val fakeEventListener = FakeEventListener()
    private val logger = Logger(StaticConfig())
    private lateinit var finishedSessionsHandler: FinishedSessionsHandler

    private val defaultLabel =
        DomainLabel(
            label = Label(name = Label.DEFAULT_LABEL_NAME, colorIndex = 1),
            profile = TimerProfile(name = "default", workDuration = 25),
        )

    @BeforeTest
    fun setup() = runTest(testDispatcher) {
        timeProvider.elapsedRealtime = 0L
        settingsRepo = FakeSettingsRepository()
        localDataRepo =
            LocalDataRepositoryImpl(
                sessionDao = FakeSessionDao(),
                labelDao = FakeLabelDao(),
                timerProfileDao = FakeTimerProfileDao(),
                settingsRepo = settingsRepo,
                coroutineScope = testScope,
            )
        localDataRepo.updateDefaultLabel(defaultLabel.label)

        finishedSessionsHandler =
            FinishedSessionsHandler(
                coroutineScope = testScope,
                repo = localDataRepo,
                settingsRepo = settingsRepo,
                log = logger,
            )

        timerManager =
            TimerManager(
                localDataRepo = localDataRepo,
                settingsRepo = settingsRepo,
                listeners = listOf(fakeEventListener),
                timeProvider,
                finishedSessionsHandler,
                BreakBudgetManager(settingsRepo, timeProvider, testScope, logger),
                StreakManager(settingsRepo, timeProvider, testScope, logger),
                logger,
                coroutineScope = testScope,
                timerStateRestoration = TimerStateRestoration(settingsRepo, timeProvider, logger),
            )
        timerManager.setup()
    }

    @Test
    fun testDefaultPauseReasonIsManual() = runTest {
        assertEquals(PauseReason.MANUAL, timerManager.timerData.value.runtime.lastPauseReason)
    }

    @Test
    fun testPauseWithStrictFocusViolation() = runTest {
        timerManager.start(TimerType.FOCUS)
        assertEquals(TimerState.RUNNING, timerManager.timerData.value.runtime.state)

        timerManager.pause(PauseReason.STRICT_FOCUS_VIOLATION)
        assertEquals(TimerState.PAUSED, timerManager.timerData.value.runtime.state)
        assertEquals(PauseReason.STRICT_FOCUS_VIOLATION, timerManager.timerData.value.runtime.lastPauseReason)

        // Resuming clears or resets state to RUNNING
        timerManager.toggle()
        assertEquals(TimerState.RUNNING, timerManager.timerData.value.runtime.state)

        // Normal pause sets reason to MANUAL
        timerManager.toggle()
        assertEquals(TimerState.PAUSED, timerManager.timerData.value.runtime.state)
        assertEquals(PauseReason.MANUAL, timerManager.timerData.value.runtime.lastPauseReason)
    }

    @Test
    fun testStrictFocusSettingsToggle() = runTest {
        val initialSettings = settingsRepo.settings.first()
        assertFalse(initialSettings.uiSettings.strictFocusMode)

        settingsRepo.updateUiSettings { it.copy(strictFocusMode = true) }
        val updatedSettings = settingsRepo.settings.first()
        assertTrue(updatedSettings.uiSettings.strictFocusMode)

        settingsRepo.updateUiSettings { it.copy(strictFocusMode = false) }
        val revertedSettings = settingsRepo.settings.first()
        assertFalse(revertedSettings.uiSettings.strictFocusMode)
    }

    @Test
    fun testTimerForegroundMonitorDelegatesToStrictFocusManager() {
        var foregroundCalls = 0
        var backgroundCalls = 0

        val fakeStrictManager = object : StrictFocusManager {
            override fun onAppForegrounded() {
                foregroundCalls++
            }
            override fun onAppBackgrounded() {
                backgroundCalls++
            }
        }

        val monitor = TimerForegroundMonitor(
            timerManager = timerManager,
            timeProvider = timeProvider,
            logger = logger,
            strictFocusManager = fakeStrictManager,
        )

        monitor.onBringToForeground(testScope)
        assertEquals(1, foregroundCalls)
        assertEquals(0, backgroundCalls)

        monitor.onSendToBackground()
        assertEquals(1, foregroundCalls)
        assertEquals(1, backgroundCalls)
    }
}
