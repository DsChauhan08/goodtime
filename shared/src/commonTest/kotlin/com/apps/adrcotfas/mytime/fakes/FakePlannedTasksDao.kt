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
package com.apps.adrcotfas.mytime.fakes

import com.apps.adrcotfas.mytime.data.local.LocalPlannedTask
import com.apps.adrcotfas.mytime.data.local.PlannedTasksDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePlannedTasksDao : PlannedTasksDao {
    private val tasks = MutableStateFlow<List<LocalPlannedTask>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(task: LocalPlannedTask): Long {
        val id = if (task.id != 0L) task.id else nextId++
        val newTask = task.copy(id = id)
        tasks.value = tasks.value.filterNot { it.id == id } + newTask
        return id
    }

    override suspend fun update(task: LocalPlannedTask) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun delete(task: LocalPlannedTask) {
        tasks.value = tasks.value.filterNot { it.id == task.id }
    }

    override suspend fun deleteById(id: Long) {
        tasks.value = tasks.value.filterNot { it.id == id }
    }

    override fun selectById(id: Long): Flow<LocalPlannedTask?> =
        tasks.map { list -> list.find { it.id == id } }

    override fun selectTasksForDay(dayEpoch: Long): Flow<List<LocalPlannedTask>> =
        tasks.map { list ->
            list.filter { it.dayEpoch == dayEpoch }
                .sortedWith(compareBy({ it.startTime }, { it.orderIndex }))
        }

    override fun selectTasksForRange(startDayEpoch: Long, endDayEpoch: Long): Flow<List<LocalPlannedTask>> =
        tasks.map { list ->
            list.filter { it.dayEpoch in startDayEpoch..endDayEpoch }
                .sortedWith(compareBy({ it.dayEpoch }, { it.startTime }))
        }

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean, linkedSessionId: Long?) {
        tasks.value = tasks.value.map {
            if (it.id == id) it.copy(isCompleted = isCompleted, linkedSessionId = linkedSessionId) else it
        }
    }

    override fun countUncompletedTasksForDay(dayEpoch: Long): Flow<Int> =
        tasks.map { list -> list.count { it.dayEpoch == dayEpoch && !it.isCompleted } }

    override fun countCompletedTasksForDay(dayEpoch: Long): Flow<Int> =
        tasks.map { list -> list.count { it.dayEpoch == dayEpoch && it.isCompleted } }
}
