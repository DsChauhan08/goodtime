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
import com.apps.adrcotfas.mytime.data.local.LocalDataRepository
import com.apps.adrcotfas.mytime.data.model.Session
import com.apps.adrcotfas.mytime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FinishedSessionsHandler(
    private val coroutineScope: CoroutineScope,
    private val repo: LocalDataRepository,
    private val settingsRepo: SettingsRepository,
    private val log: Logger,
) {
    fun updateLastFinishedSessionNotes(notes: String) {
        log.v { "Updating the notes" }
        coroutineScope.launch {
            try {
                val lastSessionId = settingsRepo.settings.first().lastInsertedSessionId
                log.v { "lastSessionId: $lastSessionId" }

                if (lastSessionId != Long.MAX_VALUE) {
                    val oldSession = repo.selectSessionById(lastSessionId).first()
                    val updatedSession = oldSession.copy(notes = notes)
                    repo.updateSession(lastSessionId, updatedSession)
                }
            } catch (e: Exception) {
                log.e { "Error updating notes: $e" }
                when (e) {
                    !is NoSuchElementException -> throw e
                }
            }
        }
    }

    fun updateSession(newSession: Session) {
        log.v { "Updating a finished session" }
        coroutineScope.launch {
            try {
                val lastSessionId = settingsRepo.settings.first().lastInsertedSessionId
                log.v { "lastSessionId: $lastSessionId" }
                if (lastSessionId != Long.MAX_VALUE) {
                    repo.updateSession(lastSessionId, newSession)
                }
            } catch (e: Exception) {
                log.e { "Error updating work time at reset: $e" }
            }
        }
    }

    fun saveSession(session: Session) {
        log.i { "Saving session to stats: $session" }
        coroutineScope.launch {
            val id = repo.insertSession(session)
            log.v { "Inserted session with id: $id" }
            settingsRepo.setLastInsertedSessionId(id)

            // Award focus XP for time spent focusing
            if (session.duration > 0) {
                settingsRepo.awardFocusXp(session.duration.toInt(), isTaskBonus = false)
            }

            // Link to any pending planned task for today with matching label
            try {
                val todayDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val todayEpoch = todayDate.toEpochDays()
                val plannedTasks = repo.selectPlannedTasksForDay(todayEpoch).first()
                val pendingTask = plannedTasks.firstOrNull { !it.isCompleted && it.labelName == session.label }
                if (pendingTask != null) {
                    repo.updatePlannedTask(pendingTask.copy(isCompleted = true, linkedSessionId = id))
                    // Award bonus completion XP for finishing planned task
                    settingsRepo.awardFocusXp(pendingTask.targetDurationMinutes, isTaskBonus = true)
                    log.i { "Completed planned task: ${pendingTask.title} linked to session $id" }
                }
            } catch (e: Exception) {
                log.e { "Error linking planned task: $e" }
            }
        }
    }

    fun resetLastInsertedSessionId() {
        log.v { "Resetting last inserted session id" }
        coroutineScope.launch {
            settingsRepo.setLastInsertedSessionId(Long.MAX_VALUE)
        }
    }
}
