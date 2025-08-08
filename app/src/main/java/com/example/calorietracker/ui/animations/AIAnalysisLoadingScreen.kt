package com.example.calorietracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Универсальный контейнер, который реализует эффект размытия фона в новом окне.
 */
@Composable
private fun FullscreenEffectContainer(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.35f))
        ) {

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                content()
            }
        }
    }
}

/**
 * Анимированное кольцо загрузки AI
 */
@Composable
private fun AILoadingRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        CircularProgressIndicator(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation),
            color = Color.Black,
            strokeWidth = 4.dp,
            trackColor = Color.Black.copy(alpha = 0.2f)
        )

        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.Black
        )
    }
}

/**
 * Компонент с анимированными забавными фразами
 *
 * @param inputMethod Метод ввода данных: "photo", "text", "chat", "analysis", "recipe"
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedPhrases(
    inputMethod: String? = null,
    modifier: Modifier = Modifier
) {
    // Базовые фразы для анализа еды
    val baseFoodPhrases = listOf(
        "Дайте подумать... 🤔",
        "Так, это похоже на еду... 🍽️",
        "Мне кажется это съедобно... 🧐",
        "Активирую нейросети... 🧠",
        "Анализирую молекулярный состав... 🔬",
        "Проверяю базу данных вкусняшек... 📚",
        "Хм, выглядит аппетитно... 😋",
        "Применяю магию подсчета КБЖУ... ✨",
        "Почти готово, еще чуть-чуть... ⏳",
        "AI в замешательстве... 🤖"
    )

    // Фразы для фото
    val photoPhrases = listOf(
        "Считаю калории по пикселям... 📸",
        "Сканирую изображение... 🖼️",
        "Рассматриваю под микроскопом... 🔍",
        "Это точно не торт? 🎂",
        "Определяю продукт по фото... 📷",
        "Анализирую цвета и текстуры... 🎨"
    )

    // Фразы для текста описания еды
    val textFoodPhrases = listOf(
        "Читаю ваше описание... 📖",
        "Разбираю текст по буквам... 📝",
        "Понимаю, о чем вы говорите... 💬",
        "Ищу в базе по описанию... 🔎",
        "Обрабатываю ваши слова... 💭",
        "Перевожу текст в калории... 📊"
    )

    // Фразы для поиска макронутриентов
    val macrosPhrases = listOf(
        "Сканирую на предмет белков... 🥩",
        "Ищу спрятанные углеводы... 🍞",
        "Жиры, покажитесь! 🧈",
        "Подсчитываю БЖУ... 🧮"
    )

    // Фразы для обычного чата
    val chatPhrases = listOf(
        "Размышляю над ответом... 💭",
        "Формулирую мысли... 🤔",
        "Подбираю нужные слова... 📝",
        "Обдумываю ваш вопрос... 🧠",
        "Готовлю ответ... ⏳",
        "Консультируюсь с базой знаний... 📚",
        "Анализирую контекст... 🔍",
        "Почти готов ответить... 🎯",
        "Обрабатываю информацию... 💡",
        "Секундочку, думаю... ⚡"
    )

    // Фразы для режима анализа дня
    val analysisPhrases = listOf(
        "Изучаю ваш рацион... 📊",
        "Анализирую статистику дня... 📈",
        "Считаю общее КБЖУ... 🧮",
        "Проверяю баланс нутриентов... ⚖️",
        "Оцениваю полезность питания... 🥗",
        "Сравниваю с вашими целями... 🎯",
        "Ищу паттерны в питании... 🔍",
        "Готовлю персональные советы... 💡",
        "Анализирую калорийность... 🔥",
        "Формирую рекомендации... 📋"
    )

    // Фразы для режима рецептов
    val recipePhrases = listOf(
        "Придумываю рецепт... 👨‍🍳",
        "Подбираю ингредиенты... 🥕",
        "Рассчитываю пропорции... ⚖️",
        "Вспоминаю кулинарные секреты... 🔐",
        "Колдую на кухне... ✨",
        "Составляю список продуктов... 📝",
        "Определяю время готовки... ⏲️",
        "Продумываю этапы приготовления... 📋",
        "Адаптирую под ваши предпочтения... 🎯",
        "Создаю кулинарный шедевр... 🍳"
    )

    // Выбираем фразы в зависимости от метода
    val phrases = remember(inputMethod) {
        val combinedPhrases = mutableListOf<String>()

        when (inputMethod) {
            "photo" -> {
                combinedPhrases.addAll(baseFoodPhrases)
                combinedPhrases.addAll(photoPhrases)
                combinedPhrases.addAll(macrosPhrases)
            }
            "text" -> {
                combinedPhrases.addAll(baseFoodPhrases)
                combinedPhrases.addAll(textFoodPhrases)
                combinedPhrases.addAll(macrosPhrases)
            }
            "chat" -> {
                combinedPhrases.addAll(chatPhrases)
            }
            "analysis" -> {
                combinedPhrases.addAll(analysisPhrases)
            }
            "recipe" -> {
                combinedPhrases.addAll(recipePhrases)
            }
            else -> {
                // По умолчанию используем фразы для чата
                combinedPhrases.addAll(chatPhrases)
            }
        }

        // Перемешиваем для рандомизации
        combinedPhrases.shuffled()
    }

    // Индекс текущей фразы и список показанных
    var currentPhraseIndex by remember { mutableStateOf(0) }
    var shownIndices by remember { mutableStateOf(setOf<Int>()) }

    // Получаем следующую случайную фразу
    fun getNextRandomIndex(): Int {
        // Если показали все фразы, сбрасываем
        if (shownIndices.size >= phrases.size) {
            shownIndices = setOf()
        }

        // Находим индекс, который еще не показывали
        var nextIndex: Int
        val availableIndices = phrases.indices.filter { !shownIndices.contains(it) }
        
        return if (availableIndices.isNotEmpty()) {
            availableIndices.random()
        } else {
            // Если все показаны (не должно происходить из-за проверки выше), берем случайный
            Random.nextInt(phrases.size)
        }
    }

    LaunchedEffect(Unit) {
        // Начинаем со случайной фразы
        currentPhraseIndex = getNextRandomIndex()
        shownIndices = shownIndices + currentPhraseIndex

        while (true) {
            delay(2500) // Показываем каждую фразу 2.5 секунды (стандарт)
            val nextIndex = getNextRandomIndex()
            shownIndices = shownIndices + nextIndex
            currentPhraseIndex = nextIndex
        }
    }

    // Анимация смены фраз с эффектом растворения
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = currentPhraseIndex,
            transitionSpec = {
                // Плавное растворение и появление
                (fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                )) with (fadeOut(
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    )
                ) + scaleOut(
                    targetScale = 1.08f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    )
                ))
            },
            label = "phrase_animation"
        ) { index ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phrases.getOrElse(index) { phrases.firstOrNull() ?: "" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp * 1.05f,
                        lineHeight = 24.sp * 1.1f,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Минималистичные точки для индикации загрузки
                AnimatedTypingDots(
                    dotSize = 5.dp,
                    dotSpacing = 2.dp,
                    primaryColor = Color.Black.copy(alpha = 0.5f),
                    secondaryColor = Color.Black.copy(alpha = 0.25f),
                    animationDuration = 600
                )
            }
        }
    }
}

@Composable
fun AIAnalysisLoadingScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    showDismissButton: Boolean = false,
    inputMethod: String? = null // Новый параметр
) {
    FullscreenEffectContainer(onDismiss = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, top = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            AILoadingRing()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedPhrases(inputMethod = inputMethod)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (showDismissButton) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Отменить",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}