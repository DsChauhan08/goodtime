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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.mytime.data.model.PlannedTask
import com.apps.adrcotfas.mytime.ui.SmallLabelChip
import com.apps.adrcotfas.mytime.ui.TopBar
import com.apps.adrcotfas.mytime.ui.getLabelColor
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    onNavigateBack: () -> Unit,
    onStartFocus: (PlannedTask) -> Unit = {},
    viewModel: PlanningViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<PlannedTask?>(null) }

    Scaffold(
        topBar = {
            TopBar(
                title = "Daily Plan",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Plan Focus Block",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Plan Block") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Habitica-Style Discipline & Gamification Header
            GamificationHeader(
                level = uiState.gamification.level,
                title = getGamificationRankTitle(uiState.gamification.level),
                currentXp = uiState.gamification.xp,
                thresholdXp = uiState.gamification.xpForNextLevel,
                integrity = uiState.gamification.focusIntegrity,
                completedMinutes = uiState.completedMinutes,
                totalMinutes = uiState.totalPlannedMinutes,
            )

            // Day Selector Tabs: TODAY vs TOMORROW
            SecondaryTabRow(
                selectedTabIndex = if (uiState.selectedDayTab == PlanDayTab.TODAY) 0 else 1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = uiState.selectedDayTab == PlanDayTab.TODAY,
                    onClick = { viewModel.selectDayTab(PlanDayTab.TODAY) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Today", fontWeight = FontWeight.SemiBold)
                            if (uiState.selectedDayTab == PlanDayTab.TODAY && uiState.uncompletedCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge { Text(uiState.uncompletedCount.toString()) }
                            }
                        }
                    },
                )
                Tab(
                    selected = uiState.selectedDayTab == PlanDayTab.TOMORROW,
                    onClick = { viewModel.selectDayTab(PlanDayTab.TOMORROW) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tomorrow", fontWeight = FontWeight.SemiBold)
                            if (uiState.selectedDayTab == PlanDayTab.TOMORROW && uiState.uncompletedCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge { Text(uiState.uncompletedCount.toString()) }
                            }
                        }
                    },
                )
            }

            // Timeline / Task List
            if (uiState.tasks.isEmpty()) {
                EmptyPlanView(
                    isToday = uiState.selectedDayTab == PlanDayTab.TODAY,
                    onPlanClick = { showAddDialog = true },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.tasks,
                        key = { it.id },
                    ) { task ->
                        PlannedTaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                            onStartFocus = {
                                viewModel.selectLabelForFocus(task.labelName)
                                onStartFocus(task)
                            },
                            onClick = { taskToEdit = task },
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddEditPlannedTaskDialog(
            initialTask = null,
            labels = uiState.labels,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, labelName, durationMinutes, startTime, endTime, notes ->
                viewModel.insertTask(
                    title = title,
                    labelName = labelName,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    notes = notes,
                )
                showAddDialog = false
            },
        )
    }

    // Edit dialog
    taskToEdit?.let { task ->
        AddEditPlannedTaskDialog(
            initialTask = task,
            labels = uiState.labels,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, labelName, durationMinutes, startTime, endTime, notes ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        labelName = labelName,
                        targetDurationMinutes = durationMinutes,
                        startTime = startTime,
                        endTime = endTime,
                        notes = notes,
                    ),
                )
                taskToEdit = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                taskToEdit = null
            },
        )
    }
}

@Composable
fun GamificationHeader(
    level: Int,
    title: String,
    currentXp: Long,
    thresholdXp: Long,
    integrity: Int,
    completedMinutes: Int,
    totalMinutes: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Level and Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = level.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Level $level Disciplined Monk",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Integrity Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$integrity%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (integrity >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Focus Integrity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // XP Progress Bar
            val progress = (currentXp.toFloat() / thresholdXp.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$currentXp / $thresholdXp XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Planned: $completedMinutes / ${totalMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PlannedTaskCard(
    task: PlannedTask,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val labelColor = MaterialTheme.getLabelColor(task.colorIndex)
    val cardBackground by animateColorAsState(
        if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored label strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(84.dp)
                    .background(labelColor),
            )

            // Checkbox for completion
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = "Toggle completed",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }

            // Task info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SmallLabelChip(name = task.labelName, colorIndex = task.colorIndex)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        val timeStr = if (task.startTime > 0) {
                            val seconds = (task.startTime / 1000) % 86400
                            val h = (seconds / 3600).toString().padStart(2, '0')
                            val m = ((seconds % 3600) / 60).toString().padStart(2, '0')
                            "$h:$m (${task.targetDurationMinutes}m)"
                        } else {
                            "${task.targetDurationMinutes}m"
                        }
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Start Focus Button (Quick trigger into timer)
            if (!task.isCompleted) {
                FilledTonalIconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.padding(end = 12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPlanView(
    isToday: Boolean,
    onPlanClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isToday) "No focus blocks planned for today" else "Tomorrow is a blank canvas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pre-committing to daily blocks dramatically boosts focus integrity and earns you discipline XP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            ExtendedFloatingActionButton(
                onClick = onPlanClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Plan Focus Block") },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun getGamificationRankTitle(level: Int): String = when {
    level >= 20 -> "Focus Grandmaster"
    level >= 15 -> "Master of Flow"
    level >= 10 -> "Deep Work Sage"
    level >= 5 -> "Disciplined Warrior"
    level >= 3 -> "Focused Initiate"
    else -> "Mindful Novice"
}

