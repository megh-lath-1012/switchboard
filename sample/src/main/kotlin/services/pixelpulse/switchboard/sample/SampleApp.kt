package services.pixelpulse.switchboard.sample

import android.app.Application
import android.util.Log
import services.pixelpulse.switchboard.core.Switchboard
import services.pixelpulse.switchboard.shake.SwitchboardShakeDetector
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.concurrent.thread

import services.pixelpulse.switchboard.SwitchboardRegistryImpl
import services.pixelpulse.switchboard.android.init

class SampleApp : Application() {

    companion object {
        var mockApiUrl: String = ""
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Switchboard
        Switchboard.init(
            context = this,
            registry = SwitchboardRegistryImpl, // KSP-generated
            backend = LocalFakeFirebaseBackend(),
            debugEnabled = BuildConfig.DEBUG
        )

        // Install Shake Detector for debug builds
        if (BuildConfig.DEBUG) {
            SwitchboardShakeDetector.install(this)
        }

        // Start MockWebServer for local API simulation
        thread(start = true) {
            try {
                val server = MockWebServer()
                server.dispatcher = object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        // The delay is dynamic based on the current flag value
                        val delayMs = AppFlags.apiTimeoutMs.toLong()
                        Thread.sleep(delayMs)
                        return MockResponse()
                            .setResponseCode(200)
                            .setBody("""{"status":"success","message":"Order confirmed"}""")
                    }
                }
                server.start()
                mockApiUrl = server.url("/checkout").toString()
                Log.d("SampleApp", "MockWebServer started at $mockApiUrl")
            } catch (e: Exception) {
                Log.e("SampleApp", "Failed to start MockWebServer", e)
            }
        }
    }
}
