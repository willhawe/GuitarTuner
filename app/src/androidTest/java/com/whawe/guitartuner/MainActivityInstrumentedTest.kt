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
import org.junit.Assume
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// NOTE on test ordering:
// `pm revoke` for a dangerous permission kills all processes sharing the app UID on
// Android 10+, which includes the instrumentation process. A @Before revoke therefore
// crashes the test runner before the next test can start.
//
// Instead, tests are ordered so the dialog tests (a_ and b_) run first while the
// emulator is fresh and no permission exists, and the UI/navigation tests (c_ and d_)
// run after permission has been granted by b_. Each test that needs permission calls
// grantAudioPermission() directly so it is also safe on subsequent runs.
//
// Tests a_ and b_ use Assume.assumeTrue to skip (not fail) when the dialog cannot
// appear — i.e., permission is already granted, or permanently denied.
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MainActivityInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun a_denyingMicrophonePermission_keepsAppUsable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val denyButton = waitForPermissionButton(
                "permission_deny_button",
                "permission_deny_and_dont_ask_again_button"
            )
            // Skip rather than fail when the dialog does not appear (permission already
            // granted or permanently denied). On CI the emulator is always fresh.
            Assume.assumeTrue("Permission dialog did not appear; skipping", denyButton != null)
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
    fun b_allowingMicrophonePermissionStartsAutomaticTuning() {
        // Android 11 shows the dialog again after one denial, so this runs second.
        ActivityScenario.launch(MainActivity::class.java).use {
            val allowButton = waitForPermissionButton(
                "permission_allow_foreground_only_button",
                "permission_allow_button",
                "permission_allow_one_time_button"
            )
            Assume.assumeTrue("Permission dialog did not appear; skipping", allowButton != null)
            allowButton!!.click()
            device.waitForIdle()

            onView(withId(R.id.scalePracticeButton)).check(matches(isDisplayed()))
            onView(withId(R.id.statusTextView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun c_launch_rendersInitialState() {
        // Runs third. Permission is granted by b_ above; grantAudioPermission() makes
        // this test self-sufficient on runs where b_ was skipped.
        grantAudioPermission()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.noteTextView)).check(matches(withText(R.string.app_name)))
            onView(withId(R.id.frequencyTextView)).check(matches(withText(R.string.frequency_placeholder)))
            onView(withId(R.id.statusTextView)).check(matches(withText(R.string.status_listening)))
            onView(withId(R.id.scalePracticeButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun d_scalePracticeButton_opensScalePracticePage() {
        grantAudioPermission()

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.scalePracticeButton)).perform(click())
            onView(withId(R.id.scaleTitleTextView)).check(matches(withText(R.string.scale_practice_title)))
            onView(withId(R.id.scaleStaveView)).check(matches(isDisplayed()))
        }
    }

    // pm grant does NOT kill the process (unlike pm revoke on API 29+).
    private fun grantAudioPermission() {
        instrumentation.uiAutomation
            .executeShellCommand("pm grant ${targetContext.packageName} ${Manifest.permission.RECORD_AUDIO}")
            .close()
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
