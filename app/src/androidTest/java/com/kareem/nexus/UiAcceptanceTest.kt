package com.kareem.nexus

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import org.junit.Rule
import org.junit.Test

class UiAcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val command = "mkdir -p /sdcard/Download/NEXUS-test-screenshots && screencap -p /sdcard/Download/NEXUS-test-screenshots/$name.png"
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).use { stream ->
            while (stream.read() != -1) Unit
        }
        descriptor.close()
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
