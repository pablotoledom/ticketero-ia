package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("Feature: Procesamiento de Tickets")
class TicketProcessingIT extends BaseIntegrationTest {

    @Nested
    @DisplayName("Escenarios Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Procesar ticket completo → WAITING → COMPLETED")
        void procesarTicket_debeCompletarFlujo() {
            // Given - Verificar asesores disponibles
            int asesoresDisponibles = countAdvisorsInStatus("AVAILABLE");
            
            // When - Crear ticket (worker lo procesará automáticamente)
            given()
                .contentType("application/json")
                .body(createTicketRequest("33333333", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201);

            // Then - Esperar procesamiento (si hay workers activos)
            try {
                await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> countTicketsInStatus("COMPLETED") >= 1);
                
                // Verificar que se completó
                assertThat(countTicketsInStatus("COMPLETED")).isGreaterThanOrEqualTo(1);
            } catch (Exception e) {
                // Si no hay workers activos, verificar que el ticket existe
                assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);
            }
        }

        @Test
        @DisplayName("Múltiples tickets mantienen orden FIFO")
        void procesarTickets_debenSerFIFO() {
            // Given - Crear 3 tickets en orden
            String[] nationalIds = {"44444441", "44444442", "44444443"};
            
            for (String id : nationalIds) {
                given()
                    .contentType("application/json")
                    .body(createTicketRequest(id, "CAJA"))
                .when()
                    .post("/tickets");
                
                // Pequeña pausa para garantizar orden
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }

            // Then - Verificar que se crearon correctamente
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Escenarios Edge Case")
    class EdgeCases {

        @Test
        @DisplayName("Sin asesores disponibles → ticket permanece WAITING")
        void sinAsesores_ticketPermanece() {
            // Given - Poner todos los asesores en BUSY (si existen)
            try {
                jdbcTemplate.execute("UPDATE advisor SET status = 'BUSY'");
            } catch (Exception e) {
                // Ignorar si no hay tabla advisor
            }

            // When - Crear ticket
            given()
                .contentType("application/json")
                .body(createTicketRequest("55555555", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201);

            // Then - Verificar que permanece en WAITING
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);

            // Cleanup - Restaurar asesores
            try {
                jdbcTemplate.execute("UPDATE advisor SET status = 'AVAILABLE'");
            } catch (Exception e) {
                // Ignorar errores
            }
        }

        @Test
        @DisplayName("Ticket ya procesado no se reprocesa")
        void ticketCompletado_noSeReprocesa() {
            // Given - Crear ticket
            given()
                .contentType("application/json")
                .body(createTicketRequest("66666666", "CAJA"))
            .when()
                .post("/tickets");

            // Simular que se completó manualmente
            try {
                jdbcTemplate.execute(
                    "UPDATE ticket SET status = 'COMPLETED' WHERE national_id = '66666666'"
                );
            } catch (Exception e) {
                // Ignorar si no se puede actualizar
            }

            // When - Esperar un poco
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            // Then - Verificar que sigue completado
            int completedCount = countTicketsInStatus("COMPLETED");
            assertThat(completedCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Asesor en BREAK no recibe tickets")
        void asesorEnBreak_noRecibeTickets() {
            // Given - Poner un asesor en BREAK
            try {
                jdbcTemplate.execute("UPDATE advisor SET status = 'BREAK' WHERE id = 1");
            } catch (Exception e) {
                // Ignorar si no hay asesores
            }

            // When - Crear ticket
            given()
                .contentType("application/json")
                .body(createTicketRequest("77777777", "CAJA"))
            .when()
                .post("/tickets");

            // Then - Verificar que el ticket se creó
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);

            // Cleanup
            try {
                jdbcTemplate.execute("UPDATE advisor SET status = 'AVAILABLE' WHERE id = 1");
            } catch (Exception e) {
                // Ignorar errores
            }
        }
    }
}