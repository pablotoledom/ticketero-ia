# ADR-002: Implementación de RabbitMQ para Notificaciones Asíncronas

## Estado
**Aceptado** - 2024-11-25

## Contexto

El sistema requiere enviar notificaciones Telegram de forma asíncrona:
- 3 tipos de mensajes programados (inmediato, 30s, 60s)
- Garantía de entrega de mensajes
- Tolerancia a fallos de Telegram API
- Desacoplamiento entre creación de ticket y envío de notificaciones

## Decisión

**Implementar RabbitMQ 3.13 como message broker** para el procesamiento asíncrono de notificaciones.

### Arquitectura Seleccionada

```
[TicketService] → [Database] → [Scheduler] → [RabbitMQ] → [TelegramService]
                     ↓
               [mensaje table]
```

## Justificación Técnica

1. **Garantía de Entrega**: Acknowledgments manuales previenen pérdida de mensajes
2. **Tolerancia a Fallos**: Dead letter queues para mensajes fallidos
3. **Escalabilidad**: Múltiples workers pueden procesar mensajes
4. **Observabilidad**: Management UI para monitoreo
5. **Ecosistema Spring**: Integración nativa con Spring AMQP

## Consecuencias

### ✅ Positivas
- **Desacoplamiento**: Creación de tickets independiente de notificaciones
- **Confiabilidad**: Mensajes persistentes sobreviven reinicio
- **Escalabilidad**: Workers horizontales para mayor throughput
- **Monitoreo**: Visibilidad completa de colas y mensajes
- **Retry Logic**: Reintentos automáticos en fallos temporales

### ⚠️ Negativas
- **Complejidad**: Infraestructura adicional a mantener
- **Latencia**: Overhead de serialización/deserialización
- **Dependencia**: Punto de fallo adicional en la arquitectura

### 🔄 Mitigaciones
- Health checks para RabbitMQ
- Clustering para alta disponibilidad (producción)
- Monitoring con Prometheus metrics

## Implementación

### Configuración Docker
```yaml
rabbitmq:
  image: rabbitmq:3.13-management-alpine
  environment:
    RABBITMQ_DEFAULT_USER: dev
    RABBITMQ_DEFAULT_PASS: dev123
  ports:
    - "5672:5672"    # AMQP
    - "15672:15672"  # Management UI
```

### Configuración Spring
```java
@RabbitListener(queues = "telegram-notifications")
public void processNotification(
    @Payload NotificationMessage message,
    Channel channel,
    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
) {
    try {
        telegramService.sendMessage(message);
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, true);
    }
}
```

## Alternativas Consideradas

### Apache Kafka
- ❌ **Rechazado**: Over-engineering para este volumen
- ❌ **Complejidad**: Setup y operación más compleja
- ✅ **Ventaja**: Mayor throughput y durabilidad

### Redis Pub/Sub
- ❌ **Rechazado**: Sin garantía de entrega
- ❌ **Persistencia**: Mensajes se pierden si Redis falla
- ✅ **Ventaja**: Menor latencia

### Amazon SQS
- ❌ **Rechazado**: Dependencia de AWS para desarrollo local
- ❌ **Costo**: Charges por mensaje
- ✅ **Ventaja**: Fully managed, alta disponibilidad

### Scheduler Simple (@Scheduled)
- ❌ **Rechazado**: No escalable horizontalmente
- ❌ **Confiabilidad**: Mensajes se pierden en reinicio
- ✅ **Ventaja**: Simplicidad máxima

## Patrones Implementados

### 1. **Message Scheduling**
```java
@Scheduled(fixedDelay = 5000)
public void processScheduledMessages() {
    List<Mensaje> pending = mensajeRepository
        .findByEstadoAndFechaProgramadaLessThanEqual(
            "PENDIENTE", LocalDateTime.now()
        );
    
    pending.forEach(this::sendToQueue);
}
```

### 2. **Dead Letter Queue**
```java
@Bean
public Queue telegramQueue() {
    return QueueBuilder.durable("telegram-notifications")
        .withArgument("x-dead-letter-exchange", "dlx")
        .withArgument("x-dead-letter-routing-key", "failed")
        .build();
}
```

## Métricas de Éxito

- ✅ **Throughput**: > 100 mensajes/segundo
- ✅ **Latencia**: < 5 segundos desde programación hasta envío
- ✅ **Confiabilidad**: 99.9% de mensajes entregados
- ✅ **Recovery**: < 30 segundos para procesar backlog

## Monitoreo

### Métricas Clave
- Queue depth (mensajes pendientes)
- Message processing rate
- Failed message count
- Consumer lag

### Alertas
- Queue depth > 1000 mensajes
- Failed messages > 5% del total
- No consumers activos > 1 minuto

## Referencias

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/docs/current/reference/html/)
- [ARCHITECTURE.md - Scheduler](../ARCHITECTURE.md#scheduler)

---

**Autor:** Equipo de Arquitectura  
**Revisado por:** Tech Lead  
**Próxima revisión:** 2025-05-25