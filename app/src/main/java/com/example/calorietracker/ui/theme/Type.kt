package com.example.calorietracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.calorietracker.R

// Определяем семейство Gilroy
val GilroyFontFamily = FontFamily(
    Font(R.font.gilroy_light, FontWeight.Light),
    Font(R.font.gilroy_regular, FontWeight.Normal),
    Font(R.font.gilroy_medium, FontWeight.Medium),
    Font(R.font.gilroy_semibold, FontWeight.SemiBold),
    Font(R.font.gilroy_bold, FontWeight.Bold),
    Font(R.font.gilroy_extrabold, FontWeight.ExtraBold),
    // Italic версии
    Font(R.font.gilroy_lightitalic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.gilroy_regularitalic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.gilroy_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.gilroy_bolditalic, FontWeight.Bold, FontStyle.Italic)
)

// Настраиваем Typography
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GilroyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GilroyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GilroyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GilroyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)