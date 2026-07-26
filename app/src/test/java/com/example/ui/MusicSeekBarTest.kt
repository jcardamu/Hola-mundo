package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MusicSeekBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFormatTime_zero() {
        assertEquals("00:00", formatTime(0L))
    }

    @Test
    fun testFormatTime_seconds() {
        assertEquals("00:45", formatTime(45000L))
    }

    @Test
    fun testFormatTime_minutes() {
        assertEquals("03:15", formatTime(195000L))
    }

    @Test
    fun testFormatTime_doubleDigitsMinutes() {
        assertEquals("12:05", formatTime(725000L))
    }

    @Test
    fun testSeekBarRendersCorrectTimeLabels() {
        composeTestRule.setContent {
            AnimatedMusicSeekBar(
                trackId = "test_track",
                position = 45000L, // 45 seconds
                duration = 180000L, // 3 minutes
                isPlaying = false,
                onSeek = {}
            )
        }

        // Verify that the start and end times are shown correctly in the text labels
        composeTestRule.onNodeWithText("00:45").assertExists()
        composeTestRule.onNodeWithText("03:00").assertExists()
    }
}
