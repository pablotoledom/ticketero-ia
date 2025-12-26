package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Base Integration Test Setup")
class BaseIntegrationTestSetupIT extends BaseIntegrationTest {

    @Test
    @DisplayName("TestContainers should start correctly")
    void testContainers_shouldStart() {
        // Given - TestContainers are started
        assertThat(postgres.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
        
        // When - Check database connection
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        
        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Database should be clean between tests")
    void database_shouldBeClean() {
        // Given - Clean database
        int ticketCount = countTicketsInStatus("WAITING");
        int advisorCount = countAdvisorsInStatus("AVAILABLE");
        
        // Then
        assertThat(ticketCount).isZero();
        assertThat(advisorCount).isGreaterThan(0); // Should have advisors from migrations
    }
}