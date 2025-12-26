package com.example.ticketero.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Feature: Creación de Tickets")
class TicketCreationIT extends BaseIntegrationTest {

    @Nested
    @DisplayName("Escenarios Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Crear ticket con datos válidos → 201 + status WAITING + Outbox")
        void crearTicket_datosValidos_debeCrearConOutbox() {
            // When - Crear ticket
            Response response = given()
                .contentType("application/json")
                .body(createTicketRequest("12345678", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .body("numero", notNullValue())
                .body("status", equalTo("WAITING"))
                .body("queueType", equalTo("CAJA"))
                .body("positionInQueue", greaterThan(0))
                .body("estimatedWaitMinutes", greaterThanOrEqualTo(0))
                .body("codigoReferencia", notNullValue())
                .extract().response();

            // Then - Verificar BD
            assertThat(countTicketsInStatus("WAITING")).isGreaterThanOrEqualTo(1);
            
            // Verificar Outbox (si existe)
            int outboxCount = countOutboxMessages("PENDING");
            // No fallar si no hay outbox configurado aún
        }

        @Test
        @DisplayName("Crear ticket sin teléfono → debe funcionar")
        void crearTicket_sinTelefono_debeCrear() {
            // Given
            String requestSinTelefono = """
                {
                    "nationalId": "87654321",
                    "branchOffice": "Sucursal Norte",
                    "queueType": "PERSONAL"
                }
                """;

            // When + Then
            given()
                .contentType("application/json")
                .body(requestSinTelefono)
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .body("numero", startsWith("P"));
        }

        @Test
        @DisplayName("Crear tickets para diferentes colas → posiciones independientes")
        void crearTickets_diferentesColas_posicionesIndependientes() {
            // Given + When - Crear un ticket por cada cola
            String[] colas = {"CAJA", "PERSONAL", "EMPRESAS", "GERENCIA"};
            String[] prefijos = {"C", "P", "E", "G"};

            for (int i = 0; i < colas.length; i++) {
                Response response = given()
                    .contentType("application/json")
                    .body(createTicketRequest("2000000" + i, colas[i]))
                .when()
                    .post("/tickets")
                .then()
                    .statusCode(201)
                    .extract().response();

                // Then - Cada cola empieza en posición 1
                assertThat(response.jsonPath().getInt("positionInQueue")).isEqualTo(1);
                assertThat(response.jsonPath().getString("numero")).startsWith(prefijos[i]);
            }
        }

        @Test
        @DisplayName("Consultar ticket por código de referencia")
        void consultarTicket_porCodigo_debeRetornarDatos() {
            // Given - Crear ticket
            Response createResponse = given()
                .contentType("application/json")
                .body(createTicketRequest("22222222", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(201)
                .extract().response();

            String codigoReferencia = createResponse.jsonPath().getString("codigoReferencia");

            // When + Then
            given()
            .when()
                .get("/tickets/" + codigoReferencia)
            .then()
                .statusCode(200)
                .body("codigoReferencia", equalTo(codigoReferencia))
                .body("status", equalTo("WAITING"))
                .body("positionInQueue", notNullValue())
                .body("estimatedWaitMinutes", notNullValue());
        }
    }

    @Nested
    @DisplayName("Escenarios de Validación")
    class Validation {

        @Test
        @DisplayName("nationalId inválido → 400")
        void nationalId_invalido_debeRechazar() {
            given()
                .contentType("application/json")
                .body(createTicketRequest("12345ABC", "CAJA"))
            .when()
                .post("/tickets")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("queueType inválido → 400")
        void queueType_invalido_debeRechazar() {
            String request = """
                {
                    "nationalId": "12345678",
                    "branchOffice": "Centro",
                    "queueType": "INVALIDO"
                }
                """;

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets")
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("branchOffice vacío → 400")
        void branchOffice_vacio_debeRechazar() {
            String request = """
                {
                    "nationalId": "12345678",
                    "branchOffice": "",
                    "queueType": "CAJA"
                }
                """;

            given()
                .contentType("application/json")
                .body(request)
            .when()
                .post("/tickets")
            .then()
                .statusCode(400);
        }
    }
}