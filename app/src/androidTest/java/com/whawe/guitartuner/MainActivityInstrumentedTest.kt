package com.whawe.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.FixMethodOrder
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MainActivityInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun a_launch_rendersIdleState() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.noteTextView)).check(matches(withText(R.string.app_name)))
            onView(withId(R.id.frequencyTextView)).check(matches(withText(R.string.frequency_placeholder)))
            onView(withId(R.id.statusTextView)).check(matches(withText(R.string.status_idle)))
            onView(withId(R.id.scalePracticeButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun b_denyingMicrophonePermission_keepsAppUsable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val denyButton = waitForPermissionButton(
                "permission_deny_button",
                "permission_deny_and_dont_ask_again_button"
            )
            assertNotNull("Permission deny button not found", denyButton)
            denyButton!!.click()
            device.waitForIdle()

            assertEquals(
                PackageManager.PERMISSION_DENIED,
                ContextCompat.checkSelfPermission(targetContext, Manifest.permission.RECORD_AUDIO)
            )
            onView(withId(R.id.noteTextView)).check(matches(withText(R.string.app_name)))
            onView(withId(R.id.scalePracticeButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun c_allowingMicrophonePermissionStartsAutomaticTuning() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val allowButton = waitForPermissionButton(
                "permission_allow_foreground_only_button",
                "permission_allow_button",
                "permission_allow_one_time_button"
            )
            assertNotNull("Permission allow button not found", allowButton)
            allowButton!!.click()
            device.waitForIdle()

            onView(withId(R.id.scalePracticeButton)).check(matches(isDisplayed()))
            onView(withId(R.id.statusTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun d_scalePracticeButton_opensScalePracticePage() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.scalePracticeButton)).perform(click())
            onView(withId(R.id.scaleTitleTextView)).check(matches(withText(R.string.scale_practice_title)))
            onView(withId(R.id.scaleStaveView)).check(matches(isDisplayed()))
        }
    }

    private fun waitForPermissionButton(vararg buttonIds: String) =
        buttonIds.firstNotNullOfOrNull { buttonId ->
            permissionDialogPackages.firstNotNullOfOrNull { packageName ->
                device.wait(Until.findObject(By.res(packageName, buttonId)), PERMISSION_DIALOG_TIMEOUT_MS)
            }
        }

    private companion object {
        val permissionDialogPackages = listOf(
            "com.android.permissioncontroller",
            "com.android.packageinstaller"
        )
        const val PERMISSION_DIALOG_TIMEOUT_MS = 5_000L
    }
}
