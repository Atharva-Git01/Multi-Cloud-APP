package com.bbg.cloudapp.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbg.cloudapp.navigation.Routes
import com.bbg.cloudapp.ui.theme.StorageAmberColor
import com.bbg.cloudapp.ui.theme.StorageGreenColor
import com.bbg.cloudapp.ui.theme.StorageRedColor

private data class PasswordRule(val label: String, val check: (String) -> Boolean)

private val passwordRules = listOf(
    PasswordRule("8+ characters") { it.length >= 8 },
    PasswordRule("1 uppercase letter") { it.any { c -> c.isUpperCase() } },
    PasswordRule("1 number") { it.any { c -> c.isDigit() } },
    PasswordRule("1 special character") { it.any { c -> !c.isLetterOrDigit() } }
)

@Composable
fun CredentialSetupScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    val rulesPassed = passwordRules.map { rule -> rule.check(password) }
    val strengthScore = rulesPassed.count { it }
    val strengthColor = when (strengthScore) {
        0, 1 -> StorageRedColor
        2, 3 -> StorageAmberColor
        4 -> StorageGreenColor
        else -> StorageRedColor
    }
    val strengthLabel = when (strengthScore) {
        0, 1 -> "Weak"
        2, 3 -> "Fair"
        4 -> "Strong"
        else -> "Weak"
    }

    val isEmailValid = email.contains("@") && email.contains(".")
    val isPasswordValid = rulesPassed.all { it }
    val canProceed = isEmailValid && isPasswordValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "These credentials will be used for all your cloud provider accounts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text("Email address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = attemptedSubmit && !isEmailValid,
            supportingText = if (attemptedSubmit && !isEmailValid) {
                { Text("Please enter a valid email address") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            // Strength meter
            LinearProgressIndicator(
                progress = { strengthScore / 4f },
                modifier = Modifier.fillMaxWidth(),
                color = strengthColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Password strength: $strengthLabel",
                style = MaterialTheme.typography.labelMedium,
                color = strengthColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rules
            passwordRules.forEachIndexed { index, rule ->
                val passed = rulesPassed[index]
                Text(
                    text = "${if (passed) "✓" else "○"} ${rule.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (passed) StorageGreenColor
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                attemptedSubmit = true
                if (canProceed) {
                    viewModel.saveCredentials(email, password)
                    navController.navigate(Routes.PLATFORM_SELECTOR)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canProceed || !attemptedSubmit
        ) {
            Text("Continue")
        }
    }
}
