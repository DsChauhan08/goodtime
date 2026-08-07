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
package com.apps.adrcotfas.mytime.settings.timerstyle

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.apps.adrcotfas.mytime.common.findActivity
import com.apps.adrcotfas.mytime.common.getAppLanguage
import com.apps.adrcotfas.mytime.ui.BetterListItem
import com.apps.adrcotfas.mytime.ui.CheckboxListItem
import mytime_productivity.shared.generated.resources.Res
import mytime_productivity.shared.generated.resources.settings_language
import mytime_productivity.shared.generated.resources.settings_use_dynamic_color
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun LanguageSettingsItem() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val activity = context.findActivity()
        BetterListItem(
            title = stringResource(Res.string.settings_language),
            trailing = context.getAppLanguage(),
            onClick = {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                intent.data = Uri.fromParts("package", activity?.packageName, null)
                activity?.startActivity(intent)
            },
        )
    }
}

@Composable
actual fun DynamicColorCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        CheckboxListItem(
            title = stringResource(Res.string.settings_use_dynamic_color),
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
