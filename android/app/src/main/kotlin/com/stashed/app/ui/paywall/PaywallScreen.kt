package com.stashed.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stashed.app.billing.BillingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    billingManager: BillingManager,
    onDismiss: () -> Unit,
) {
    val products by billingManager.products.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activity = LocalContext.current as? Activity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✨", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock Stashed Premium",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Everything stays on your phone. Always.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ComparisonTable()

            Spacer(modifier = Modifier.height(24.dp))

            // Pricing buttons
            products.forEach { product ->
                val label = when (product.productId) {
                    BillingManager.PRODUCT_MONTHLY -> "Monthly"
                    BillingManager.PRODUCT_ANNUAL -> "Annual (Best Value)"
                    BillingManager.PRODUCT_LIFETIME -> "Lifetime"
                    else -> product.name
                }

                val price = product.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    ?: product.oneTimePurchaseOfferDetails?.formattedPrice
                    ?: ""

                val isAnnual = product.productId == BillingManager.PRODUCT_ANNUAL

                if (isAnnual) {
                    Button(
                        onClick = { activity?.let { billingManager.launchPurchaseFlow(it, product) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            "$label \u2014 $price",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { activity?.let { billingManager.launchPurchaseFlow(it, product) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            "$label \u2014 $price",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (products.isEmpty()) {
                Text(
                    text = "Loading plans...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    "Not now",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ComparisonTable() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ComparisonRow("Feature", "Free", "Premium", isHeader = true)
            ComparisonRow("Saved memories", "50", "Unlimited")
            ComparisonRow("Semantic search", "\u2717", "\u2713")
            ComparisonRow("Voice input", "\u2717", "\u2713")
            ComparisonRow("Home widget", "\u2717", "\u2713")
            ComparisonRow("Location history", "\u2717", "\u2713")
            ComparisonRow("Backup & export", "\u2717", "\u2713")
        }
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    free: String,
    premium: String,
    isHeader: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1.4f),
            style = if (isHeader) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = free,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center,
            style = if (isHeader) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium,
            color = if (!isHeader && free == "\u2717") MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = premium,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center,
            style = if (isHeader) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium,
            color = if (!isHeader && premium == "\u2713") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}
