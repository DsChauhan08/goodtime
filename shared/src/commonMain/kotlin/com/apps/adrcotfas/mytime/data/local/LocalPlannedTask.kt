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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.apps.adrcotfas.mytime.data.model.Label.Companion.DEFAULT_LABEL_NAME

@Entity(
    tableName = "plannedTask",
    foreignKeys = [
        ForeignKey(
            entity = LocalLabel::class,
            parentColumns = ["name", "isArchived"],
            childColumns = ["labelName", "isArchived"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_DEFAULT,
        ),
        ForeignKey(
            entity = LocalSession::class,
            parentColumns = ["id"],
            childColumns = ["linkedSessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["startTime", "endTime"]),
        Index(value = ["labelName", "isArchived"]),
        Index(value = ["linkedSessionId"]),
        Index(value = ["isCompleted"]),
        Index(value = ["dayEpoch"]),
    ],
)
data class LocalPlannedTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = DEFAULT_LABEL_NAME)
    val labelName: String = DEFAULT_LABEL_NAME,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean = false,
    /** Start epoch millis of planned window */
    val startTime: Long,
    /** End epoch millis of planned window */
    val endTime: Long,
    /** Target focus duration in minutes */
    val targetDurationMinutes: Int,
    /** Epoch day number (localDate.toEpochDays()) for fast daily queries */
    val dayEpoch: Long,
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    /** Linked to actual recorded LocalSession when finished */
    val linkedSessionId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val orderIndex: Long = 0,
    @ColumnInfo(defaultValue = "")
    val notes: String = "",
)
