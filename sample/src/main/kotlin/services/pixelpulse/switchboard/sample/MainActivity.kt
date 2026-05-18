package services.pixelpulse.switchboard.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import services.pixelpulse.switchboard.sample.ui.screens.CartScreen
import services.pixelpulse.switchboard.sample.ui.screens.DoneScreen
import services.pixelpulse.switchboard.sample.ui.screens.HomeScreen
import services.pixelpulse.switchboard.sample.ui.screens.PaymentScreen
import services.pixelpulse.switchboard.sample.ui.screens.ShippingScreen
import services.pixelpulse.switchboard.sample.ui.theme.SwitchboardSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwitchboardSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onBuyNowClick = { navController.navigate("cart") }
                            )
                        }
                        composable("cart") {
                            CartScreen(
                                onContinue = { skipShipping ->
                                    if (skipShipping) {
                                        navController.navigate("payment")
                                    } else {
                                        navController.navigate("shipping")
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("shipping") {
                            ShippingScreen(
                                onContinue = { navController.navigate("payment") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("payment") {
                            PaymentScreen(
                                onPaymentSuccess = {
                                    navController.navigate("done") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("done") {
                            DoneScreen(
                                onBackToHome = {
                                    navController.navigate("home") {
                                        popUpTo(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
