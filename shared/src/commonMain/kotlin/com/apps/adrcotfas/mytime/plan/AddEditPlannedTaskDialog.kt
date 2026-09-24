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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.apps.adrcotfas.mytime.data.model.Label
import com.apps.adrcotfas.mytime.data.model.PlannedTask
import com.apps.adrcotfas.mytime.data.model.getLabelData
import com.apps.adrcotfas.mytime.ui.LabelChip
import com.apps.adrcotfas.mytime.ui.SelectLabelDialog
import com.apps.adrcotfas.mytime.ui.TimePicker
import mytime_productivity.shared.generated.resources.Res
import mytime_productivity.shared.generated.resources.main_cancel
import mytime_productivity.shared.generated.resources.main_delete
import mytime_productivity.shared.generated.resources.main_ok
import mytime_productivity.shared.generated.resources.main_save
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPlannedTaskDialog(
    initialTask: PlannedTask? = null,
    labels: List<Label>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, labelName: String, durationMinutes: Int, startTimeMillis: Long, endTimeMillis: Long, notes: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var selectedLabelName by remember {
        mutableStateOf(initialTask?.labelName ?: labels.firstOrNull()?.name ?: Label.DEFAULT_LABEL_NAME)
    }
    var durationMinutes by remember {
        mutableStateOf(initialTask?.targetDurationMinutes ?: 25)
    }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }

    // Start time: if set, use hour/minute from initialTask.startTime, else default to 9:00 AM
    val initialHour = if (initialTask != null && initialTask.startTime > 0) {
        val seconds = (initialTask.startTime / 1000) % 86400
        (seconds / 3600).toInt()
    } else 9

    val initialMinute = if (initialTask != null && initialTask.startTime > 0) {
        val seconds = (initialTask.startTime / 1000) % 86400
        ((seconds % 3600) / 60).toInt()
    } else 0

    var startHour by remember { mutableStateOf(initialHour) }
    var startMinute by remember { mutableStateOf(initialMinute) }
    var hasSpecificTime by remember { mutableStateOf(initialTask?.startTime != null && initialTask.startTime > 0) }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showLabelPickerDialog by remember { mutableStateOf(false) }

    val selectedLabelColorIndex = labels.firstOrNull { it.name == selectedLabelName }?.colorIndex ?: Label.DEFAULT_LABEL_COLOR_INDEX

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (initialTask == null) "Plan Focus Block" else "Edit Planned Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.main_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Goal Name") },
                    placeholder = { Text("e.g., Deep Work, Chapter 4, Code Review") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target Duration selector
                Text(
                    text = "Target Duration",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                val durations = listOf(15, 25, 45, 60, 90)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    durations.forEach { d ->
                        val isSelected = durationMinutes == d
                        FilterChip(
                            selected = isSelected,
                            onClick = { durationMinutes = d },
                            label = { Text("${d}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Label selector
                Text(
                    text = "Label / Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabelChip(
                        name = selectedLabelName,
                        colorIndex = selectedLabelColorIndex,
                        onClick = { showLabelPickerDialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scheduled Time
                Text(
                    text = "Scheduled Time (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { showTimePickerDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val formattedTime = if (hasSpecificTime) {
                            val h = startHour.toString().padStart(2, '0')
                            val m = startMinute.toString().padStart(2, '0')
                            "$h:$m"
                        } else {
                            "Any time today"
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (hasSpecificTime) {
                        TextButton(
                            onClick = { hasSpecificTime = false },
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Key deliverables or intention") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.main_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val computedStartTime = if (hasSpecificTime) {
                                (startHour * 3600L + startMinute * 60L) * 1000L
                            } else 0L
                            val computedEndTime = if (computedStartTime > 0) {
                                computedStartTime + (durationMinutes * 60L * 1000L)
                            } else 0L

                            onConfirm(
                                title.trim().ifEmpty { "Focus Block" },
                                selectedLabelName,
                                durationMinutes,
                                computedStartTime,
                                computedEndTime,
                                notes.trim(),
                            )
                        },
                    ) {
                        Text(
                            text = if (initialTask == null) stringResource(Res.string.main_ok) else stringResource(Res.string.main_save),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // Time Picker Modal
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true,
        )
        TimePicker(
            title = "Set Start Time",
            onConfirm = { state ->
                startHour = state.hour
                startMinute = state.minute
                hasSpecificTime = true
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false },
            timePickerState = timePickerState,
        )
    }

    // Label Picker Modal
    if (showLabelPickerDialog) {
        SelectLabelDialog(
            title = "Select Label",
            singleSelection = true,
            labels = labels.map { it.getLabelData() },
            initialSelectedLabels = listOf(selectedLabelName),
            onConfirm = { selected ->
                if (selected.isNotEmpty()) {
                    selectedLabelName = selected.first()
                }
                showLabelPickerDialog = false
            },
            onDismiss = { showLabelPickerDialog = false },
        )
    }
}
