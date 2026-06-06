package com.bbg.cloudapp.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bbg.cloudapp.core.model.AccountCreationStatus
import com.bbg.cloudapp.core.model.CreationState
import com.bbg.cloudapp.navigation.Routes
import com.bbg.cloudapp.ui.theme.PrimaryLight
import com.bbg.cloudapp.ui.theme.SecondaryLight
import com.bbg.cloudapp.ui.theme.StorageGreenColor
import com.bbg.cloudapp.ui.theme.StorageRedColor

@Composable
fun AccountCreationScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statuses by viewModel.creationStatuses.collectAsStateWithLifecycle()
    val selectedProviders by viewModel.selectedProviders.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState is OnboardingUiState.Idle) {
            viewModel.startAccountCreation()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is OnboardingUiState.Complete) {
            navController.navigate(Routes.ONBOARDING_COMPLETE) {
                popUpTo(Routes.ACCOUNT_CREATION) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryLight, Color(0xFF0D2438))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Setting Up Your Cloud Vault",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Creating accounts across your selected platforms...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            val displayStatuses = if (statuses.isNotEmpty()) statuses
            else selectedProviders.map { AccountCreationStatus(it, CreationState.PENDING) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayStatuses) { status ->
                    ProviderStatusRow(status = status)
                }
            }

            // Retry button for any failed
            val hasFailed = statuses.any { it.state == CreationState.FAILED }
            val allDone = statuses.isNotEmpty() &&
                    statuses.all { it.state == CreationState.DONE || it.state == CreationState.FAILED }

            if (hasFailed && allDone) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.retryFailedProviders() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Failed Providers")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProviderStatusRow(status: AccountCreationStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate_${status.provider.name}")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_${status.provider.name}"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = status.provider.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.provider.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = when (status.state) {
                        CreationState.PENDING -> "Waiting..."
                        CreationState.CREATING -> "Creating account..."
                        CreationState.DONE -> "Account ready"
                        CreationState.FAILED -> status.errorMessage ?: "Failed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (status.state) {
                            CreationState.DONE -> StorageGreenColor.copy(alpha = 0.2f)
                            CreationState.FAILED -> StorageRedColor.copy(alpha = 0.2f)
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (status.state) {
                    CreationState.CREATING -> CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation),
                        color = SecondaryLight,
                        strokeWidth = 2.dp
                    )
                    CreationState.DONE -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Done",
                        tint = StorageGreenColor,
                        modifier = Modifier.size(20.dp)
                    )
                    CreationState.FAILED -> Icon(
                        Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = StorageRedColor,
                        modifier = Modifier.size(20.dp)
                    )
                    CreationState.PENDING -> Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
