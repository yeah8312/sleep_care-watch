package com.sleepcare.watch.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sleepcare.watch.ui.theme.SleepCareWatchTheme
import org.junit.Rule
import org.junit.Test

class WatchScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun connectionWaitingScreen_showsWaitingCopy() {
        composeRule.setContent {
            SleepCareWatchTheme {
                ConnectionWaitingScreen(
                    state = ConnectionWaitingUiState(),
                    onOpenOnPhone = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Waiting for phone").assertIsDisplayed()
        composeRule.onNodeWithText("Open on Phone").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsOpenOnPhoneAction() {
        composeRule.setContent {
            SleepCareWatchTheme {
                WatchSettingsScreen(
                    state = WatchSettingsUiState(),
                    onOpenOnPhone = {},
                    onRequestPermissions = {},
                    onRetrySync = {},
                )
            }
        }

        composeRule.onNodeWithText("Watch settings").assertIsDisplayed()
        composeRule.onNodeWithText("Open on Phone").assertIsDisplayed()
    }
}

