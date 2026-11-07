package com.example.littlesteps

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*

class UiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun reset() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking(Dispatchers.IO) {
            LittleDatabase.get(context).clearAllTables()
            context.preferences.edit { it[languageKey] = "en" }
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Today").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun createToggleAndRecreate() {
        compose.onNodeWithText("Add activity").performClick()
        compose.onNodeWithText("Name").performTextInput("Read a book")
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Read a book").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Read a book").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("1 of 1 complete").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("1 of 1 complete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Read a book").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("0 of 1 complete").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun languageChangesImmediatelyAndPersists() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Русский").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Настройки").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithText("Сегодня").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Сегодня").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun measuredQuantityAndCalendarHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = runBlocking(Dispatchers.IO) {
            LittleDatabase.get(context).habits().insert(Habit(name = "Read pages", measured = true, goal = 20.0, unit = "pages", created = java.time.LocalDate.now().minusDays(14).toEpochDay()))
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Read pages").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Read pages").performTouchInput { longClick() }
        compose.onNodeWithText("Quantity").performTextInput("12,5")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("12.5 / 20 pages").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0 of 1 complete").assertIsDisplayed()
        compose.onNodeWithContentDescription("Read pages").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("1 of 1 complete").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Previous period").performClick()
        compose.onNodeWithText("History").assertIsDisplayed()
        compose.onNodeWithText("0 of 1 complete").assertIsDisplayed()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("1 of 1 complete").assertIsDisplayed()
        runBlocking(Dispatchers.IO) { Assert.assertEquals(20.0, LittleDatabase.get(context).habits().entry(id, java.time.LocalDate.now().toEpochDay())!!.quantity, 0.0) }
    }
    @Test fun dragOrderPersistsOnDrop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ids = runBlocking(Dispatchers.IO) {
            val dao = LittleDatabase.get(context).habits()
            listOf(dao.insert(Habit(name = "First step", position = 0)), dao.insert(Habit(name = "Second step", position = 1)))
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Second step").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Reorder").performClick()
        val first = compose.onNodeWithText("First step").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Second step").fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(first.right + 22f, first.center.y), androidx.compose.ui.geometry.Offset(second.right + 22f, second.center.y), 800)
        }
        compose.waitUntil(5_000) {
            runBlocking(Dispatchers.IO) { LittleDatabase.get(context).habits().observeHabits().first().map { it.id } == ids.reversed() }
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Second step").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertTrue(compose.onNodeWithText("Second step").fetchSemanticsNode().boundsInRoot.top < compose.onNodeWithText("First step").fetchSemanticsNode().boundsInRoot.top)
    }

    @Test fun advancedSettingsSaveGoalAndUnit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = runBlocking(Dispatchers.IO) { LittleDatabase.get(context).habits().insert(Habit(name = "Daily reading")) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Daily reading").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Daily reading").performClick()
        compose.onNodeWithText("Advanced settings").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Track a quantity").performScrollTo().performClick()
        compose.onNodeWithText("Daily goal").performScrollTo().performTextReplacement("15")
        compose.onNodeWithText("Unit (optional)").performScrollTo().performTextInput("pages")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("0 / 15 pages").fetchSemanticsNodes().isNotEmpty() }
        runBlocking(Dispatchers.IO) {
            val habit = LittleDatabase.get(context).habits().habit(id)!!
            Assert.assertTrue(habit.measured); Assert.assertEquals(15.0, habit.goal, 0.0); Assert.assertEquals("pages", habit.unit)
        }
    }

}
