package com.example.calorietracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.calorietracker.ui.utils.TransparentSystemBars
import com.example.calorietracker.ui.utils.systemBarsPadding

/**
 * Универсальный Scaffold с прозрачными системными барами
 * Используйте этот компонент вместо обычного Scaffold для всех экранов
 */
@Composable
fun TransparentScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    darkStatusBarIcons: Boolean = true,
    darkNavigationBarIcons: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    TransparentSystemBars(darkIcons = darkStatusBarIcons) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(containerColor)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = topBar,
                bottomBar = bottomBar,
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = floatingActionButtonPosition,
                containerColor = Color.Transparent,
                contentColor = contentColor,
                content = content
            )
        }
    }
}

/**
 * Простая обертка для экранов без Scaffold
 * Автоматически добавляет прозрачные системные бары и правильные отступы
 */
@Composable
fun TransparentSystemBarsScreen(
    modifier: Modifier = Modifier,
    darkStatusBarIcons: Boolean = true,
    darkNavigationBarIcons: Boolean = true,
    applyTopPadding: Boolean = true,
    applyBottomPadding: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    TransparentSystemBars(darkIcons = darkStatusBarIcons) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
                .systemBarsPadding(
                    top = applyTopPadding,
                    bottom = applyBottomPadding
                ),
            content = content
        )
    }
}

/**
 * TopAppBar с учетом прозрачного статус бара
 * Автоматически добавляет отступ сверху
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransparentTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    Surface(
        modifier = modifier,
        color = colors.containerColor
    ) {
        TopAppBar(
            title = title,
            modifier = Modifier.statusBarsPadding(),
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets(0),
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    }
}
