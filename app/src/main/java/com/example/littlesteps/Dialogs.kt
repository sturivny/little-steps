package com.example.littlesteps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AddDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(s(R.string.new_activity)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text(s(R.string.name)) }, placeholder = { Text(s(R.string.name_hint)) }, singleLine = true)
            Text(s(R.string.auto_style), color = ink.copy(alpha = .6f))
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name.trim()) }) { Text(s(R.string.add)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } })
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, language: String, onLanguage: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(s(R.string.settings)) }, text = {
        Column {
            Text(s(R.string.language), fontWeight = FontWeight.SemiBold)
            listOf("en" to "English", "ru" to "Русский").forEach { (code, label) ->
                Row(Modifier.fillMaxWidth().clickable { onLanguage(code) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = language == code, onClick = { onLanguage(code) }); Text(label) }
            }
            Spacer(Modifier.height(16.dp)); Text(s(R.string.offline), color = ink.copy(alpha = .6f), fontSize = 13.sp)
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(s(R.string.done)) } })
}

@Composable
fun QuantityDialog(habit: Habit, current: Double?, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by rememberSaveable(habit.id) { mutableStateOf(current?.let(::number) ?: "") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(habit.name) }, text = {
        Column {
            Text(s(R.string.quantity_for), modifier = Modifier.padding(bottom = 12.dp))
            OutlinedTextField(amount, { amount = it; error = false }, label = { Text(s(R.string.quantity)) }, suffix = { Text(habit.unit) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = error)
            if (error) Text(s(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { onSave(0.0) }) { Text(s(R.string.clear)) }
        }
    }, confirmButton = { TextButton(onClick = { val value = parseAmount(amount); if (value == null) error = true else onSave(value) }) { Text(s(R.string.save)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } })
}

@Composable
fun EditDialog(habit: Habit, locale: Locale, onDismiss: () -> Unit, onSave: (Habit) -> Unit, onArchive: () -> Unit) {
    var name by rememberSaveable(habit.id) { mutableStateOf(habit.name) }
    var color by rememberSaveable(habit.id) { mutableIntStateOf(habit.color) }
    var icon by rememberSaveable(habit.id) { mutableIntStateOf(habit.icon) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var measured by rememberSaveable { mutableStateOf(habit.measured) }
    var goal by rememberSaveable { mutableStateOf(number(habit.goal)) }
    var unit by rememberSaveable { mutableStateOf(habit.unit) }
    var days by rememberSaveable { mutableIntStateOf(habit.days) }
    var reminder by rememberSaveable { mutableStateOf(habit.reminder != null) }
    var time by rememberSaveable { mutableStateOf(String.format(Locale.ROOT, "%02d:%02d", (habit.reminder ?: 540) / 60, (habit.reminder ?: 540) % 60)) }
    var validation by remember { mutableIntStateOf(0) }
    var archive by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val measuredLabel = s(R.string.measured)
    val reminderLabel = s(R.string.reminder)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionDenied = !it }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.fillMaxWidth().heightIn(max = 760.dp)) {
            Column(Modifier.imePadding().padding(20.dp)) {
                Text(s(R.string.activity_settings), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(name, { name = it.take(80) }, label = { Text(s(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(s(R.string.color), fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.forEachIndexed { i, value ->
                            val label = s(colorNames[i])
                            Box(Modifier.size(40.dp).clip(CircleShape).background(value.copy(alpha = .15f)).clickable { color = i }.semantics { contentDescription = label; selected = color == i }, contentAlignment = Alignment.Center) {
                                Box(Modifier.size(25.dp).background(value, CircleShape))
                                if (color == i) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                    Text(s(R.string.icon), fontSize = 13.sp)
                    symbols.chunked(4).forEachIndexed { row, chunk ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            chunk.forEachIndexed { col, symbol ->
                                val index = row * 4 + col
                                IconToggleButton(checked = icon == index, onCheckedChange = { icon = index }) { Icon(symbol, s(iconNames[index]), tint = if (icon == index) accent else ink.copy(alpha = .4f)) }
                            }
                        }
                    }
                    TextButton(onClick = { advanced = !advanced }) { Text(s(R.string.advanced)); Icon(if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null) }
                    if (advanced) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(s(R.string.measured), Modifier.weight(1f)); Switch(measured, { measured = it }, modifier = Modifier.semantics { contentDescription = measuredLabel }) }
                        if (measured) {
                            Text(s(R.string.measured_hint), fontSize = 12.sp, color = ink.copy(alpha = .6f))
                            OutlinedTextField(goal, { goal = it }, label = { Text(s(R.string.goal)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(unit, { unit = it.take(24) }, label = { Text(s(R.string.unit)) }, placeholder = { Text(s(R.string.unit_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        Text(s(R.string.schedule), fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            (1..7).forEach { day ->
                                val chosen = days and (1 shl (day - 1)) != 0
                                val full = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, locale)
                                Box(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(if (chosen) accent.copy(alpha = .12f) else Color.Transparent).clickable { days = days xor (1 shl (day - 1)) }.semantics { contentDescription = full; selected = chosen }, contentAlignment = Alignment.Center) {
                                    Text(DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, locale), fontSize = 13.sp, color = if (chosen) accent else ink.copy(alpha = .4f))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(s(R.string.reminder), Modifier.weight(1f)); Switch(reminder, {
                            reminder = it
                            if (it && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }, modifier = Modifier.semantics { contentDescription = reminderLabel }) }
                        if (reminder) {
                            OutlinedTextField(time, { time = it.take(5) }, label = { Text(s(R.string.time)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), modifier = Modifier.fillMaxWidth())
                            Text(s(R.string.reminder_note), fontSize = 12.sp, color = ink.copy(alpha = .6f))
                            if (permissionDenied || (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) Text(s(R.string.notification_denied), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (validation != 0) Text(s(validation), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { archive = true }) { Text(s(R.string.archive), color = MaterialTheme.colorScheme.error) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) }
                    TextButton(enabled = name.isNotBlank(), onClick = {
                        val value = parseAmount(goal)
                        val parsedTime = runCatching { java.time.LocalTime.parse(time, java.time.format.DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.getOrNull()
                        validation = when { measured && value == null -> R.string.invalid_amount; days == 0 -> R.string.select_day; reminder && parsedTime == null -> R.string.invalid_time; else -> 0 }
                        if (validation == 0) onSave(habit.copy(name = name.trim(), color = color, icon = icon, measured = measured, goal = value ?: 1.0, unit = unit.trim(), days = days, reminder = if (reminder) parsedTime!!.hour * 60 + parsedTime.minute else null))
                    }) { Text(s(R.string.save)) }
                }
            }
        }
    }
    if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text(s(R.string.archive_title)) }, text = { Text(s(R.string.archive_body)) }, confirmButton = { TextButton(onClick = onArchive) { Text(s(R.string.archive)) } }, dismissButton = { TextButton(onClick = { archive = false }) { Text(s(R.string.cancel)) } })
}
