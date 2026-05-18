package services.pixelpulse.switchboard.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import services.pixelpulse.switchboard.core.FlagType

private val OverrideGreen = Color(0xFF4CAF50)
private val RemoteBlue = Color(0xFF2196F3)
private val DefaultYellow = Color(0xFFFFC107)

/**
 * Premium standalone debug interface rendering live feature flag states,
 * remote overrides aggregation, responsive text filtering, and direct local override controls.
 *
 * @param stateHolder Controller encapsulating immutable state streams and event mutators.
 * @param modifier Custom styling modifiers applied to the top-level host layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SwitchboardDebugScreen(
    stateHolder: SwitchboardDebugStateHolder,
    modifier: Modifier = Modifier
) {
    val uiState by stateHolder.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Switchboard Debug UI",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    if (!uiState.isEmptyState && uiState.allFlags.any { it.source == ValueSource.OVERRIDE }) {
                        Button(
                            onClick = { stateHolder.clearAllOverrides() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Clear Overrides")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isEmptyState -> {
                    EmptyStateView()
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search bar spanning both keys and descriptions
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { stateHolder.updateSearchQuery(it) },
                            placeholder = { Text("Search flags by key or description...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Scrollable horizontal category tabs
                        if (uiState.categories.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = uiState.selectedCategory == null,
                                        onClick = { stateHolder.selectCategory(null) },
                                        label = { Text("All") }
                                    )
                                }
                                items(uiState.categories) { category ->
                                    FilterChip(
                                        selected = uiState.selectedCategory == category,
                                        onClick = { stateHolder.selectCategory(category) },
                                        label = { Text(category) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filtered Interactive Flag Elements
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.filteredFlags, key = { it.key }) { flag ->
                                FlagItemCard(
                                    flag = flag,
                                    onOverrideChange = { value -> stateHolder.setOverride(flag.key, value) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Feature Flags Discovered",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Switchboard.init() was invoked without providing a populated feature flag registry (or KSP discovered zero annotations). Ensure you pass the KSP-generated implementation (e.g. SwitchboardRegistryImpl) to enable interactive runtime capabilities.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlagItemCard(
    flag: FlagUiModel,
    onOverrideChange: (String?) -> Unit
) {
    val indicatorColor = when (flag.source) {
        ValueSource.OVERRIDE -> OverrideGreen
        ValueSource.REMOTE -> RemoteBlue
        ValueSource.DEFAULT -> DefaultYellow
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left color-coded vertical indicator border
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(indicatorColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = flag.key,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    SourceBadge(source = flag.source, color = indicatorColor)
                }

                if (flag.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = flag.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Render specific input interfaces adhering to native type safety constraints
                when (flag.type) {
                    FlagType.BOOLEAN -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active State: ${flag.currentValue}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = flag.currentValue.toBooleanStrictOrNull() ?: false,
                                onCheckedChange = { isChecked -> onOverrideChange(isChecked.toString()) }
                            )
                        }
                    }
                    FlagType.INT, FlagType.LONG, FlagType.FLOAT, FlagType.DOUBLE -> {
                        NumericFlagInput(flag = flag, onOverrideChange = onOverrideChange)
                    }
                    FlagType.STRING -> {
                        StringFlagInput(flag = flag, onOverrideChange = onOverrideChange)
                    }
                    FlagType.ENUM -> {
                        EnumFlagInput(flag = flag, onOverrideChange = onOverrideChange)
                    }
                }

                // Dedicated inline control enabling fine-grained individual override clearing
                if (flag.source == ValueSource.OVERRIDE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOverrideChange(null) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset to ${if (flag.defaultValue != flag.currentValue) "Default/Remote" else "Fallback"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(source: ValueSource, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = source.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun NumericFlagInput(
    flag: FlagUiModel,
    onOverrideChange: (String?) -> Unit
) {
    var text by remember(flag.currentValue) { mutableStateOf(flag.currentValue) }
    var isError by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            if (newValue.isBlank()) {
                isError = false
            } else {
                val isValid = when (flag.type) {
                    FlagType.INT -> newValue.toIntOrNull() != null
                    FlagType.LONG -> newValue.toLongOrNull() != null
                    FlagType.FLOAT -> newValue.toFloatOrNull() != null
                    FlagType.DOUBLE -> newValue.toDoubleOrNull() != null
                    else -> true
                }
                isError = !isValid
                if (isValid) {
                    onOverrideChange(newValue)
                }
            }
        },
        label = { Text("Numeric Value (${flag.type.name.lowercase()})") },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StringFlagInput(
    flag: FlagUiModel,
    onOverrideChange: (String?) -> Unit
) {
    var text by remember(flag.currentValue) { mutableStateOf(flag.currentValue) }

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
            onOverrideChange(newValue)
        },
        label = { Text("String Value") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EnumFlagInput(
    flag: FlagUiModel,
    onOverrideChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Selected Variant",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = flag.currentValue, fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            flag.enumEntries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry, fontWeight = if (entry == flag.currentValue) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onOverrideChange(entry)
                        expanded = false
                    }
                )
            }
        }
    }
}
