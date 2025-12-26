package com.example.ticketero.service;

import com.example.ticketero.model.dto.TicketCreateRequest;
import com.example.ticketero.model.dto.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.OutboxMessageRepository;
import com.example.ticketero.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TicketService.
 * Uses mocks to isolate business logic from infrastructure.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private QueueManagementService queueManagementService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MetricsService metricsService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TicketService ticketService;

    private TicketCreateRequest request;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        request = new TicketCreateRequest(
                "12345678",
                "987654321",
                "Sucursal Centro",
                QueueType.CAJA
        );

        ticket = Ticket.builder()
                .id(1L)
                .codigoReferencia(UUID.randomUUID())
                .numero("C001")
                .nationalId("12345678")
                .telefono("987654321")
                .branchOffice("Sucursal Centro")
                .queueType(QueueType.CAJA)
                .status(TicketStatus.WAITING)
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();
    }

    @Test
    @DisplayName("crearTicket should calculate position and estimated time")
    void crearTicket_shouldCalculatePositionAndTime() {
        // Given
        when(queueManagementService.calcularPosicionEnCola(QueueType.CAJA)).thenReturn(1);
        when(queueManagementService.calcularTiempoEstimado(QueueType.CAJA, 1)).thenReturn(5);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenReturn(ticket);

        // When
        TicketResponse response = ticketService.crearTicket(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.numero()).isNotBlank();
        assertThat(response.positionInQueue()).isEqualTo(1);
        assertThat(response.estimatedWaitMinutes()).isEqualTo(5);
        assertThat(response.status()).isEqualTo(TicketStatus.WAITING);

        // Verify interactions
        verify(queueManagementService).calcularPosicionEnCola(QueueType.CAJA);
        verify(queueManagementService).calcularTiempoEstimado(QueueType.CAJA, 1);
        verify(ticketRepository).saveAndFlush(any(Ticket.class));
        verify(outboxMessageRepository).save(any());
        verify(notificationService).notificarTicketCreado(any(Ticket.class));
        verify(metricsService).incrementTicketsCreated(QueueType.CAJA);
    }

    @Test
    @DisplayName("crearTicket should save message to outbox for reliable messaging")
    void crearTicket_shouldSaveToOutbox() {
        // Given
        when(queueManagementService.calcularPosicionEnCola(QueueType.PERSONAL)).thenReturn(3);
        when(queueManagementService.calcularTiempoEstimado(QueueType.PERSONAL, 3)).thenReturn(15);

        Ticket personalTicket = Ticket.builder()
                .id(2L)
                .codigoReferencia(UUID.randomUUID())
                .numero("P002")
                .nationalId("87654321")
                .telefono("123456789")
                .branchOffice("Sucursal Norte")
                .queueType(QueueType.PERSONAL)
                .status(TicketStatus.WAITING)
                .positionInQueue(3)
                .estimatedWaitMinutes(15)
                .build();

        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenReturn(personalTicket);

        TicketCreateRequest personalRequest = new TicketCreateRequest(
                "87654321",
                "123456789",
                "Sucursal Norte",
                QueueType.PERSONAL
        );

        // When
        TicketResponse response = ticketService.crearTicket(personalRequest);

        // Then
        assertThat(response.queueType()).isEqualTo(QueueType.PERSONAL);
        assertThat(response.positionInQueue()).isEqualTo(3);

        // Verify outbox pattern: message saved to outbox table
        verify(outboxMessageRepository).save(any());
    }
}
