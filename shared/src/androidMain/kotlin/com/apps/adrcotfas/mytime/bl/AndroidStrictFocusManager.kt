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

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.mytime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AndroidStrictFocusManager(
    private val context: Context,
    private val timerManager: TimerManager,
    private val settingsRepo: SettingsRepository,
    private val coroutineScope: CoroutineScope,
    private val log: Logger,
) : StrictFocusManager {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private var isScreenOff = false
    private var isAppInForeground = false
    private var evaluationJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    log.d { "StrictFocusManager: Screen turned OFF" }
                    isScreenOff = true
                    evaluationJob?.cancel()
                    evaluationJob = null
                }
                Intent.ACTION_SCREEN_ON -> {
                    log.d { "StrictFocusManager: Screen turned ON" }
                    isScreenOff = false
                    scheduleEvaluation()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        context.registerReceiver(screenReceiver, filter)
    }

    override fun onAppForegrounded() {
        log.v { "StrictFocusManager: onAppForegrounded" }
        isAppInForeground = true
        evaluationJob?.cancel()
        evaluationJob = null
    }

    override fun onAppBackgrounded() {
        log.v { "StrictFocusManager: onAppBackgrounded" }
        isAppInForeground = false
        scheduleEvaluation()
    }

    private fun scheduleEvaluation() {
        evaluationJob?.cancel()
        evaluationJob = coroutineScope.launch {
            // Latch delay to avoid race conditions on OEM devices where onStop/onPause
            // fires slightly before ACTION_SCREEN_OFF or PowerManager state flips
            delay(SCREEN_OFF_DEBOUNCE_MS)
            evaluateBackgroundState()
        }
    }

    private suspend fun evaluateBackgroundState() {
        val timerData = timerManager.timerData.value
        if (!timerData.runtime.state.isRunning || !timerData.runtime.type.isFocus) {
            return
        }

        val settings = settingsRepo.settings.first()
        if (!settings.uiSettings.strictFocusMode) {
            return
        }

        val isInteractive = powerManager.isInteractive
        val isLocked = keyguardManager?.isKeyguardLocked ?: false

        log.d { "Evaluating strict focus: interactive=$isInteractive, screenOff=$isScreenOff, locked=$isLocked, inForeground=$isAppInForeground" }

        // If screen is off or device is locked, user put phone away to focus -> ALLOWED
        if (!isInteractive || isScreenOff || isLocked) {
            log.i { "Strict focus: device locked or screen off, timer continues normally." }
            return
        }

        // Screen is on and interactive while app is not in foreground -> VIOLATION
        if (!isAppInForeground) {
            log.w { "Strict focus violation: App was left while screen was interactive! Pausing timer." }
            timerManager.pause(reason = PauseReason.STRICT_FOCUS_VIOLATION)
        }
    }

    companion object {
        private const val SCREEN_OFF_DEBOUNCE_MS = 300L
    }
}
