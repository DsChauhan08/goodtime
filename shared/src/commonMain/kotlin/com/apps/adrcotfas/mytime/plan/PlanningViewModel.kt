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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.mytime.data.local.LocalDataRepository
import com.apps.adrcotfas.mytime.data.model.Label
import com.apps.adrcotfas.mytime.data.model.PlannedTask
import com.apps.adrcotfas.mytime.data.settings.GamificationData
import com.apps.adrcotfas.mytime.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class PlanDayTab {
    TODAY,
    TOMORROW,
}

data class PlanningUiState(
    val selectedDayTab: PlanDayTab = PlanDayTab.TODAY,
    val tasks: List<PlannedTask> = emptyList(),
    val todayEpoch: Long = 0L,
    val tomorrowEpoch: Long = 0L,
    val labels: List<Label> = emptyList(),
    val gamification: GamificationData = GamificationData(),
    val uncompletedCount: Int = 0,
    val totalPlannedMinutes: Int = 0,
    val completedMinutes: Int = 0,
    val isLoading: Boolean = false,
)

class PlanningViewModel(
    private val localDataRepo: LocalDataRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PlanDayTab.TODAY)

    private fun getTodayDate() =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private val todayEpoch = getTodayDate().toEpochDays()
    private val tomorrowEpoch = getTodayDate().plus(DatePeriod(days = 1)).toEpochDays()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlanningUiState> =
        combine(
            _selectedTab,
            _selectedTab.flatMapLatest { tab ->
                val epoch = if (tab == PlanDayTab.TODAY) todayEpoch else tomorrowEpoch
                localDataRepo.selectPlannedTasksForDay(epoch)
            },
            localDataRepo.selectAllLabels(),
            settingsRepo.settings,
        ) { tab, tasks, labels, settings ->
            val uncompleted = tasks.count { !it.isCompleted }
            val totalMinutes = tasks.sumOf { it.targetDurationMinutes }
            val completedMins = tasks.filter { it.isCompleted }.sumOf { it.targetDurationMinutes }
            PlanningUiState(
                selectedDayTab = tab,
                tasks = tasks,
                todayEpoch = todayEpoch,
                tomorrowEpoch = tomorrowEpoch,
                labels = labels,
                gamification = settings.gamification,
                uncompletedCount = uncompleted,
                totalPlannedMinutes = totalMinutes,
                completedMinutes = completedMins,
                isLoading = false,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PlanningUiState(todayEpoch = todayEpoch, tomorrowEpoch = tomorrowEpoch),
        )

    fun selectDayTab(tab: PlanDayTab) {
        _selectedTab.value = tab
    }

    fun toggleTaskCompleted(task: PlannedTask) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            localDataRepo.setPlannedTaskCompleted(task.id, newCompleted)
            if (newCompleted) {
                // Award completion bonus XP!
                settingsRepo.awardFocusXp(task.targetDurationMinutes, isTaskBonus = true)
            }
        }
    }

    fun insertTask(
        title: String,
        labelName: String,
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
        notes: String = "",
    ) {
        viewModelScope.launch {
            val currentTab = _selectedTab.value
            val targetEpoch = if (currentTab == PlanDayTab.TODAY) todayEpoch else tomorrowEpoch
            val task = PlannedTask(
                title = title.ifBlank { "Focus Block" },
                labelName = labelName,
                startTime = startTime,
                endTime = endTime,
                targetDurationMinutes = durationMinutes,
                dayEpoch = targetEpoch,
                isCompleted = false,
                notes = notes,
            )
            localDataRepo.insertPlannedTask(task)
        }
    }

    fun updateTask(task: PlannedTask) {
        viewModelScope.launch {
            localDataRepo.updatePlannedTask(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            localDataRepo.deletePlannedTask(taskId)
        }
    }

    fun selectLabelForFocus(labelName: String) {
        viewModelScope.launch {
            settingsRepo.activateLabelWithName(labelName)
        }
    }
}
