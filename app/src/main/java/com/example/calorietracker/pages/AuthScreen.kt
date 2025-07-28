package com.example.calorietracker.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.auth.AuthManager
import kotlinx.coroutines.launch
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.calorietracker.utils.capitalizeFirst


@Composable
fun AuthScreen(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit // Лямбда, которая будет вызвана при успешном входе/регистрации
) {
    var showLogin by remember { mutableStateOf(true) }
    val authState by authManager.authState.collectAsState()
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
    }
    LaunchedEffect(authState) {
        if (authState == AuthManager.AuthState.AUTHENTICATED) {
            onAuthSuccess()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showLogin) {
            LoginView(authManager = authManager, onAuthSuccess = onAuthSuccess) {
                showLogin = false // Переключиться на экран регистрации
            }
        } else {
            SignUpView(authManager = authManager, onAuthSuccess = onAuthSuccess) {
                showLogin = true // Переключиться на экран входа
            }
        }
    }
}

@Composable
fun LoginView(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Вход", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Button(
            onClick = {
                isLoading = true
                error = null
                scope.launch {
                    val result = authManager.signInWithEmail(email, password)
                    if (result.isSuccess) {
                        onAuthSuccess()
                    } else {
                        error = result.exceptionOrNull()?.message ?: "Ошибка входа"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("Войти")
        }
        Text("Нет аккаунта? Зарегистрироваться", modifier = Modifier.clickable(onClick = onNavigateToSignUp), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SignUpView(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Регистрация", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it.capitalizeFirst() },
            label = { Text("Ваше имя") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Button(
            onClick = {
                isLoading = true
                error = null
                scope.launch {
                    val result = authManager.signUpWithEmail(email, password, displayName)
                    if (result.isSuccess) {
                        onAuthSuccess()
                    } else {
                        error = result.exceptionOrNull()?.message ?: "Ошибка регистрации"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("Зарегистрироваться")
        }
        Text("Уже есть аккаунт? Войти", modifier = Modifier.clickable(onClick = onNavigateToLogin), color = MaterialTheme.colorScheme.primary)
    }
}