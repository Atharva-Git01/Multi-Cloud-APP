package com.bbg.cloudapp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bbg.cloudapp.core.model.CloudProvider
import com.bbg.cloudapp.core.model.FileCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingRulesScreen(
    navController: NavController,
    viewModel: RoutingRulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is RoutingRulesUiState.Saved) {
            snackbarHostState.showSnackbar("Routing rules saved")
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routing Rules", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is RoutingRulesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is RoutingRulesUiState.Loaded, is RoutingRulesUiState.Saved, is RoutingRulesUiState.Error -> {
                val rules = when (state) {
                    is RoutingRulesUiState.Loaded -> state.rules
                    is RoutingRulesUiState.Saved -> state.rules
                    else -> emptyMap()
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                ) {
                    Text(
                        text = "Configure which cloud provider receives each file type.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(FileCategory.values()) { category ->
                            RoutingRuleRow(
                                category = category,
                                selectedProvider = rules[category.name]?.let {
                                    runCatching { CloudProvider.valueOf(it) }.getOrNull()
                                } ?: category.let {
                                    CloudProvider.values().find { p -> p.defaultCategory == category }
                                },
                                onProviderSelected = { provider ->
                                    viewModel.updateRule(category, provider)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.saveRules() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Rules")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutingRuleRow(
    category: FileCategory,
    selectedProvider: CloudProvider?,
    onProviderSelected: (CloudProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Route to:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(
                        text = selectedProvider?.let { "${it.emoji} ${it.displayName}" } ?: "Auto",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CloudProvider.values().forEach { provider ->
                        DropdownMenuItem(
                            text = { Text("${provider.emoji} ${provider.displayName}") },
                            onClick = {
                                onProviderSelected(provider)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
