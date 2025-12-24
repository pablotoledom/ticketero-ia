package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Configuración Base de Tests E2E")
class ConfigurationIT extends BaseIntegrationTest {

    @Test
    @DisplayName("TestContainers deben iniciar correctamente")
    void testContainers_shouldStart() {
        // Given - TestContainers iniciados automáticamente
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }

    @Test
    @DisplayName("API debe estar disponible")
    void api_shouldBeAvailable() {
        // When - Llamar al health endpoint
        given()
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("Base de datos debe estar limpia entre tests")
    void database_shouldBeClean() {
        // Given - Base de datos limpia
        int ticketCount = countTicketsInStatus("WAITING");
        
        // Then - No debe haber tickets
        assertThat(ticketCount).isZero();
    }
}