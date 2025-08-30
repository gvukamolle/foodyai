# Решение проблемы блокировки webhook-запросов

## Диагностика проблемы

Поскольку проблема НЕ связана со скоростью сети (с VPN работает медленнее, но работает), вероятные причины:

1. **DNS блокировка** - оператор блокирует резолвинг домена hook.us2.make.com
2. **DPI (Deep Packet Inspection)** - анализ и блокировка webhook трафика по содержимому
3. **SNI блокировка** - блокировка по имени сервера в TLS handshake
4. **Блокировка по сигнатурам** - определенные HTTP заголовки или паттерны запросов

## Внесенные изменения

### 1. DNS обход
- Добавлена проверка стандартного DNS
- Реализован DNS-over-HTTPS через Cloudflare
- Возможность использования IP адреса напрямую
- Хардкод известных IP адресов Make.com как fallback

### 2. Маскировка трафика
- Изменены HTTP заголовки чтобы выглядеть как обычный браузер
- Добавлены заголовки Accept-Language, Origin, Referer
- User-Agent имитирует Chrome на Android

### 3. Альтернативные методы отправки
- Multipart с измененными заголовками
- Base64 JSON fallback
- Компрессия изображений для уменьшения размера

### 4. Диагностический инструмент
Создан `NetworkDiagnosticHelper` который может определить тип блокировки.

## Как использовать диагностику

Добавьте временно в ваш ViewModel следующий код для диагностики:

```kotlin
// В CalorieTrackerViewModel добавьте:
@Inject lateinit var networkDiagnostic: NetworkDiagnosticHelper

// Создайте временную функцию для тестирования:
fun runNetworkDiagnostic() {
    viewModelScope.launch {
        val result = networkDiagnostic.runFullDiagnostic()
        println("=== NETWORK DIAGNOSTIC RESULTS ===")
        println("DNS работает: ${result.dnsResolvable}")
        println("DNS IP: ${result.dnsIP}")
        println("DNS-over-HTTPS работает: ${result.dnsOverHttpsWorks}")
        println("Прямой IP доступ: ${result.directIPAccessWorks}")
        println("HTTPS в целом: ${result.httpsConnectivityOk}")
        println("Make.com доступен: ${result.makeComReachable}")
        println("Подозреваемая проблема: ${result.suspectedIssue}")
        println("================================")
        
        // Тест минимального запроса
        val minimalWorks = networkDiagnostic.testMinimalRequest()
        println("Минимальный запрос работает: $minimalWorks")
    }
}
```

## Что делать дальше

### 1. Запустите диагностику
Вызовите `runNetworkDiagnostic()` без VPN и посмотрите результаты в логах.

### 2. В зависимости от результатов:

**Если "DNS блокировка оператором":**
- DNS-over-HTTPS уже реализован и должен работать автоматически
- Проверьте что Cloudflare DNS (1.1.1.1) не заблокирован

**Если "DPI блокировка webhook трафика":**
- Попробуйте изменить webhook URL на Make.com (создать новый)
- Используйте нестандартный порт если Make.com позволяет

**Если "SNI блокировка":**
- Требуется использование прокси или туннелирования
- Рассмотрите использование промежуточного сервера

### 3. Получение актуальных IP адресов Make.com

С включенным VPN выполните:
```bash
nslookup hook.us2.make.com
# или
dig hook.us2.make.com
```

Затем обновите список IP в коде:
```kotlin
val knownIPs = listOf("НОВЫЙ_IP_1", "НОВЫЙ_IP_2")
```

### 4. Альтернативное решение - промежуточный сервер

Если ничего не помогает, можно:
1. Развернуть простой прокси на вашем сервере (например, на Heroku/Vercel)
2. Отправлять запросы на ваш сервер
3. Сервер перенаправляет их на Make.com

## Тестирование с минимальными правами

Для быстрого теста создайте простую кнопку в UI:

```kotlin
Button(onClick = {
    viewModel.runNetworkDiagnostic()
}) {
    Text("Диагностика сети")
}
```

Это поможет точно определить тип блокировки и выбрать правильное решение.
