package com.example.littlesteps

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val ink = Color(0xFF263D35)
val accent = Color(0xFF40745E)
val canvas = Color(0xFFF6F8F7)
val colors = listOf(0xFF55956F,0xFF598DCB,0xFFDE9A46,0xFF9A7CCD,0xFFD77FA0,0xFF45A59D).map { Color(it) }
val symbols = listOf(Icons.AutoMirrored.Rounded.MenuBook,Icons.AutoMirrored.Rounded.DirectionsWalk,Icons.Rounded.WaterDrop,Icons.Rounded.Translate,Icons.Rounded.Bedtime,Icons.Rounded.Palette,Icons.Rounded.FavoriteBorder,Icons.Rounded.Spa)
val iconNames = listOf(R.string.icon_book,R.string.icon_walk,R.string.icon_water,R.string.icon_learn,R.string.icon_sleep,R.string.icon_craft,R.string.icon_heart,R.string.icon_leaf)
val colorNames = listOf(R.string.color_green,R.string.color_blue,R.string.color_orange,R.string.color_purple,R.string.color_pink,R.string.color_teal)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: StepsModel = viewModel()
            val language by model.language.collectAsStateWithLifecycle()
            val base = LocalContext.current
            val config = LocalConfiguration.current
            if (language != null) {
                val locale = Locale.forLanguageTag(language!!)
                val localized = remember(language, config) {
                    base.createConfigurationContext(Configuration(config).apply { setLocale(locale) })
                }
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides this@MainActivity, LocalContext provides localized, LocalConfiguration provides localized.resources.configuration, LocalResources provides localized.resources) {
                    MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = canvas, surface = Color.White, onSurface = ink, onBackground = ink)) {
                        StepsApp(model, locale)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsApp(model: StepsModel, locale: Locale) {
    val habits by model.habits.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    LaunchedEffect(Unit) { while (true) {
        delay(30_000)
        val next = LocalDate.now()
        if (selectedDay == today.toEpochDay()) selectedDay = next.toEpochDay()
        today = next
    } }
    val date = LocalDate.ofEpochDay(selectedDay)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var add by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var quantity by remember { mutableStateOf<Habit?>(null) }
    var reorder by remember { mutableStateOf(false) }
    var orderedIds by remember { mutableStateOf(listOf<Long>()) }
    var dragged by remember { mutableStateOf<Long?>(null) }
    val active = habits.filter { it.visible(date) }
    val displayed = if (reorder) orderedIds.mapNotNull { id -> active.find { it.id == id } } else active
    val dayEntries = entries.filter { it.day == selectedDay }.associateBy { it.habitId }
    val count = active.count { isComplete(it, dayEntries[it.id]) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val error by model.failed.collectAsStateWithLifecycle()
    val errorText = s(R.string.error_save)
    LaunchedEffect(error) { if (error) { snackbar.showSnackbar(errorText); model.failed.value = false } }
    val currentDisplayed by rememberUpdatedState(displayed)
    fun persistOrder() {
        // Include hidden/scheduled-off habits so the global order stays unique.
        val visibleIds = orderedIds.toSet()
        val iterator = orderedIds.iterator()
        val merged = habits.map { if (it.id in visibleIds && iterator.hasNext()) iterator.next() else it.id }
        model.reorder(merged)
    }
    val saveOrder by rememberUpdatedState({ persistOrder() })
    Scaffold(containerColor = canvas, snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text(s(R.string.app_name), fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = canvas),
                actions = { IconButton(onClick = { settings = true }) { Icon(Icons.Rounded.Settings, s(R.string.settings)) } })
        },
        floatingActionButton = { if (!reorder && date == today) FloatingActionButton(onClick = { add = true }, containerColor = accent, contentColor = Color.White) { Icon(Icons.Rounded.Add, s(R.string.add_activity)) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s(if (date == today) R.string.today else R.string.history), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(s(R.string.tagline), color = ink.copy(alpha = .6f), fontSize = 14.sp)
                }
                if (date != today) TextButton(onClick = { selectedDay = today.toEpochDay(); reorder = false }) { Text(s(R.string.today)) }
            }
            Spacer(Modifier.height(16.dp))
            CalendarPanel(date, today, expanded, locale, habits, entries,
                onSelect = { selectedDay = it.toEpochDay(); reorder = false }, onExpand = { expanded = !expanded })
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s(R.string.progress, count, active.size), Modifier.weight(1f), color = ink.copy(alpha = .65f), fontSize = 13.sp)
                if (active.size > 1 && date <= today) TextButton(onClick = {
                    if (reorder) { persistOrder(); reorder = false } else { orderedIds = active.map { it.id }; reorder = true }
                }) { Text(s(if (reorder) R.string.done else R.string.reorder)) }
            }
            if (reorder) Text(s(R.string.reorder_hint), fontSize = 12.sp, color = accent, modifier = Modifier.padding(bottom = 8.dp))
            if (date > today) Text(s(R.string.future_note), fontSize = 12.sp, color = accent, modifier = Modifier.padding(bottom = 8.dp))
            if (active.isEmpty()) {
                Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Spa, null, tint = accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(s(if (habits.isEmpty()) R.string.empty_title else R.string.empty_day), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    if (date == today) { Text(s(R.string.empty_body), textAlign = TextAlign.Center, color = ink.copy(alpha = .6f), modifier = Modifier.padding(vertical = 12.dp)); Button(onClick = { add = true }) { Text(s(R.string.add_activity)) } }
                }
            } else LazyColumn(state = list, userScrollEnabled = !reorder, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.weight(1f).pointerInput(reorder) {
                    if (reorder) detectDragGestures(onDragStart = { point ->
                        dragged = list.layoutInfo.visibleItemsInfo.find { point.y >= it.offset && point.y < it.offset + it.size }?.key as? Long
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }, onDragEnd = { dragged = null; saveOrder() }, onDragCancel = { dragged = null; saveOrder() }, onDrag = { change, _ ->
                        change.consume()
                        val id = dragged
                        val target = list.layoutInfo.visibleItemsInfo.find { change.position.y > it.offset + it.size * .25f && change.position.y < it.offset + it.size * .75f }?.key as? Long
                        if (id != null && target != null && id != target) {
                            val ids = currentDisplayed.map { it.id }.toMutableList()
                            val from = ids.indexOf(id); val to = ids.indexOf(target)
                            if (from >= 0 && to >= 0) { ids.removeAt(from); ids.add(to, id); orderedIds = ids }
                        }
                        if (change.position.y < 60) scope.launch { list.scrollBy(-18f) }
                        if (change.position.y > size.height - 60) scope.launch { list.scrollBy(18f) }
                    })
                }) {
                items(displayed, key = { it.id }) { habit ->
                    HabitCard(habit, dayEntries[habit.id], reorder, dragged == habit.id, date <= today,
                        onEdit = { editing = habit }, onToggle = { model.toggle(habit, date); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onQuantity = { if (habit.measured) { quantity = habit; haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }, modifier = Modifier.animateItem())
                }
            }
        }
    }
    if (add) AddDialog(onDismiss = { add = false }, onAdd = { model.add(it); add = false })
    if (settings) SettingsDialog(onDismiss = { settings = false }, language = locale.language, onLanguage = model::language)
    editing?.let { habit -> EditDialog(habit, locale, onDismiss = { editing = null }, onSave = { model.save(it); editing = null }, onArchive = { model.archive(habit); editing = null }) }
    quantity?.let { habit -> QuantityDialog(habit, dayEntries[habit.id]?.quantity, onDismiss = { quantity = null }, onSave = { model.quantity(habit, date, it); quantity = null }) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(habit: Habit, entry: Entry?, reorder: Boolean, dragged: Boolean, enabled: Boolean, onEdit: () -> Unit, onToggle: () -> Unit, onQuantity: () -> Unit, modifier: Modifier = Modifier) {
    val color = colors[habit.color.mod(colors.size)]
    val complete = isComplete(habit, entry)
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = if (dragged) 8.dp else 0.dp,
        border = if (dragged) BorderStroke(1.dp, accent) else null) {
        Row(Modifier.padding(16.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(symbols[habit.icon.mod(symbols.size)], null, tint = color, modifier = Modifier.size(25.dp)) }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(enabled = !reorder, onClick = onEdit).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(habit.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                if (habit.measured) Text("${number(entry?.quantity ?: 0.0)} / ${number(habit.goal)} ${habit.unit}", color = ink.copy(alpha = .5f), fontSize = 12.sp)
            }
            if (reorder) Icon(Icons.Rounded.DragHandle, s(R.string.reorder), tint = ink.copy(alpha = .45f), modifier = Modifier.size(48.dp))
            else Box(Modifier.size(48.dp).clip(CircleShape).combinedClickable(enabled = enabled, onClick = onToggle, onLongClick = if (habit.measured) onQuantity else null)
                .semantics { contentDescription = habit.name; stateDescription = if (complete) "✓" else "○"; role = Role.Checkbox }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(if (complete) color else Color.Transparent).border(1.8.dp, if (complete) color else Color(0xFFDCE3DF), CircleShape), contentAlignment = Alignment.Center) {
                    if (complete) Icon(Icons.Rounded.Check, s(R.string.undo_complete), tint = Color.White, modifier = Modifier.size(19.dp))
                    else Icon(Icons.Rounded.Add, s(R.string.complete), tint = color.copy(alpha = .3f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
fun number(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
