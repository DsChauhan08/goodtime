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
package com.apps.adrcotfas.mytime

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.apps.adrcotfas.mytime.app.BuildConfig
import com.apps.adrcotfas.mytime.bl.AlarmManagerHandler
import com.apps.adrcotfas.mytime.bl.DndModeManager
import com.apps.adrcotfas.mytime.bl.TimeProvider
import com.apps.adrcotfas.mytime.bl.TimerManager
import com.apps.adrcotfas.mytime.bl.TimerServiceStarter
import com.apps.adrcotfas.mytime.bl.notifications.FinishedNotificationHandler
import com.apps.adrcotfas.mytime.bl.notifications.NotificationArchManager
import com.apps.adrcotfas.mytime.data.settings.SettingsRepository
import com.apps.adrcotfas.mytime.di.IO_SCOPE
import com.apps.adrcotfas.mytime.di.MAIN_SCOPE
import com.apps.adrcotfas.mytime.di.coreBackupModule
import com.apps.adrcotfas.mytime.di.coreModule
import com.apps.adrcotfas.mytime.di.coroutineScopeModule
import com.apps.adrcotfas.mytime.di.distributionModule
import com.apps.adrcotfas.mytime.di.getWith
import com.apps.adrcotfas.mytime.di.localDataModule
import com.apps.adrcotfas.mytime.di.mainModule
import com.apps.adrcotfas.mytime.di.platformModule
import com.apps.adrcotfas.mytime.di.timerManagerModule
import com.apps.adrcotfas.mytime.di.viewModelModule
import com.apps.adrcotfas.mytime.platform.Distribution
import com.apps.adrcotfas.mytime.settings.notifications.SoundsViewModel
import com.apps.adrcotfas.mytime.settings.reminders.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class MyTimeApplication :
    Application(),
    KoinComponent,
    Configuration.Provider {
    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        Distribution.isFdroid = BuildConfig.IS_FDROID
        startKoin {
            modules(
                module {
                    single<Context> { this@MyTimeApplication }
                    single<NotificationArchManager> {
                        NotificationArchManager(
                            get<Context>(),
                            MainActivity::class.java,
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }
                    single<TimerServiceStarter> {
                        TimerServiceStarter(
                            get(),
                            getWith("TimerServiceStarter"),
                        )
                    }
                    single<FinishedNotificationHandler> {
                        FinishedNotificationHandler(
                            notificationManager = get<NotificationArchManager>(),
                            timerData = { get<TimerManager>().timerData.value },
                            coroutineScope = get<CoroutineScope>(named(MAIN_SCOPE)),
                        )
                    }
                    single<AlarmManagerHandler> {
                        AlarmManagerHandler(
                            get<Context>(),
                            get<TimeProvider>(),
                            getWith("AlarmManagerHandler"),
                        )
                    }
                    viewModel<SoundsViewModel> {
                        SoundsViewModel(
                            settingsRepository = get(),
                        )
                    }

                    single<DndModeManager> {
                        DndModeManager(
                            notificationManager = get<NotificationArchManager>(),
                            settingsRepository = get<SettingsRepository>(),
                            coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                        )
                    }
                },
                coroutineScopeModule,
                platformModule,
                coreModule(isDebug = BuildConfig.DEBUG),
                localDataModule,
                coreBackupModule,
                distributionModule,
                timerManagerModule,
                viewModelModule,
                mainModule,
            )
            workManagerFactory()
        }

        val reminderManager = get<ReminderManager>()
        applicationScope.launch {
            reminderManager.init()
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            if (BuildConfig.DEBUG) {
                Configuration
                    .Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()
            } else {
                Configuration
                    .Builder()
                    .setMinimumLoggingLevel(android.util.Log.ERROR)
                    .build()
            }
}
