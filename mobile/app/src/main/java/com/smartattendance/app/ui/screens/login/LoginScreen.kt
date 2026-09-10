package com.smartattendance.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartattendance.app.ServiceLocator
import com.smartattendance.app.ui.components.PrimaryButton
import com.smartattendance.app.ui.theme.Ink
import com.smartattendance.app.ui.theme.Primary
import com.smartattendance.app.ui.theme.SlateMuted

@Composable
fun LoginScreen(services: ServiceLocator, onLoggedIn: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory { initializer { LoginViewModel(services.authRepository) } }
    )
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    if (state.loggedIn) {
        onLoggedIn()
        return
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("BP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Text(
                "Blink Presence",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
            )
            Text(
                "Student sign-in",
                fontSize = 14.sp,
                color = SlateMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 28.dp),
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 20.dp))
            PrimaryButton(
                text = if (state.loading) "Signing in..." else "Sign in",
                onClick = viewModel::submit,
                loading = state.loading,
            )
        }
    }
}
