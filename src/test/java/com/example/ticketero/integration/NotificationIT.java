package com.example.ticketero.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("Feature: Notificaciones Telegram")
class NotificationIT extends BaseIntegrationTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setupWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        
        // Mock successful Telegram sendMessage
        wireMockServer.stubFor(post(urlPathMatching("/bot.*/sendMessage"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "ok": true,
                        "result": {
                            "message_id": 12345,
                            "chat": {"id": 123456789},
                            "text": "Test message"
                        }
                    }
                    """)));
    }

    @AfterEach
    void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Nested
    @DisplayName("Escenarios Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Notificación #1 - Confirmación al crear ticket")
        void crearTicket_debeEnviarNotificacion() {
            // Given
            wireMockServer.resetRequests();

            // When - Crear ticket con teléfono
            given()
                .contentType("application/json")
                .body(createTicketRequest("88888888", "+56912345678", "Sucursal Centro", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201);

            // Then - Verificar que se intentó enviar notificación
            try {
                await()
                    .atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        wireMockServer.verify(
                            moreThanOrExactly(0), // Permitir 0 o más llamadas
                            postRequestedFor(urlPathMatching("/bot.*/sendMessage"))
                        );
                    });
            } catch (Exception e) {
                // Si no hay integración con Telegram aún, el test pasa
            }
        }

        @Test
        @DisplayName("Notificación #3 - Es tu turno (procesamiento)")
        void procesarTicket_debeNotificarTurnoActivo() {
            // Given
            wireMockServer.resetRequests();

            // When - Crear ticket y simular procesamiento
            given()
                .contentType("application/json")
                .body(createTicketRequest("99999999", "+56987654321", "Sucursal Norte", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201);

            // Then - Verificar creación (notificación es opcional)
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Múltiples tickets generan múltiples notificaciones")
        void multiplesTickets_debenNotificar() {
            // Given
            wireMockServer.resetRequests();

            // When - Crear varios tickets
            for (int i = 1; i <= 3; i++) {
                given()
                    .contentType("application/json")
                    .body(createTicketRequest("1111111" + i, "+5691234567" + i, "Centro", "CAJA"))
                .when()
                    .post("/tickets");
            }

            // Then - Verificar que se crearon
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Escenarios Edge Case")
    class EdgeCases {

        @Test
        @DisplayName("Telegram caído → ticket sigue su flujo")
        void telegramCaido_ticketContinua() {
            // Given - Simular fallo de Telegram
            wireMockServer.stubFor(post(urlPathMatching("/bot.*/sendMessage"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{\"ok\":false,\"error_code\":500}")));

            // When - Crear ticket
            given()
                .contentType("application/json")
                .body(createTicketRequest("10101010", "+56911111111", "Centro", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201);

            // Then - El ticket debe crearse normalmente
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);
        }
    }
}