package dev.meghlath.switchboard.shake

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import java.lang.ref.WeakReference
import kotlin.math.sqrt

/**
 * Global shake gesture monitor that triggers the Switchboard debug interface.
 * 
 * Automatically manages sensor lifecycle by binding to activity resume/pause events,
 * ensuring zero battery impact when the application is backgrounded.
 */
public object SwitchboardShakeDetector : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var currentActivity: WeakReference<Activity>? = null
    
    private var shakeCount = 0
    private var lastShakeTimestamp = 0L
    
    private var targetShakeCount = 3
    private var targetWindowMs = 1000L
    private const val THRESHOLD = 12f

    /**
     * Initializes the detector and hooks into the application lifecycle.
     * 
     * @param application The hosting application instance.
     * @param shakeCount Minimum number of shakes required to trigger.
     * @param windowMs Time window in milliseconds to accumulate [shakeCount].
     */
    public fun install(
        application: Application,
        shakeCount: Int = 3,
        windowMs: Long = 1000L
    ) {
        this.targetShakeCount = shakeCount
        this.targetWindowMs = windowMs
        
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity.javaClass.isAnnotationPresent(SwitchboardIgnoreShake::class.java)) return
                
                currentActivity = WeakReference(activity)
                sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                sensorManager?.let { sm ->
                    sm.registerListener(
                        this@SwitchboardShakeDetector,
                        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
            }

            override fun onActivityPaused(activity: Activity) {
                sensorManager?.unregisterListener(this@SwitchboardShakeDetector)
                sensorManager = null
                currentActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        val magnitude = sqrt(x * x + y * y + z * z)
        
        if (magnitude > THRESHOLD) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeTimestamp > targetWindowMs) {
                shakeCount = 0
            }
            
            shakeCount++
            lastShakeTimestamp = now
            
            if (shakeCount >= targetShakeCount) {
                shakeCount = 0
                currentActivity?.get()?.let { activity ->
                    launchDebugActivity(activity)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    private fun launchDebugActivity(context: Context) {
        val intent = Intent(context, SwitchboardDebugActivity::class.java).apply {
            // No NEW_TASK flag if we have an activity context, unless we want to start a new task
            // Usually, we want it on top of the current stack.
        }
        context.startActivity(intent)
    }
}
