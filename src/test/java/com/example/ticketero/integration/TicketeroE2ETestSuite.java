package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Suite de Tests E2E para el Sistema Ticketero
 * 
 * Ejecuta todos los tests de integración en orden:
 * 1. Configuración base
 * 2. Creación de tickets
 * 3. Procesamiento de tickets
 * 4. Notificaciones Telegram
 * 5. Validaciones avanzadas
 * 6. Dashboard administrativo
 */
@Suite
@SelectClasses({
    ConfigurationIT.class,
    TicketCreationIT.class,
    TicketProcessingIT.class,
    NotificationIT.class,
    ValidationIT.class,
    AdminDashboardIT.class
})
@DisplayName("Suite E2E: Sistema Ticketero")
public class TicketeroE2ETestSuite {
    // Esta clase solo define la suite, no contiene tests
}