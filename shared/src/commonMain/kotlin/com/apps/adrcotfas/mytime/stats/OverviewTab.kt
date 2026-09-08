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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.mytime.bl.TimeUtils.getLocalizedMonthNamesForStats
import com.apps.adrcotfas.mytime.common.Time.currentDateTime
import com.apps.adrcotfas.mytime.common.isoWeekNumber
import com.apps.adrcotfas.mytime.data.settings.OverviewDurationType
import com.apps.adrcotfas.mytime.data.settings.OverviewType
import com.apps.adrcotfas.mytime.data.settings.StatisticsSettings
import com.apps.adrcotfas.mytime.stats.history.HistorySection
import kotlinx.datetime.DayOfWeek
import mytime_productivity.shared.generated.resources.Res
import mytime_productivity.shared.generated.resources.stats_today
import mytime_productivity.shared.generated.resources.stats_total
import mytime_productivity.shared.generated.resources.stats_week
import org.jetbrains.compose.resources.stringResource

@Composable
fun OverviewTab(
    firstDayOfWeek: DayOfWeek,
    workDayStart: Int,
    is24HourFormat: Boolean,
    statisticsSettings: StatisticsSettings,
    statisticsData: StatisticsData,
    onChangeOverviewType: (OverviewType) -> Unit,
    onChangeOverviewDurationType: (OverviewDurationType) -> Unit,
    onChangePieChartOverviewType: (OverviewDurationType) -> Unit,
    historyChartViewModel: StatisticsHistoryViewModel,
) {
    val currentDateTime = remember { currentDateTime() }
    val uiState by historyChartViewModel.uiState.collectAsStateWithLifecycle()

    val monthNames = remember { getLocalizedMonthNamesForStats() }

    val todayString = stringResource(Res.string.stats_today)
    val weekString =
        stringResource(
            Res.string.stats_week,
            currentDateTime.date.isoWeekNumber(),
        )
    val totalString = stringResource(Res.string.stats_total)

    val typeNames =
        remember(currentDateTime, monthNames, todayString, weekString, totalString) {
            mapOf(
                OverviewDurationType.TODAY to todayString,
                OverviewDurationType.THIS_WEEK to weekString,
                OverviewDurationType.THIS_MONTH to monthNames[currentDateTime.month.ordinal],
                OverviewDurationType.TOTAL to totalString,
            )
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
    ) {
        item(key = "overview") {
            OverviewSection(
                statisticsData.overviewData,
                typeNames,
                statisticsSettings.overviewType,
                onChangeOverviewType,
            )
        }

        item(key = "history") {
            HistorySection(historyChartViewModel)
        }

        item(key = "productive_time") {
            ProductiveTimeSection(
                statisticsData.productiveHoursOfTheDay,
                workDayStart,
                is24HourFormat,
            )
        }

        item(key = "heatmap") {
            HeatmapSection(
                firstDayOfWeek,
                data = statisticsData.heatmapData,
            )
        }

        if (uiState.selectedLabels.size > 1) {
            item(key = "pie_chart") {
                PieChartSection(
                    statisticsData.overviewData,
                    statisticsSettings.pieChartViewType,
                    onChangePieChartOverviewType,
                    typeNames = typeNames,
                    selectedLabels = uiState.selectedLabels,
                )
            }
        }

        item(key = "work_break_ratio") {
            WorkBreakRatioSection(
                statisticsData.overviewData,
                statisticsSettings.overviewDurationType,
                onChangeOverviewDurationType,
                typeNames = typeNames,
            )
        }
    }
}
