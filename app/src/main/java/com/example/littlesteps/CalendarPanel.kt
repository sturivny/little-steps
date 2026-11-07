package com.example.littlesteps

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.time.*
import java.time.format.*
import java.util.Locale

@Composable
fun CalendarPanel(date: LocalDate, today: LocalDate, expanded: Boolean, locale: Locale, habits: List<Habit>, entries: List<Entry>, onSelect: (LocalDate) -> Unit, onExpand: () -> Unit) {
    val month = YearMonth.from(date)
    val start = if (expanded) month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value - 1).toLong()) else date.minusDays((date.dayOfWeek.value - 1).toLong())
    val cells = if (expanded) ((month.lengthOfMonth() + month.atDay(1).dayOfWeek.value - 2) / 7 + 1) * 7 else 7
    val dots = remember(entries, habits) {
        val map = habits.associateBy { it.id }
        entries.filter { map[it.habitId]?.let { habit -> isComplete(habit, it) } == true }.groupBy { it.day }
    }
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSelect(if (expanded) date.minusMonths(1) else date.minusWeeks(1)) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, s(R.string.previous)) }
                Text(date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onExpand) { Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, s(if (expanded) R.string.week else R.string.month)) }
                IconButton(onClick = { onSelect(if (expanded) date.plusMonths(1) else date.plusWeeks(1)) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, s(R.string.next)) }
            }
            Row(Modifier.fillMaxWidth()) {
                (1..7).forEach { day -> Text(DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = ink.copy(alpha = .45f), fontSize = 11.sp) }
            }
            repeat(cells / 7) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val day = start.plusDays((row * 7 + col).toLong())
                        val selected = day == date
                        val faded = expanded && YearMonth.from(day) != month
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSelect(day) }.padding(vertical = 6.dp)
                            .semantics { contentDescription = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)); this.selected = selected }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(if (selected) accent else Color.Transparent)
                                .then(if (day == today && !selected) Modifier.border(1.dp, accent, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                                Text(day.dayOfMonth.toString(), color = if (selected) Color.White else ink.copy(alpha = if (faded) .3f else 1f), fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                            Row(Modifier.height(7.dp).padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                dots[day.toEpochDay()].orEmpty().take(3).forEach { entry ->
                                    val habit = habits.find { it.id == entry.habitId }
                                    Box(Modifier.size(4.dp).background(colors[(habit?.color ?: 0).mod(colors.size)], CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
