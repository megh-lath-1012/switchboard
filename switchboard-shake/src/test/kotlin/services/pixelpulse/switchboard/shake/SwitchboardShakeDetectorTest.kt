package services.pixelpulse.switchboard.shake

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
public class SwitchboardShakeDetectorTest {

    private val application = mockk<Application>(relaxed = true)
    private val sensorManager = mockk<SensorManager>(relaxed = true)
    private val activity = mockk<Activity>(relaxed = true)

    @Before
    public fun setUp() {
        clearAllMocks()
        every { activity.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        
        // Reset internal state of the singleton via reflection
        val countField = SwitchboardShakeDetector::class.java.getDeclaredField("shakeCount")
        countField.isAccessible = true
        countField.set(SwitchboardShakeDetector, 0)
        
        val timeField = SwitchboardShakeDetector::class.java.getDeclaredField("lastShakeTimestamp")
        timeField.isAccessible = true
        timeField.set(SwitchboardShakeDetector, 0L)
        
        val currentActField = SwitchboardShakeDetector::class.java.getDeclaredField("currentActivity")
        currentActField.isAccessible = true
        currentActField.set(SwitchboardShakeDetector, null)
    }

    @Test
    public fun `install registers lifecycle callbacks`() {
        SwitchboardShakeDetector.install(application)
        verify { application.registerActivityLifecycleCallbacks(any<Application.ActivityLifecycleCallbacks>()) }
    }

    @Test
    public fun `onActivityResumed registers sensor listener`() {
        val callbacksSlot = slot<Application.ActivityLifecycleCallbacks>()
        every { application.registerActivityLifecycleCallbacks(capture(callbacksSlot)) } returns Unit
        
        SwitchboardShakeDetector.install(application)
        callbacksSlot.captured.onActivityResumed(activity)
        
        verify { sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>()) }
    }

    @Test
    public fun `onActivityPaused unregisters sensor listener`() {
        val callbacksSlot = slot<Application.ActivityLifecycleCallbacks>()
        every { application.registerActivityLifecycleCallbacks(capture(callbacksSlot)) } returns Unit
        
        SwitchboardShakeDetector.install(application)
        callbacksSlot.captured.onActivityResumed(activity)
        callbacksSlot.captured.onActivityPaused(activity)
        
        verify { sensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    @Test
    public fun `activity with ignore annotation does not register listener`() {
        @SwitchboardIgnoreShake
        class IgnoredActivity : Activity()
        
        val ignoredActivity = mockk<IgnoredActivity>(relaxed = true)
        val callbacksSlot = slot<Application.ActivityLifecycleCallbacks>()
        every { application.registerActivityLifecycleCallbacks(capture(callbacksSlot)) } returns Unit
        
        SwitchboardShakeDetector.install(application)
        callbacksSlot.captured.onActivityResumed(ignoredActivity)
        
        verify(exactly = 0) { sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>()) }
    }

    @Test
    public fun `two shakes within window does NOT trigger activity launch`() {
        val callbacksSlot = slot<Application.ActivityLifecycleCallbacks>()
        every { application.registerActivityLifecycleCallbacks(capture(callbacksSlot)) } returns Unit
        SwitchboardShakeDetector.install(application, shakeCount = 3, windowMs = 1000L)
        callbacksSlot.captured.onActivityResumed(activity)

        // 1st shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))
        // 2nd shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))

        verify(exactly = 0) { activity.startActivity(any<Intent>()) }
    }

    @Test
    public fun `three shakes within window DOES trigger activity launch`() {
        val callbacksSlot = slot<Application.ActivityLifecycleCallbacks>()
        every { application.registerActivityLifecycleCallbacks(capture(callbacksSlot)) } returns Unit
        SwitchboardShakeDetector.install(application, shakeCount = 3, windowMs = 1000L)
        callbacksSlot.captured.onActivityResumed(activity)

        // 1st shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))
        // 2nd shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))
        // 3rd shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))

        verify(exactly = 1) { activity.startActivity(any<Intent>()) }
    }

    @Test
    public fun `three shakes spread over 2 seconds does NOT trigger with 1s window`() {
        val callbacksSlot = slot<Application.ActivityLifecycleCallbacks>()
        every { application.registerActivityLifecycleCallbacks(capture(callbacksSlot)) } returns Unit
        SwitchboardShakeDetector.install(application, shakeCount = 3, windowMs = 1000L)
        callbacksSlot.captured.onActivityResumed(activity)

        // 1st shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))
        
        // Wait 1.1s
        SystemClock.setCurrentTimeMillis(SystemClock.elapsedRealtime() + 1100)
        
        // 2nd shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))
        // 3rd shake
        SwitchboardShakeDetector.onSensorChanged(createSensorEvent(15f))

        verify(exactly = 0) { activity.startActivity(any<Intent>()) }
    }

    private fun createSensorEvent(magnitude: Float): SensorEvent {
        // Instantiate SensorEvent via reflection as it has no public constructor
        val constructor = SensorEvent::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val event = constructor.newInstance()
        
        // Set values field directly
        val valuesField = SensorEvent::class.java.getField("values")
        valuesField.set(event, floatArrayOf(magnitude, 0f, 0f))
        
        return event
    }
}
