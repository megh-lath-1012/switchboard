package dev.meghlath.switchboard.shake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.meghlath.switchboard.compose.SwitchboardDebugScreen
import dev.meghlath.switchboard.compose.SwitchboardDebugStateHolder

/**
 * Dedicated internal host for the Switchboard debug dashboard.
 * Launched via shake detection or manual intent.
 */
internal class SwitchboardDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SwitchboardDebugScreen(
                        stateHolder = SwitchboardDebugStateHolder(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
