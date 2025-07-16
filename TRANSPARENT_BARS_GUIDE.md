# Руководство по использованию прозрачных системных баров

## Основные принципы

1. **Все системные бары в приложении теперь полностью прозрачные**
2. **Контент должен правильно отступать от системных баров**
3. **Цвет иконок системных баров управляется из кода**

## Использование для разных типов экранов

### 1. Экраны с Scaffold (большинство экранов)

```kotlin
@Composable
fun YourScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0), // Важно!
        topBar = {
            // Ваш TopAppBar с отступом от статус бара
            YourTopBar(
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            // Ваш BottomBar с отступом от навигационного бара
            YourBottomBar(
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { paddingValues ->
        // Ваш контент
    }
}
```

### 2. Экраны без Scaffold

```kotlin
@Composable
fun SimpleScreen() {
    TransparentSystemBarsScreen(
        darkStatusBarIcons = true, // true для темных иконок на светлом фоне
        darkNavigationBarIcons = true,
        backgroundColor = Color.White
    ) {
        // Ваш контент автоматически получит правильные отступы
        Column {
            Text("Контент")
        }
    }
}
```

### 3. Экраны с кастомными отступами

```kotlin
@Composable
fun CustomScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(
                top = true,      // Отступ от статус бара
                bottom = true,   // Отступ от навигационного бара
                start = false,   // Боковые отступы (для landscape)
                end = false
            )
    ) {
        // Ваш контент
    }
}
```

### 4. Экраны с изображением на весь экран

```kotlin
@Composable
fun FullscreenImageScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Изображение на весь экран без отступов
        Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // UI элементы с отступами
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Только статус бар
        ) {
            // TopBar и другие элементы
        }
    }
}
```

## Миграция существующих экранов

### До:
```kotlin
LaunchedEffect(Unit) {
    systemUiController.setSystemBarsColor(
        color = Color.White,
        darkIcons = true
    )
}
```

### После:
```kotlin
// Удалите код управления цветом системных баров
// Добавьте правильные отступы к элементам UI
```

## Важные моменты

1. **НЕ используйте** `systemUiController.setSystemBarsColor()` - бары всегда прозрачные
2. **ВСЕГДА добавляйте** отступы для TopBar и BottomBar
3. **Используйте** `WindowInsets(0)` в Scaffold для отключения автоматических отступов
4. **Для модальных окон** системные бары остаются прозрачными автоматически

## Полезные функции

- `Modifier.statusBarsPadding()` - отступ от статус бара
- `Modifier.navigationBarsPadding()` - отступ от навигационного бара
- `Modifier.systemBarsPadding()` - настраиваемые отступы от всех системных баров
- `getStatusBarHeight()` - получить высоту статус бара
- `getNavigationBarHeight()` - получить высоту навигационного бара

## Проверка

После миграции экрана убедитесь что:
1. Контент не перекрывается системными барами
2. Системные бары остаются прозрачными при навигации
3. Цвет иконок корректный для фона экрана
