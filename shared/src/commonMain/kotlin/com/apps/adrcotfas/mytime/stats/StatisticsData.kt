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
package com.apps.adrcotfas.mytime.stats

import com.apps.adrcotfas.mytime.common.Time
import com.apps.adrcotfas.mytime.common.Time.toLocalDateTime
import com.apps.adrcotfas.mytime.common.toEpochMilliseconds
import com.apps.adrcotfas.mytime.data.model.Session
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class SessionOverviewData(
    // TODO: consider including the color index in this map
    val workTodayPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsToday: Long = 0,
    val workToday: Long = 0,
    val breakToday: Long = 0,
    val workThisWeekPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsThisWeek: Long = 0,
    val workThisWeek: Long = 0,
    val breakThisWeek: Long = 0,
    val workThisMonthPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsThisMonth: Long = 0,
    val workThisMonth: Long = 0,
    val breakThisMonth: Long = 0,
    val workTotalPerLabel: Map<String, Long> = emptyMap(),
    val workSessionsTotal: Long = 0,
    val workTotal: Long = 0,
    val breakTotal: Long = 0,
)

typealias HeatmapData = Map<LocalDate, Float>

typealias ProductiveHoursOfTheDay = Map<Int, Float>

data class StatisticsData(
    val overviewData: SessionOverviewData = SessionOverviewData(),
    val heatmapData: HeatmapData = emptyMap(),
    val productiveHoursOfTheDay: ProductiveHoursOfTheDay = emptyMap(),
)

fun computeStatisticsData(
    sessions: List<Session>,
    firstDayOfWeek: DayOfWeek,
    secondOfDay: Int,
): StatisticsData {
    val today = Time.startOfTodayAdjusted(secondOfDay)
    val startOfThisWeekDate = Time.startOfThisWeekAdjusted(firstDayOfWeek, secondOfDay)
    val startOfThisWeek = startOfThisWeekDate.toEpochMilliseconds()
    val startOfThisMonthDate = Time.startOfThisMonth(secondOfDay)
    val startOfThisMonth = startOfThisMonthDate.toEpochMilliseconds()

    val workTodayPerLabel = mutableMapOf<String, Long>()
    var workSessionsToday = 0L
    var workToday = 0L
    var breakToday = 0L

    val workThisWeekPerLabel = mutableMapOf<String, Long>()
    var workSessionsThisWeek = 0L
    var workThisWeek = 0L
    var breakThisWeek = 0L

    val workThisMonthPerLabel = mutableMapOf<String, Long>()
    var workSessionsThisMonth = 0L
    var workThisMonth = 0L
    var breakThisMonth = 0L

    val workTotalPerLabel = mutableMapOf<String, Long>()
    var workSessionsTotal = 0L
    var workTotal = 0L
    var breakTotal = 0L

    val todayLocalDate = Time.currentDateTime().date
    val oneYearAgoLocalDate = todayLocalDate.minus(DatePeriod(years = 1))

    val heatmapData = mutableMapOf<LocalDate, Float>()
    var maxHeatMapValue = 1f

    val productiveHoursOfTheDay =
        mutableMapOf<Int, Float>().apply {
            for (i in 0..23) {
                this[i] = 0f
            }
        }

    val oneYearAgoMillis = oneYearAgoLocalDate.toEpochMilliseconds()
    val adjustedOffsetMillis = secondOfDay.seconds.inWholeMilliseconds

    for (session in sessions) {
        val timestamp = session.timestamp
        val duration = session.duration

        if (session.isWork) {
            if (timestamp >= oneYearAgoMillis) {
                val adjustedDateTime = toLocalDateTime(timestamp - adjustedOffsetMillis)
                val date = adjustedDateTime.date
                heatmapData[date] = (heatmapData[date] ?: 0f) + duration
                maxHeatMapValue = maxOf(maxHeatMapValue, heatmapData[date] ?: 0f)

                val weight = calculateSessionWeight(timestamp, today)

                val currentSplitByHour = splitSessionByHour(toLocalDateTime(timestamp), duration)
                currentSplitByHour.forEach { (hour, value) ->
                    productiveHoursOfTheDay[hour] =
                        (productiveHoursOfTheDay[hour] ?: 0f) + value * weight
                }
            }

            if (timestamp >= today) {
                workToday += duration
                workTodayPerLabel[session.label] =
                    (workTodayPerLabel[session.label] ?: 0L) + duration
                workSessionsToday++
            }
            if (timestamp >= startOfThisWeek) {
                workThisWeek += duration
                workThisWeekPerLabel[session.label] =
                    (workThisWeekPerLabel[session.label] ?: 0L) + duration
                workSessionsThisWeek++
            }
            if (timestamp >= startOfThisMonth) {
                workThisMonth += duration
                workThisMonthPerLabel[session.label] =
                    (workThisMonthPerLabel[session.label] ?: 0L) + duration
                workSessionsThisMonth++
            }
            workTotal += duration
            workTotalPerLabel[session.label] =
                (workTotalPerLabel[session.label] ?: 0L) + duration
            workSessionsTotal++
        } else {
            if (timestamp >= today) {
                breakToday += duration
            }
            if (timestamp >= startOfThisWeek) {
                breakThisWeek += duration
            }
            if (timestamp >= startOfThisMonth) {
                breakThisMonth += duration
            }
            breakTotal += duration
        }
    }

    // normalize the values for the heatmap
    heatmapData.forEach {
        heatmapData[it.key] = (it.value / maxHeatMapValue).coerceIn(0f, 1f)
    }

    val maxProductiveHourOfTheDay = productiveHoursOfTheDay.values.maxOrNull() ?: 1f
    productiveHoursOfTheDay.forEach {
        productiveHoursOfTheDay[it.key] = (it.value / maxProductiveHourOfTheDay).coerceIn(0f, 1f)
    }

    val overviewData =
        SessionOverviewData(
            workTodayPerLabel = aggregateDataIfNeeded(workTodayPerLabel),
            workSessionsToday = workSessionsToday,
            workToday = workToday,
            breakToday = breakToday,
            workThisWeekPerLabel = aggregateDataIfNeeded(workThisWeekPerLabel),
            workSessionsThisWeek = workSessionsThisWeek,
            workThisWeek = workThisWeek,
            breakThisWeek = breakThisWeek,
            workThisMonthPerLabel = aggregateDataIfNeeded(workThisMonthPerLabel),
            workSessionsThisMonth = workSessionsThisMonth,
            workThisMonth = workThisMonth,
            breakThisMonth = breakThisMonth,
            workTotalPerLabel = aggregateDataIfNeeded(workTotalPerLabel),
            workSessionsTotal = workSessionsTotal,
            workTotal = workTotal,
            breakTotal = breakTotal,
        )

    return StatisticsData(
        overviewData = overviewData,
        heatmapData = heatmapData,
        productiveHoursOfTheDay = productiveHoursOfTheDay,
    )
}

fun splitSessionByHour(
    dateTime: LocalDateTime,
    durationMinutes: Long,
): Map<Int, Long> {
    if (durationMinutes <= 0) return emptyMap()
    val timezone = TimeZone.currentSystemDefault()
    val result = mutableMapOf<Int, Long>()
    var remainingDuration = durationMinutes
    val startDateTime =
        dateTime.toInstant(timezone).minus(durationMinutes.minutes).toLocalDateTime(timezone)

    var currentHour = startDateTime.hour
    var minutesInCurrentHour = (60 - startDateTime.minute).toLong()

    while (remainingDuration > 0) {
        val minutesToAdd = minOf(remainingDuration, minutesInCurrentHour)
        result[currentHour] = (result[currentHour] ?: 0L) + minutesToAdd

        remainingDuration -= minutesToAdd
        currentHour = (currentHour + 1) % 24
        minutesInCurrentHour = 60L
    }

    return result
}

/**
 * The older the session, the less weight it has.
 * For a session from 365 days ago, the weight is 0. For a session from today, the weight is 1.
 */
fun calculateSessionWeight(
    sessionTimestamp: Long,
    todayTimestamp: Long,
): Float {
    val daysDifference = (todayTimestamp - sessionTimestamp) / 86_400_000L
    return (365L - daysDifference).coerceIn(0L, 365L) / 365f
}
