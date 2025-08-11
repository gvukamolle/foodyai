package com.example.calorietracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.ChatMessage
import com.example.calorietracker.MessageType
import com.example.calorietracker.FoodItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    private var _messages by mutableStateOf<List<ChatMessage>>(emptyList())
    var messages: List<ChatMessage>
        get() = _messages
        private set(value) { _messages = value }
    fun setMessages(value: List<ChatMessage>) { _messages = value }

    var inputMessage by mutableStateOf("")
        private set
    fun setInputMessage(value: String) { inputMessage = value }

    private var loadingJob: Job? = null

    fun addMessage(message: ChatMessage) {
        messages = messages + message
    }

    fun markMessageAnimated(message: ChatMessage) {
        messages = messages.map {
            if (it.id == message.id) it.copy(animate = true) else it
        }
    }

    fun removeMessageWithAnimation(messageId: String) {
        messages = messages.map {
            if (it.id == messageId) it.copy(animate = false) else it
        }
        viewModelScope.launch {
            delay(300)
            messages = messages.filterNot { it.id == messageId }
        }
    }

    fun removeFoodMessages(food: FoodItem) {
        messages = messages.filterNot { it.foodItem == food }
    }

    private fun getLoadingPhrases(inputMethod: String?): List<String> {
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

        val photoPhrases = listOf(
            "Считаю калории по пикселям... 📸",
            "Сканирую изображение... 🖼️",
            "Рассматриваю под микроскопом... 🔍",
            "Это точно не торт? 🎂",
            "Определяю продукт по фото... 📷",
            "Анализирую цвета и текстуры... 🎨"
        )

        val textFoodPhrases = listOf(
            "Читаю ваше описание... 📖",
            "Разбираю текст по буквам... 📝",
            "Понимаю, о чем вы говорите... 💬",
            "Ищу в базе по описанию... 🔎",
            "Обрабатываю ваши слова... 💭",
            "Перевожу текст в калории... 📊"
        )

        val macrosPhrases = listOf(
            "Сканирую на предмет белков... 🥩",
            "Ищу спрятанные углеводы... 🍞",
            "Жиры, покажитесь! 🧈",
            "Подсчитываю БЖУ... 🧮"
        )

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

        val recipePhrases = listOf(
            "Придумываю рецепт... 👨‍🍳",
            "Подбираю ингредиенты... 🥕",
            "Рассчитываю пропорции... ⚖️",
            "Вспоминаю кулинарные секреты... 🔐",
            "Колдую на кухне... ✨",
            "Составляю список продуктов... 📝",
            "Определяю время готовки... ⏲️",
            "Продумываю этапы приготовления... 📋",
            "Адаптирую под ваи предпочтения... 🎯",
            "Создаю кулинарный шедевр... 🍳"
        )

        val phrases = mutableListOf<String>()
        when (inputMethod) {
            "photo" -> {
                phrases.addAll(baseFoodPhrases)
                phrases.addAll(photoPhrases)
                phrases.addAll(macrosPhrases)
            }
            "text" -> {
                phrases.addAll(baseFoodPhrases)
                phrases.addAll(textFoodPhrases)
                phrases.addAll(macrosPhrases)
            }
            "analysis" -> phrases.addAll(analysisPhrases)
            "recipe" -> phrases.addAll(recipePhrases)
            else -> phrases.addAll(chatPhrases)
        }
        return phrases
    }

    fun startLoadingPhrases(inputMethod: String?) {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            val phrases = getLoadingPhrases(inputMethod).shuffled()
            var index = 0
            var previousId: String? = null
            while (isActive) {
                val phrase = phrases[index % phrases.size]
                index++
                val msg = ChatMessage(
                    type = MessageType.AI,
                    content = phrase,
                    isProcessing = true,
                    inputMethod = inputMethod,
                    animate = true
                )
                addMessage(msg)
                previousId?.let { removeMessageWithAnimation(it) }
                previousId = msg.id

                var elapsed = 0
                while (elapsed < 3000 && isActive) {
                    delay(100)
                    elapsed += 100
                }
                if (!isActive) break
            }
        }
    }

    fun stopLoadingPhrases() {
        loadingJob?.cancel()
        loadingJob = null
        val ids = messages.filter { it.isProcessing }.map { it.id }
        ids.forEach { removeMessageWithAnimation(it) }
    }
}