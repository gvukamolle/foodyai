package com.example.calorietracker.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Единый компонент для полей ввода в приложении
 * Использует стиль из FeedbackScreen
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            // Активное состояние
            focusedBorderColor = Color.Black,
            focusedLabelColor = Color.Black,
            focusedContainerColor = Color.White,
            
            // Неактивное состояние
            unfocusedBorderColor = Color(0xFFE0E0E0),
            unfocusedLabelColor = Color.Gray,
            unfocusedContainerColor = Color(0xFFF5F5F5),
            
            // Отключенное состояние
            disabledBorderColor = Color(0xFFE0E0E0),
            disabledLabelColor = Color.Gray,
            disabledContainerColor = Color(0xFFF5F5F5),
            disabledTextColor = Color.Black.copy(alpha = 0.6f),
            
            // Состояние ошибки
            errorBorderColor = Color.Red,
            errorLabelColor = Color.Red,
            errorContainerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Константы для единого стиля в приложении
 */
object AppTheme {
    val cornerRadius = 12.dp
    val buttonHeight = 56.dp
    val fieldSpacing = 16.dp
    val cardSpacing = 16.dp
    
    object Colors {
        val primaryBlack = Color.Black
        val backgroundGray = Color(0xFFF5F5F5)
        val borderGray = Color(0xFFE0E0E0)
        val textGray = Color.Gray
    }
}
