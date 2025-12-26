# ADR-005: Integración con Telegram Bot API

## Estado
**Aceptado** - 2024-11-25

## Contexto

El sistema requiere enviar notificaciones push a usuarios móviles:
- Notificaciones inmediatas y programadas
- Alcance masivo (potencialmente miles de usuarios)
- Costo-efectivo para MVP
- Fácil setup y testing durante desarrollo
- Experiencia de usuario familiar

## Decisión

**Integrar con Telegram Bot API** como canal principal de notificaciones para el sistema de ticketero.

### Arquitectura de Integración

```
[TicketService] → [MessageScheduler] → [TelegramService] → [Telegram Bot API]
                                              ↓
                                      [User Telegram App]
```

## Justificación Técnica

1. **Setup Inmediato**: Bot creado en minutos con @BotFather
2. **Costo Zero**: API gratuita sin límites para uso normal
3. **Confiabilidad**: Infraestructura robusta de Telegram
4. **Rich Messages**: Soporte para emojis, formatting, botones
5. **Testing**: Fácil testing durante desarrollo

## Implementación

### Configuración del Bot
```bash
# 1. Crear bot con @BotFather
/newbot
# 2. Obtener token: 123456789:ABCDEF...
# 3. Configurar webhook (opcional) o polling
```

### Integración Spring Boot
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {
    
    @Value("${telegram.bot-token}")
    private String botToken;
    
    @Value("${telegram.api-url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    
    public void sendMessage(String chatId, String text) {
        String url = apiUrl + botToken + "/sendMessage";
        
        Map<String, Object> request = Map.of(
            "chat_id", chatId,
            "text", text,
            "parse_mode", "HTML"
        );
        
        try {
            restTemplate.postForObject(url, request, Map.class);
            log.info("Message sent to chat: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send message to chat: {}", chatId, e);
            throw new NotificationException("Telegram API error", e);
        }
    }
}
```

### Templates de Mensajes
```java
public class MessageTemplates {
    
    public static String ticketCreated(String numero, int posicion, String tiempo) {
        return String.format(
            "🎫 <b>Tu ticket %s está listo</b>\n\n" +
            "📍 Posición en cola: %d\n" +
            "⏱️ Tiempo estimado: %s\n\n" +
            "Te notificaremos cuando sea tu turno.",
            numero, posicion, tiempo
        );
    }
    
    public static String ticketUpcoming(String numero, int remaining) {
        return String.format(
            "⏰ <b>¡Casi es tu turno!</b>\n\n" +
            "🎫 Ticket: %s\n" +
            "👥 Faltan %d turnos\n\n" +
            "Prepárate para dirigirte a ventanilla.",
            numero, remaining
        );
    }
    
    public static String ticketActive(String numero) {
        return String.format(
            "🔔 <b>¡ES TU TURNO!</b>\n\n" +
            "🎫 Ticket: %s\n" +
            "🏃‍♂️ Dirígete a ventanilla ahora\n\n" +
            "Gracias por usar nuestro sistema.",
            numero
        );
    }
}
```

## Consecuencias

### ✅ Positivas
- **Adopción**: Telegram ampliamente usado (500M+ usuarios)
- **Confiabilidad**: 99.9%+ uptime de Telegram
- **Features**: Rich messaging con HTML, emojis, botones
- **Costo**: Completamente gratuito
- **Development**: Testing inmediato durante desarrollo
- **Escalabilidad**: Maneja millones de mensajes/día

### ⚠️ Negativas
- **Dependencia Externa**: Dependemos de infraestructura de Telegram
- **Adopción Limitada**: No todos los usuarios tienen Telegram
- **Rate Limits**: 30 mensajes/segundo por bot
- **Vendor Lock-in**: Migrar a otro canal requiere reescribir

### 🔄 Mitigaciones
- **Circuit Breaker**: Para fallos de Telegram API
- **Retry Logic**: Reintentos con backoff exponencial
- **Fallback**: SMS como canal secundario (futuro)
- **Rate Limiting**: Respetar límites de API

## Alternativas Consideradas

### WhatsApp Business API
```
✅ Ventajas:
- Mayor adopción que Telegram
- Integración nativa en móviles
- Mejor UX para usuarios finales

❌ Desventajas:
- Setup complejo (verificación business)
- Costo por mensaje
- Rate limits más estrictos
- Requiere webhook HTTPS
```

### SMS (Twilio/AWS SNS)
```
✅ Ventajas:
- Cobertura universal (cualquier móvil)
- No requiere app adicional
- Confiabilidad alta

❌ Desventajas:
- Costo por mensaje (~$0.01-0.05)
- Limitado a texto plano
- Problemas de spam/delivery
```

### Push Notifications (FCM)
```
✅ Ventajas:
- Integración nativa con apps móviles
- Rich notifications
- Gratuito

❌ Desventajas:
- Requiere app móvil propia
- Complejidad de desarrollo
- Gestión de tokens
```

### Email
```
✅ Ventajas:
- Cobertura universal
- Costo muy bajo
- Rich content (HTML)

❌ Desventajas:
- No es tiempo real
- Problemas de spam
- Menor engagement
```

## Patrones Implementados

### 1. **Circuit Breaker**
```java
@Component
public class TelegramCircuitBreaker {
    
    private final CircuitBreaker circuitBreaker = CircuitBreaker
        .ofDefaults("telegram-api");
    
    public void sendMessage(String chatId, String text) {
        Supplier<Void> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> {
                telegramService.sendMessage(chatId, text);
                return null;
            });
        
        Try.ofSupplier(decoratedSupplier)
            .recover(throwable -> {
                log.error("Circuit breaker opened for Telegram API");
                // Fallback logic
                return null;
            });
    }
}
```

### 2. **Retry with Backoff**
```java
@Retryable(
    value = {RestClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendMessage(String chatId, String text) {
    // Telegram API call
}
```

### 3. **Rate Limiting**
```java
@Component
public class TelegramRateLimiter {
    
    private final RateLimiter rateLimiter = RateLimiter
        .create(25.0); // 25 requests/second (below 30 limit)
    
    public void sendMessage(String chatId, String text) {
        rateLimiter.acquire();
        telegramService.sendMessage(chatId, text);
    }
}
```

## Configuración

### Variables de Entorno
```bash
# Bot configuration
TELEGRAM_BOT_TOKEN=123456789:ABCDEF...
TELEGRAM_API_URL=https://api.telegram.org/bot

# Rate limiting
TELEGRAM_RATE_LIMIT=25  # requests/second
TELEGRAM_RETRY_ATTEMPTS=3
TELEGRAM_RETRY_DELAY=1000  # milliseconds
```

### Application Properties
```yaml
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: ${TELEGRAM_API_URL:https://api.telegram.org/bot}
  rate-limit: ${TELEGRAM_RATE_LIMIT:25}
  retry:
    attempts: ${TELEGRAM_RETRY_ATTEMPTS:3}
    delay: ${TELEGRAM_RETRY_DELAY:1000}
```

## Métricas de Éxito

- ✅ **Delivery Rate**: > 99% mensajes entregados
- ✅ **Latency**: < 5 segundos desde trigger hasta entrega
- ✅ **Error Rate**: < 1% fallos de API
- ✅ **User Engagement**: > 80% usuarios leen notificaciones

## Monitoreo

### Métricas Clave
```java
@Component
public class TelegramMetrics {
    
    private final Counter messagesTotal = Counter.builder("telegram_messages_total")
        .description("Total messages sent")
        .register(Metrics.globalRegistry);
    
    private final Counter errorsTotal = Counter.builder("telegram_errors_total")
        .description("Total API errors")
        .register(Metrics.globalRegistry);
    
    private final Timer responseTime = Timer.builder("telegram_response_time")
        .description("API response time")
        .register(Metrics.globalRegistry);
}
```

### Alertas
- API error rate > 5%
- Response time > 10 seconds
- Circuit breaker opened
- Rate limit exceeded

## Roadmap Futuro

### Fase 2: Multi-canal
- WhatsApp Business API como canal premium
- SMS como fallback universal
- Email para notificaciones no críticas

### Fase 3: Rich Interactions
- Inline keyboards para acciones
- Callback queries para confirmaciones
- Bot commands para consultas

## Referencias

- [Telegram Bot API](https://core.telegram.org/bots/api)
- [BotFather Documentation](https://core.telegram.org/bots#creating-a-new-bot)
- [Spring Boot RestTemplate](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.rest-client.resttemplate)

---

**Autor:** Equipo de Arquitectura  
**Revisado por:** Product Owner  
**Próxima revisión:** 2025-05-25