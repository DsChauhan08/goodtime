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

import com.apps.adrcotfas.mytime.labels.addedit.AddEditLabelViewModel
import com.apps.adrcotfas.mytime.labels.main.LabelsViewModel
import com.apps.adrcotfas.mytime.main.MainViewModel
import com.apps.adrcotfas.mytime.main.TimerViewModel
import com.apps.adrcotfas.mytime.main.finishedsession.FinishedSessionViewModel
import com.apps.adrcotfas.mytime.settings.SettingsViewModel
import com.apps.adrcotfas.mytime.settings.TimerProfileViewModel
import com.apps.adrcotfas.mytime.settings.about.AboutViewModel
import com.apps.adrcotfas.mytime.settings.about.AcknowledgementsViewModel
import com.apps.adrcotfas.mytime.stats.StatisticsHistoryViewModel
import com.apps.adrcotfas.mytime.stats.StatisticsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule: Module =
    module {
        viewModelOf(::MainViewModel)
        viewModelOf(::FinishedSessionViewModel)
        viewModelOf(::LabelsViewModel)
        viewModelOf(::AddEditLabelViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::TimerProfileViewModel)
        viewModelOf(::AboutViewModel)
        viewModelOf(::AcknowledgementsViewModel)
        viewModel { StatisticsViewModel(get(), get(), get(), get()) }
        viewModel { StatisticsHistoryViewModel(get(), get()) }
    }

val mainModule: Module =
    module {
        viewModelOf(::TimerViewModel)
    }
