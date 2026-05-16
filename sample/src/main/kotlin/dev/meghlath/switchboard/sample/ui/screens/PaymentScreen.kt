package dev.meghlath.switchboard.sample.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.meghlath.switchboard.okhttp.SwitchboardInterceptor
import dev.meghlath.switchboard.sample.AppFlags
import dev.meghlath.switchboard.sample.CheckoutVariant
import dev.meghlath.switchboard.sample.SampleApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onPaymentSuccess: () -> Unit, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val checkoutVariant = AppFlags.checkoutVariant

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .addInterceptor(SwitchboardInterceptor())
            // Give OkHttp a long timeout to allow the MockWebServer to simulate delays
            .readTimeout(10, TimeUnit.SECONDS) 
            .build()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    if (!isLoading) {
                        TextButton(onClick = onBack) {
                            Text("< Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            when (checkoutVariant) {
                CheckoutVariant.CONTROL -> {
                    // Standard form
                    Text("Enter Credit Card Details", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = "4111 1111 1111 1111",
                        onValueChange = {},
                        label = { Text("Card Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = "12/26",
                            onValueChange = {},
                            label = { Text("Expiry") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = "123",
                            onValueChange = {},
                            label = { Text("CVV") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                CheckoutVariant.VARIANT_A -> {
                    // Simplified form
                    Text("Express Checkout", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = "4111 1111 1111 1111, 12/26, 123",
                        onValueChange = {},
                        label = { Text("Card details (Number, Exp, CVV)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CheckoutVariant.VARIANT_B -> {
                    // Wallets
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Pay with Wallet", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Or enter card details", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Card Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    
                    coroutineScope.launch {
                        try {
                            val request = Request.Builder()
                                .url(SampleApp.mockApiUrl)
                                .build()
                                
                            val response = withContext(Dispatchers.IO) {
                                okHttpClient.newCall(request).execute()
                            }
                            
                            if (response.isSuccessful) {
                                onPaymentSuccess()
                            } else {
                                errorMessage = "Payment failed: ${response.code}"
                            }
                        } catch (e: IOException) {
                            errorMessage = "Network timeout or error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Submit Payment", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        LaunchedEffect(errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(it)
            }
        }
    }
}
