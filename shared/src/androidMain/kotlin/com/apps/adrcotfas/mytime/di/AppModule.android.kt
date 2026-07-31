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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.apps.adrcotfas.mytime.bl.AlarmManagerHandler
import com.apps.adrcotfas.mytime.bl.DndModeManager
import com.apps.adrcotfas.mytime.bl.EventListener
import com.apps.adrcotfas.mytime.bl.TimerServiceStarter
import com.apps.adrcotfas.mytime.bl.TimerStatePersistenceListener
import com.apps.adrcotfas.mytime.bl.TimerStateRestoration
import com.apps.adrcotfas.mytime.bl.notifications.AndroidSoundPlayer
import com.apps.adrcotfas.mytime.bl.notifications.AndroidTorchManager
import com.apps.adrcotfas.mytime.bl.notifications.AndroidVibrationPlayer
import com.apps.adrcotfas.mytime.bl.notifications.FinishedNotificationHandler
import com.apps.adrcotfas.mytime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.mytime.bl.notifications.SoundVibrationAndTorchPlayer
import com.apps.adrcotfas.mytime.bl.notifications.TorchManager
import com.apps.adrcotfas.mytime.bl.notifications.VibrationPlayer
import com.apps.adrcotfas.mytime.common.AndroidFeedbackHelper
import com.apps.adrcotfas.mytime.common.AndroidInstallDateProvider
import com.apps.adrcotfas.mytime.common.AndroidTimeFormatProvider
import com.apps.adrcotfas.mytime.common.AndroidUrlOpener
import com.apps.adrcotfas.mytime.common.FeedbackHelper
import com.apps.adrcotfas.mytime.common.InstallDateProvider
import com.apps.adrcotfas.mytime.common.TimeFormatProvider
import com.apps.adrcotfas.mytime.common.UrlOpener
import com.apps.adrcotfas.mytime.data.local.DATABASE_NAME
import com.apps.adrcotfas.mytime.data.local.ProductivityDatabase
import com.apps.adrcotfas.mytime.data.local.getDatabaseBuilder
import com.apps.adrcotfas.mytime.settings.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<RoomDatabase.Builder<ProductivityDatabase>> { getDatabaseBuilder(get<Context>()) }
        single<FileSystem> { FileSystem.SYSTEM }
        single<String>(named(DB_PATH_KEY)) { getDbPath { get<Context>().getDatabasePath(DATABASE_NAME).absolutePath } }
        single<String>(named(CACHE_DIR_PATH_KEY)) { getTmpPath { get<Context>().cacheDir.absolutePath } }

        single<DataStore<Preferences>>(named(SETTINGS_NAME)) {
            getDataStore(
                producePath = { get<Context>().filesDir.resolve(SETTINGS_FILE_NAME).absolutePath },
            )
        }
        single<SoundPlayer> {
            AndroidSoundPlayer(
                context = get(),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                settingsRepo = get(),
                logger = getWith("SoundPlayer"),
            )
        }
        single<VibrationPlayer> {
            AndroidVibrationPlayer(
                context = get(),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                settingsRepo = get(),
            )
        }
        single<TorchManager> {
            AndroidTorchManager(
                context = get(),
                ioScope = get<CoroutineScope>(named(IO_SCOPE)),
                playerScope = get<CoroutineScope>(named(WORKER_SCOPE)),
                settingsRepo = get(),
                logger = getWith("TorchManager"),
            )
        }
        single<SoundVibrationAndTorchPlayer> {
            SoundVibrationAndTorchPlayer(
                soundPlayer = get(),
                vibrationPlayer = get(),
                torchManager = get(),
                timeProvider = get(),
                logger = getWith("SoundVibrationAndTorchPlayer"),
            )
        }
        single<TimerStateRestoration> {
            TimerStateRestoration(
                settingsRepo = get(),
                timeProvider = get(),
                log = getWith("TimerStateRestoration"),
            )
        }
        single<TimerStatePersistenceListener> {
            TimerStatePersistenceListener(
                settingsRepo = get(),
                timeProvider = get(),
                coroutineScope = get<CoroutineScope>(named(IO_SCOPE)),
                log = getWith("TimerStatePersistence"),
            )
        }
        single<List<EventListener>> {
            listOf(
                get<DndModeManager>(),
                get<AlarmManagerHandler>(),
                get<TimerServiceStarter>(),
                get<FinishedNotificationHandler>(),
                get<SoundVibrationAndTorchPlayer>(),
                get<TimerStatePersistenceListener>(),
            )
        }
        single<UrlOpener> { AndroidUrlOpener(get()) }
        single<FeedbackHelper> { AndroidFeedbackHelper(get()) }
        single<TimeFormatProvider> { AndroidTimeFormatProvider(get()) }
        single<InstallDateProvider> { AndroidInstallDateProvider(get()) }

        single<ReminderScheduler> {
            ReminderScheduler(
                context = get(),
                timeProvider = get(),
                logger = getWith("ReminderScheduler"),
            )
        }
    }
