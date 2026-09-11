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
        FileInputStream(descriptor.fileDescriptor).use { stream -> while (stream.read() != -1) Unit }
        descriptor.close()
    }

    private fun add(text: String) {
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Save") and isEnabled(), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Save") and isEnabled(), useUnmergedTree = true).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Add", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun valueFirstOpenLoopsRecallSituationsAndHistory() {
        add("Ahmed — Please send the quotation today")
        add("CIB — payment of 5672 EGP is due today")
        add("CIB — please confirm your card payment today")

        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Needs you", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ahmed needs a reply", substring = true, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("CIB payment", substring = true, useUnmergedTree = true).assertExists()
        screenshot("01-home-value")

        compose.onNodeWithText("Memory", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("الحاجة اللي كان فيها 5672 جنيه")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("CIB — payment of 5672 EGP is due today", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("02-memory-vague-recall")

        compose.onNodeWithText("CIB — payment of 5672 EGP is due today", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Copy text", useUnmergedTree = true).assertExists()
        screenshot("03-evidence")
        compose.activityRule.scenario.recreate()

        compose.onNodeWithText("Situations", useUnmergedTree = true).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("CIB", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("04-situations")

        compose.onNodeWithText("Activity", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Outcome timeline", useUnmergedTree = true).assertExists()
        screenshot("05-activity")

        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Open loops", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("3.0.0 · 300", useUnmergedTree = true).assertExists()
        screenshot("06-settings")
    }
}
