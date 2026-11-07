package com.example.littlesteps

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PersistenceTest {
    @Test fun entriesOrderAndArchiveSurviveReopening() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = "persistence-test.db"
        context.deleteDatabase(file)
        fun open() = Room.databaseBuilder(context, LittleDatabase::class.java, file).build()
        var db = open()
        try {
            var dao = db.habits()
            val a = dao.insert(Habit(name = "Read", position = 0, measured = true, goal = 20.0))
            val b = dao.insert(Habit(name = "Walk", position = 1))
            val day = LocalDate.now().toEpochDay()
            val habit = dao.habit(a)!!
            dao.toggle(habit, day)
            assertEquals(20.0, dao.entry(a, day)!!.quantity, 0.0)
            dao.toggle(habit, day)
            assertNull(dao.entry(a, day))
            dao.put(Entry(a, day - 1, 8.5))
            dao.reorder(listOf(b, a))
            dao.update(dao.habit(a)!!.copy(archived = day))
            db.close(); db = open(); dao = db.habits()
            assertEquals(listOf(b, a), dao.observeHabits().first().map { it.id })
            assertEquals(8.5, dao.entry(a, day - 1)!!.quantity, 0.0)
            assertEquals(day, dao.habit(a)!!.archived)
            assertNull(dao.entry(a, day))
        } finally { db.close(); context.deleteDatabase(file) }
    }
}
