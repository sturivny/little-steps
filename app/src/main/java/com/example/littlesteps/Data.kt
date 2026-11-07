package com.example.littlesteps

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

@Entity(tableName = "activities")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val icon: Int = 0,
    val position: Int = 0,
    val measured: Boolean = false,
    val goal: Double = 1.0,
    val unit: String = "",
    val days: Int = 127,
    val reminder: Int? = null,
    val created: Long = LocalDate.now().toEpochDay(),
    val archived: Long? = null
) {
    fun visible(date: LocalDate) = date.toEpochDay() >= created &&
        (archived == null || date.toEpochDay() < archived) && scheduled(date)
    fun scheduled(date: LocalDate) = days and (1 shl (date.dayOfWeek.value - 1)) != 0
}

@Entity(tableName = "entries", primaryKeys = ["habitId", "day"],
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class Entry(val habitId: Long, val day: Long, val quantity: Double)

fun isComplete(habit: Habit, entry: Entry?) = entry != null && entry.quantity >= if (habit.measured) habit.goal else 1.0
fun parseAmount(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= 1_000_000_000 }
fun autoStyle(name: String): Pair<Int, Int> {
    val n = name.lowercase()
    val icon = when {
        listOf("read", "book", "чит", "книг").any(n::contains) -> 0
        listOf("walk", "run", "sport", "заряд", "бег", "ход").any(n::contains) -> 1
        listOf("water", "drink", "вод").any(n::contains) -> 2
        listOf("learn", "english", "language", "язык", "англ", "чеш").any(n::contains) -> 3
        listOf("sleep", "сон", "спать").any(n::contains) -> 4
        listOf("craft", "draw", "рис", "рукод").any(n::contains) -> 5
        else -> Math.floorMod(name.hashCode(), 8)
    }
    return Math.floorMod(name.hashCode(), 6) to icon
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM activities ORDER BY position, id") fun observeHabits(): Flow<List<Habit>>
    @Query("SELECT * FROM entries") fun observeEntries(): Flow<List<Entry>>
    @Query("SELECT * FROM activities WHERE id = :id") suspend fun habit(id: Long): Habit?
    @Query("SELECT * FROM entries WHERE habitId = :id AND day = :day") suspend fun entry(id: Long, day: Long): Entry?
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM activities") suspend fun nextPosition(): Int
    @Insert suspend fun insert(habit: Habit): Long
    @Update suspend fun update(habit: Habit)
    @Upsert suspend fun put(entry: Entry)
    @Query("DELETE FROM entries WHERE habitId = :id AND day = :day") suspend fun clear(id: Long, day: Long)
    @Query("UPDATE activities SET position = :position WHERE id = :id") suspend fun position(id: Long, position: Int)
    @Transaction suspend fun reorder(ids: List<Long>) { ids.forEachIndexed { index, id -> position(id, index) } }
    @Transaction suspend fun toggle(habit: Habit, day: Long) {
        if (isComplete(habit, entry(habit.id, day))) clear(habit.id, day)
        else put(Entry(habit.id, day, if (habit.measured) habit.goal else 1.0))
    }
}

@Database(entities = [Habit::class, Entry::class], version = 1, exportSchema = true)
abstract class LittleDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao
    companion object {
        @Volatile private var instance: LittleDatabase? = null
        fun get(context: Context): LittleDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LittleDatabase::class.java, "little-steps.db").build().also { instance = it }
        }
    }
}
val Context.preferences by preferencesDataStore("settings")
val languageKey = stringPreferencesKey("language")

class StepsModel(application: Application) : AndroidViewModel(application) {
    private val dao = LittleDatabase.get(application).habits()
    val habits = dao.observeHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val entries = dao.observeEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val language = application.preferences.data.map { it[languageKey] ?: "en" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val failed = MutableStateFlow(false)
    private val mutex = Mutex()
    private fun action(block: suspend () -> Unit) { viewModelScope.launch {
        try { mutex.withLock { block() } } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { failed.value = true }
    } }
    fun language(value: String) = action { getApplication<Application>().preferences.edit { it[languageKey] = value } }
    fun add(name: String) = action {
        val (color, icon) = autoStyle(name)
        dao.insert(Habit(name = name.trim(), color = color, icon = icon, position = dao.nextPosition()))
    }
    fun save(habit: Habit) = action {
        dao.update(habit)
        Reminders.schedule(getApplication(), habit)
    }
    fun archive(habit: Habit) = save(habit.copy(archived = LocalDate.now().toEpochDay(), reminder = null))
    fun toggle(habit: Habit, date: LocalDate) = action { dao.toggle(habit, date.toEpochDay()) }
    fun quantity(habit: Habit, date: LocalDate, value: Double) = action {
        if (value == 0.0) dao.clear(habit.id, date.toEpochDay()) else dao.put(Entry(habit.id, date.toEpochDay(), value))
    }
    fun reorder(ids: List<Long>) = action { dao.reorder(ids) }
}
