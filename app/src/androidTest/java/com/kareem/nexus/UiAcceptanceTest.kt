package com.kareem.nexus

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test

class UiAcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun captureSearchNavigateAndRecreate() {
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Text or link").performTextInput("موعد أحمد بكرة — Please confirm meeting")

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Save to Memory") and isEnabled(), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Save to Memory") and isEnabled(), useUnmergedTree = true).performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Add", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("01-home")

        compose.onNodeWithText("Memory", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Search saved context").performTextInput("احمد")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("موعد أحمد بكرة — Please confirm meeting", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("02-memory")

        compose.onNodeWithText("موعد أحمد بكرة — Please confirm meeting", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Copy text", useUnmergedTree = true).assertExists()
        screenshot("03-evidence")

        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Discover", useUnmergedTree = true).performClick()
        screenshot("04-discover")
        compose.onNodeWithText("Activity", useUnmergedTree = true).performClick()
        screenshot("05-activity")
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        screenshot("06-settings")
    }
}
