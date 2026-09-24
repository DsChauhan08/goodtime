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
package com.apps.adrcotfas.mytime.data.model

data class PlannedTask(
    val id: Long = 0,
    val title: String,
    val labelName: String = Label.DEFAULT_LABEL_NAME,
    val colorIndex: Int = Label.DEFAULT_LABEL_COLOR_INDEX,
    val startTime: Long,
    val endTime: Long,
    val targetDurationMinutes: Int,
    val dayEpoch: Long,
    val isCompleted: Boolean = false,
    val linkedSessionId: Long? = null,
    val orderIndex: Long = 0,
    val notes: String = "",
)
