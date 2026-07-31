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
package com.apps.adrcotfas.mytime.di

import com.apps.adrcotfas.mytime.bl.BreakBudgetManager
import com.apps.adrcotfas.mytime.bl.EventListener
import com.apps.adrcotfas.mytime.bl.FinishedSessionsHandler
import com.apps.adrcotfas.mytime.bl.StreakManager
import com.apps.adrcotfas.mytime.bl.TimeProvider
import com.apps.adrcotfas.mytime.bl.TimerForegroundMonitor
import com.apps.adrcotfas.mytime.bl.TimerManager
import com.apps.adrcotfas.mytime.bl.TimerStateRestoration
import com.apps.adrcotfas.mytime.data.local.LocalDataRepository
import com.apps.adrcotfas.mytime.data.settings.SettingsRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val timerManagerModule =
    module {
        single<BreakBudgetManager> {
            BreakBudgetManager(
                settingsRepo = get(),
                timeProvider = get(),
                coroutineScope = get(named(IO_SCOPE)),
                log = getWith("BreakBudgetManager"),
            )
        }

        single<StreakManager> {
            StreakManager(
                settingsRepo = get(),
                timeProvider = get(),
                coroutineScope = get(named(IO_SCOPE)),
                log = getWith("StreakManager"),
            )
        }

        single<TimerManager> {
            TimerManager(
                get<LocalDataRepository>(),
                get<SettingsRepository>(),
                get<List<EventListener>>(),
                get<TimeProvider>(),
                get<FinishedSessionsHandler>(),
                get<BreakBudgetManager>(),
                get<StreakManager>(),
                getWith("TimerManager"),
                coroutineScope = get(named(IO_SCOPE)),
                timerStateRestoration = get<TimerStateRestoration>(),
            )
        }

        single {
            TimerForegroundMonitor(
                timerManager = get(),
                timeProvider = get(),
                getWith("TimerForegroundMonitor"),
            )
        }
    }
