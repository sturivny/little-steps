package com.example.littlesteps

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HabitLogicTest {
    @Test fun completionUsesGoalAndToggleValue() {
        val habit = Habit(id = 7, name = "Read", measured = true, goal = 20.0)
        assertFalse(isComplete(habit, null))
        assertFalse(isComplete(habit, Entry(7, 0, 12.0)))
        assertTrue(isComplete(habit, Entry(7, 0, 20.0)))
        assertTrue(isComplete(habit, Entry(7, 0, 25.0)))
    }
    @Test fun simpleActivityNeedsOneCompletion() {
        val habit = Habit(name = "Walk")
        assertTrue(isComplete(habit, Entry(0, 0, 1.0)))
        assertFalse(isComplete(habit, Entry(0, 0, 0.0)))
    }
    @Test fun quantitiesAcceptBothDecimalSeparators() {
        assertEquals(2.5, parseAmount("2,5")!!, 0.0)
        assertEquals(2.5, parseAmount(" 2.5 ")!!, 0.0)
        listOf("0", "-1", "NaN", "Infinity", "1e309", "1000000001", "abc", "").forEach { assertNull(parseAmount(it)) }
    }
    @Test fun creationScheduleAndArchiveBoundaries() {
        val monday = LocalDate.of(2026, 10, 26)
        val habit = Habit(name = "Read", created = monday.toEpochDay(), days = 1, archived = monday.plusWeeks(1).toEpochDay())
        assertTrue(habit.visible(monday))
        assertFalse(habit.visible(monday.minusWeeks(1)))
        assertFalse(habit.visible(monday.plusDays(1)))
        assertFalse(habit.visible(monday.plusWeeks(1)))
    }
    @Test fun automaticStylesAreDeterministicAndBounded() {
        listOf("Read", "Чтение", "Water", "Зарядка", "", "🪴", "Something").forEach {
            val style = autoStyle(it)
            assertEquals(style, autoStyle(it)); assertTrue(style.first in 0..5); assertTrue(style.second in 0..7)
        }
        assertEquals(0, autoStyle("Чтение").second)
        assertEquals(2, autoStyle("Water").second)
    }
}
