package services.pixelpulse.switchboard.sample.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import services.pixelpulse.switchboard.sample.AppFlags
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CartItem(val name: String, val priceCents: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(onContinue: (skipShipping: Boolean) -> Unit, onBack: () -> Unit) {
    val items = listOf(
        CartItem("Premium Headphones", 29900),
        CartItem("USB-C Cable", 1900),
        CartItem("Wireless Charger", 4500)
    )

    val discount = AppFlags.autoDiscountPercent
    val multiplier = AppFlags.currencyMultiplier
    val maxCartValue = AppFlags.maxCartValueCents

    val subtotal = items.sumOf { it.priceCents }
    // Ensure we don't exceed max cart value (just for demo purposes)
    val effectiveSubtotal = if (subtotal > maxCartValue) maxCartValue else subtotal
    
    val discountAmount = (effectiveSubtotal * discount).toLong()
    val totalCents = effectiveSubtotal - discountAmount
    
    val finalTotal = totalCents * multiplier

    val formatCurrency: (Double) -> String = { amount ->
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        format.currency = Currency.getInstance("USD")
        format.format(amount / 100.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    val itemPrice = item.priceCents * multiplier
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.name, style = MaterialTheme.typography.bodyLarge)
                        Text(formatCurrency(itemPrice), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodyLarge)
                Text(formatCurrency(effectiveSubtotal * multiplier))
            }

            if (discount > 0f) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Discount (${(discount * 100).toInt()}%)", color = MaterialTheme.colorScheme.primary)
                    Text("-${formatCurrency(discountAmount * multiplier)}", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(formatCurrency(finalTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onContinue(AppFlags.skipShippingStep) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue to Checkout", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
