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
package com.apps.adrcotfas.mytime.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedTasksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: LocalPlannedTask): Long

    @Update
    suspend fun update(task: LocalPlannedTask)

    @Delete
    suspend fun delete(task: LocalPlannedTask)

    @Query("DELETE FROM plannedTask WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM plannedTask WHERE id = :id")
    fun selectById(id: Long): Flow<LocalPlannedTask?>

    @Query("SELECT * FROM plannedTask WHERE dayEpoch = :dayEpoch ORDER BY startTime ASC, orderIndex ASC")
    fun selectTasksForDay(dayEpoch: Long): Flow<List<LocalPlannedTask>>

    @Query("SELECT * FROM plannedTask WHERE dayEpoch BETWEEN :startDayEpoch AND :endDayEpoch ORDER BY dayEpoch ASC, startTime ASC")
    fun selectTasksForRange(startDayEpoch: Long, endDayEpoch: Long): Flow<List<LocalPlannedTask>>

    @Query("UPDATE plannedTask SET isCompleted = :isCompleted, linkedSessionId = :linkedSessionId WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean, linkedSessionId: Long? = null)

    @Query("SELECT COUNT(*) FROM plannedTask WHERE dayEpoch = :dayEpoch AND isCompleted = 0")
    fun countUncompletedTasksForDay(dayEpoch: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM plannedTask WHERE dayEpoch = :dayEpoch AND isCompleted = 1")
    fun countCompletedTasksForDay(dayEpoch: Long): Flow<Int>
}
